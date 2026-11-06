package server.dao;

import server.db.DBConnection;
import server.model.SharedFile;

import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TẦNG TRUY XUẤT DỮ LIỆU CHIA SẺ VÀ PHÂN QUYỀN (SHARE DATA ACCESS OBJECT)
 *
 * Quản lý vòng đời chia sẻ tệp tin:
 *   - Sinh mã Token mật mã ngẫu nhiên an toàn (Cryptographically Secure Random Token)
 *   - Kiểm tra tính hợp lệ và phân quyền truy cập tại Server (Access Control)
 *   - Thu hồi quyền chia sẻ (Revoke Share)
 *   - Liệt kê tệp được chia sẻ tới tài khoản (Shared with me) và đã chia sẻ (Shared by me)
 */
public class ShareDAO {

    private final DBConnection dbConnection;
    private final SecureRandom secureRandom;

    public ShareDAO() {
        this.dbConnection = DBConnection.getInstance();
        this.secureRandom = new SecureRandom();
    }

    /**
     * Sinh mã Token ngẫu nhiên 32 hex (128-bit) dùng cho liên kết chia sẻ
     */
    public String generateSecureToken() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Tạo một lượt chia sẻ tệp tin mới
     *
     * @param fileId            ID tệp tin cần chia sẻ
     * @param ownerId           ID chủ sở hữu tệp tin
     * @param sharedWithUserId  ID người nhận (null nếu là liên kết công khai)
     * @param permission        Quyền truy cập: READ_ONLY, DOWNLOAD, FULL
     * @param expiresAt         Thời hạn hết hiệu lực (null nếu vĩnh viễn)
     * @param maxDownloads      Số lượt tải tối đa (0 = không giới hạn)
     * @return chuỗi share_token nếu thành công, null nếu thất bại
     */
    public String createShare(int fileId, int ownerId, Integer sharedWithUserId,
                              String permission, Timestamp expiresAt, int maxDownloads) {
        if (fileId <= 0 || ownerId <= 0) return null;

        String token = generateSecureToken();
        String sql = 
            "INSERT INTO shared_files (file_id, owner_id, shared_with_user_id, share_token, " +
            "                         permission, expires_at, is_revoked, access_count, max_downloads) " +
            "VALUES (?, ?, ?, ?, ?, ?, FALSE, 0, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, fileId);
            stmt.setInt(2, ownerId);
            if (sharedWithUserId != null && sharedWithUserId > 0) {
                stmt.setInt(3, sharedWithUserId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, token);
            stmt.setString(5, (permission != null) ? permission : "DOWNLOAD");
            stmt.setTimestamp(6, expiresAt);
            stmt.setInt(7, Math.max(0, maxDownloads));

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                return token;
            }
        } catch (SQLException e) {
            System.err.println("[ShareDAO] Lỗi createShare: " + e.getMessage());
        }
        return null;
    }

    /**
     * Thu hồi quyền chia sẻ (Revoke Share)
     * Server-side Access Control: Chỉ chủ sở hữu tệp tin mới được quyền thu hồi.
     */
    public boolean revokeShare(int shareId, int requestingUserId) {
        if (shareId <= 0 || requestingUserId <= 0) return false;

        String sql = "UPDATE shared_files SET is_revoked = TRUE WHERE share_id = ? AND owner_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, shareId);
            stmt.setInt(2, requestingUserId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ShareDAO] Lỗi revokeShare: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xác thực Token và phân quyền truy cập (Access Control Verification)
     *
     * @param shareToken       Mã token chia sẻ
     * @param requestingUserId ID người dùng đang yêu cầu (có thể null nếu là khách dùng token)
     * @return SharedFile hợp lệ hoặc null nếu không đủ điều kiện
     */
    public SharedFile validateAndGetShare(String shareToken, Integer requestingUserId) {
        if (shareToken == null || shareToken.trim().isEmpty()) return null;

        String sql = 
            "SELECT s.*, f.filename, f.file_size, f.relative_path, f.is_deleted, " +
            "       u_owner.username AS owner_name, u_rec.username AS rec_name " +
            "FROM shared_files s " +
            "JOIN files f ON s.file_id = f.file_id " +
            "JOIN users u_owner ON s.owner_id = u_owner.user_id " +
            "LEFT JOIN users u_rec ON s.shared_with_user_id = u_rec.user_id " +
            "WHERE s.share_token = ? AND f.is_deleted = FALSE";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, shareToken.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    SharedFile sf = mapResultSetToSharedFile(rs);

                    // 1. Kiểm tra đã bị thu hồi chưa
                    if (sf.isRevoked()) return null;

                    // 2. Kiểm tra thời hạn hết hạn
                    if (sf.getExpiresAt() != null && sf.getExpiresAt().before(new Timestamp(System.currentTimeMillis()))) {
                        return null;
                    }

                    // 3. Kiểm tra số lượt tải tối đa
                    if (sf.getMaxDownloads() > 0 && sf.getAccessCount() >= sf.getMaxDownloads()) {
                        return null;
                    }

                    // 4. Nếu là chia sẻ đích danh tới 1 tài khoản cụ thể (Account-based sharing):
                    // Bắt buộc requestingUserId phải khớp với người nhận
                    if (sf.getSharedWithUserId() != null) {
                        if (requestingUserId == null || !requestingUserId.equals(sf.getSharedWithUserId())) {
                            return null; // Không được phép truy cập
                        }
                    }

                    return sf;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ShareDAO] Lỗi validateAndGetShare: " + e.getMessage());
        }
        return null;
    }

    /**
     * Tăng số lượt truy cập sau khi tải tệp thành công
     */
    public void incrementAccessCount(int shareId) {
        String sql = "UPDATE shared_files SET access_count = access_count + 1 WHERE share_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, shareId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ShareDAO] Lỗi incrementAccessCount: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách các tệp được chia sẻ tới người dùng hiện tại (Shared With Me)
     */
    public List<SharedFile> getSharedWithUser(int userId) {
        List<SharedFile> list = new ArrayList<>();
        if (userId <= 0) return list;

        String sql = 
            "SELECT s.*, f.filename, f.file_size, f.relative_path, " +
            "       u_owner.username AS owner_name, u_rec.username AS rec_name " +
            "FROM shared_files s " +
            "JOIN files f ON s.file_id = f.file_id " +
            "JOIN users u_owner ON s.owner_id = u_owner.user_id " +
            "LEFT JOIN users u_rec ON s.shared_with_user_id = u_rec.user_id " +
            "WHERE s.shared_with_user_id = ? AND s.is_revoked = FALSE " +
            "  AND (s.expires_at IS NULL OR s.expires_at > CURRENT_TIMESTAMP) " +
            "  AND f.is_deleted = FALSE " +
            "ORDER BY s.created_at DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSharedFile(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ShareDAO] Lỗi getSharedWithUser: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy danh sách các tệp do người dùng hiện tại đã chia sẻ (Shared By Me)
     */
    public List<SharedFile> getSharedByOwner(int ownerId) {
        List<SharedFile> list = new ArrayList<>();
        if (ownerId <= 0) return list;

        String sql = 
            "SELECT s.*, f.filename, f.file_size, f.relative_path, " +
            "       u_owner.username AS owner_name, u_rec.username AS rec_name " +
            "FROM shared_files s " +
            "JOIN files f ON s.file_id = f.file_id " +
            "JOIN users u_owner ON s.owner_id = u_owner.user_id " +
            "LEFT JOIN users u_rec ON s.shared_with_user_id = u_rec.user_id " +
            "WHERE s.owner_id = ? AND f.is_deleted = FALSE " +
            "ORDER BY s.created_at DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ownerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSharedFile(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ShareDAO] Lỗi getSharedByOwner: " + e.getMessage());
        }
        return list;
    }

    /**
     * Tìm lượt chia sẻ theo shareId
     */
    public SharedFile findById(int shareId) {
        String sql = 
            "SELECT s.*, f.filename, f.file_size, f.relative_path, " +
            "       u_owner.username AS owner_name, u_rec.username AS rec_name " +
            "FROM shared_files s " +
            "JOIN files f ON s.file_id = f.file_id " +
            "JOIN users u_owner ON s.owner_id = u_owner.user_id " +
            "LEFT JOIN users u_rec ON s.shared_with_user_id = u_rec.user_id " +
            "WHERE s.share_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, shareId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSharedFile(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ShareDAO] Lỗi findById: " + e.getMessage());
        }
        return null;
    }

    private SharedFile mapResultSetToSharedFile(ResultSet rs) throws SQLException {
        SharedFile sf = new SharedFile();
        sf.setShareId(rs.getInt("share_id"));
        sf.setFileId(rs.getInt("file_id"));
        sf.setOwnerId(rs.getInt("owner_id"));
        int recipient = rs.getInt("shared_with_user_id");
        sf.setSharedWithUserId(rs.wasNull() ? null : recipient);
        sf.setShareToken(rs.getString("share_token"));
        sf.setPermission(rs.getString("permission"));
        sf.setExpiresAt(rs.getTimestamp("expires_at"));
        sf.setRevoked(rs.getBoolean("is_revoked"));
        sf.setAccessCount(rs.getInt("access_count"));
        sf.setMaxDownloads(rs.getInt("max_downloads"));
        sf.setCreatedAt(rs.getTimestamp("created_at"));

        sf.setFilename(rs.getString("filename"));
        sf.setFileSize(rs.getLong("file_size"));
        sf.setRelativePath(rs.getString("relative_path"));
        sf.setOwnerUsername(rs.getString("owner_name"));
        sf.setRecipientUsername(rs.getString("rec_name"));
        return sf;
    }
}

