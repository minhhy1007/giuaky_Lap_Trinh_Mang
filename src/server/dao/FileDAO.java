package server.dao;

import server.db.DBConnection;
import server.model.FileMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * TẦNG TRUY XUẤT DỮ LIỆU METADATA TỆP TIN (FILE DATA ACCESS OBJECT)
 *
 * Quản lý các bản ghi tệp tin trong bảng 'files' của MySQL:
 *   - Thêm mới hoặc cập nhật metadata tệp tin sau khi upload
 *   - Đánh dấu xóa tệp tin và đối soát dung lượng
 *   - Cập nhật đổi tên tệp tin (RNFR/RNTO)
 *   - Tra cứu mã băm SHA-256 để kiểm tra tính toàn vẹn
 */
public class FileDAO {

    private final DBConnection dbConnection;

    public FileDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    /**
     * Thêm mới hoặc cập nhật tệp tin vào cơ sở dữ liệu.
     * Sử dụng ON DUPLICATE KEY UPDATE để xử lý ghi đè tệp tin cùng đường dẫn.
     *
     * @param userId       ID người dùng sở hữu
     * @param filename     Tên tệp tin
     * @param relativePath Đường dẫn tương đối trong Sandbox (ví dụ: "/docs/report.pdf")
     * @param fileSize     Kích thước tệp tin (bytes)
     * @param sha256Hash   Mã băm SHA-256 đối soát toàn vẹn
     * @return true nếu ghi nhận thành công
     */
    public boolean upsertFileRecord(int userId, String filename, String relativePath,
                                   long fileSize, String sha256Hash) {
        if (userId <= 0 || filename == null || relativePath == null) return false;

        String sql = 
            "INSERT INTO files (user_id, filename, relative_path, file_size, sha256_hash, is_deleted) " +
            "VALUES (?, ?, ?, ?, ?, FALSE) " +
            "ON DUPLICATE KEY UPDATE " +
            "  filename = VALUES(filename), " +
            "  file_size = VALUES(file_size), " +
            "  sha256_hash = VALUES(sha256_hash), " +
            "  is_deleted = FALSE, " +
            "  updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, filename);
            stmt.setString(3, relativePath);
            stmt.setLong(4, fileSize);
            stmt.setString(5, sha256Hash);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[FileDAO] Lỗi upsertFileRecord: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đánh dấu xóa tệp tin trong CSDL (Soft Delete)
     *
     * @return kích thước bytes của file đã xóa để hoàn trả quota (nếu file tồn tại), -1 nếu không tìm thấy
     */
    public long deleteFileRecord(int userId, String relativePath) {
        if (userId <= 0 || relativePath == null) return -1;

        String querySql = "SELECT file_size FROM files WHERE user_id = ? AND relative_path = ? AND is_deleted = FALSE";
        String deleteSql = "UPDATE files SET is_deleted = TRUE WHERE user_id = ? AND relative_path = ?";

        try (Connection conn = dbConnection.getConnection()) {
            long fileSize = -1;
            try (PreparedStatement stmt = conn.prepareStatement(querySql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, relativePath);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        fileSize = rs.getLong("file_size");
                    }
                }
            }

            if (fileSize >= 0) {
                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    stmt.setInt(1, userId);
                    stmt.setString(2, relativePath);
                    stmt.executeUpdate();
                }
            }
            return fileSize;

        } catch (SQLException e) {
            System.err.println("[FileDAO] Lỗi deleteFileRecord: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Cập nhật đổi tên tệp tin (Rename)
     */
    public boolean renameFileRecord(int userId, String oldRelPath, String newFilename, String newRelPath) {
        if (userId <= 0 || oldRelPath == null || newRelPath == null) return false;

        String sql = "UPDATE files SET filename = ?, relative_path = ? WHERE user_id = ? AND relative_path = ? AND is_deleted = FALSE";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newFilename);
            stmt.setString(2, newRelPath);
            stmt.setInt(3, userId);
            stmt.setString(4, oldRelPath);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[FileDAO] Lỗi renameFileRecord: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tra cứu metadata tệp tin theo đường dẫn
     */
    /**
     * Cập nhật đường dẫn tương đối cho toàn bộ các file con khi đổi tên thư mục cha
     */
    public boolean renameDirectoryRecords(int userId, String oldDirRelPath, String newDirRelPath) {
        if (userId <= 0 || oldDirRelPath == null || newDirRelPath == null) return false;
        String prefix = oldDirRelPath.endsWith("/") ? oldDirRelPath : oldDirRelPath + "/";
        String newPrefix = newDirRelPath.endsWith("/") ? newDirRelPath : newDirRelPath + "/";
        String sql = "UPDATE files SET relative_path = CONCAT(?, SUBSTRING(relative_path, ?)) " +
                     "WHERE user_id = ? AND relative_path LIKE ? AND is_deleted = FALSE";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPrefix);
            stmt.setInt(2, prefix.length() + 1);
            stmt.setInt(3, userId);
            stmt.setString(4, prefix + "%");
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[FileDAO] Lỗi renameDirectoryRecords: " + e.getMessage());
            return false;
        }
    }

    public FileMetadata findByPath(int userId, String relativePath) {
        if (userId <= 0 || relativePath == null) return null;

        String sql = "SELECT * FROM files WHERE user_id = ? AND relative_path = ? AND is_deleted = FALSE";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, relativePath);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFile(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[FileDAO] Lỗi findByPath: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả các tệp còn hiệu lực của người dùng
     */
    public List<FileMetadata> getFilesByUser(int userId) {
        List<FileMetadata> list = new ArrayList<>();
        String sql = "SELECT * FROM files WHERE user_id = ? AND is_deleted = FALSE ORDER BY uploaded_at DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFile(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[FileDAO] Lỗi getFilesByUser: " + e.getMessage());
        }
        return list;
    }

    /**
     * Tra cứu tệp theo Tên tệp và Thư mục hiện tại của người dùng
     */
    public FileMetadata findByFilename(int userId, String currentDir, String filename) {
        String relPath = "/" + (currentDir == null || currentDir.isEmpty() ? "" : currentDir + "/") + filename;
        return findByPath(userId, relPath);
    }

    /**
     * Tra cứu tệp theo fileId
     */
    public FileMetadata findById(int fileId) {
        if (fileId <= 0) return null;
        String sql = "SELECT * FROM files WHERE file_id = ? AND is_deleted = FALSE";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToFile(rs);
            }
        } catch (SQLException e) {
            System.err.println("[FileDAO] Lỗi findById: " + e.getMessage());
        }
        return null;
    }

    private FileMetadata mapResultSetToFile(ResultSet rs) throws SQLException {
        FileMetadata f = new FileMetadata();
        f.setFileId(rs.getInt("file_id"));
        f.setUserId(rs.getInt("user_id"));
        f.setFilename(rs.getString("filename"));
        f.setRelativePath(rs.getString("relative_path"));
        f.setFileSize(rs.getLong("file_size"));
        f.setSha256Hash(rs.getString("sha256_hash"));
        f.setDeleted(rs.getBoolean("is_deleted"));
        f.setUploadedAt(rs.getTimestamp("uploaded_at"));
        f.setUpdatedAt(rs.getTimestamp("updated_at"));
        return f;
    }
}

