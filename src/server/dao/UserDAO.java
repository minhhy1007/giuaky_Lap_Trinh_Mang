package server.dao;

import server.db.DBConnection;
import server.model.User;
import server.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * TẦNG TRUY XUẤT DỮ LIỆU TÀI KHOẢN (USER DATA ACCESS OBJECT)
 *
 * Quản lý toàn bộ thao tác SQL liên quan đến người dùng:
 *   - Tìm kiếm người dùng theo username
 *   - Xác thực tài khoản với mật khẩu băm SHA-256 + Salt
 *   - Đăng ký người dùng mới an toàn
 *   - Đổi mật khẩu
 *   - Liệt kê người dùng cho Quản trị viên
 */
public class UserDAO {

    private final DBConnection dbConnection;

    public UserDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    /**
     * Tìm kiếm người dùng theo username (kèm vai trò Role)
     */
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return null;

        String sql = 
            "SELECT u.user_id, u.username, u.password_hash, u.salt, u.email, " +
            "       u.role_id, r.role_name, u.quota_bytes, u.used_bytes, u.home_dir, " +
            "       u.is_active, u.created_at " +
            "FROM users u " +
            "JOIN roles r ON u.role_id = r.role_id " +
            "WHERE u.username = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi findByUsername ('" + username + "'): " + e.getMessage());
        }
        return null;
    }

    /**
     * Xác thực thông tin đăng nhập: Đối soát username và mật khẩu băm.
     *
     * @return User nếu thông tin chính xác và tài khoản đang hoạt động, null nếu sai
     */
    public User validateUser(String username, String rawPassword) {
        if (username == null || rawPassword == null) return null;

        User user = findByUsername(username);
        if (user == null || !user.isActive()) {
            return null; // Không tồn tại hoặc tài khoản bị khóa
        }

        // Đối chiếu mật khẩu an toàn
        boolean isValid = SecurityUtil.verifyPassword(rawPassword, user.getSalt(), user.getPasswordHash());
        if (isValid) {
            return user;
        }
        return null;
    }

    /**
     * Đăng ký tài khoản người dùng mới
     *
     * @param username    Tên đăng nhập (3 - 30 ký tự, không dấu cách)
     * @param rawPassword Mật khẩu gốc (tối thiểu 6 ký tự)
     * @param email       Email liên hệ (có thể null hoặc rỗng)
     * @return true nếu đăng ký thành công, false nếu username đã tồn tại hoặc lỗi
     */
    public boolean registerUser(String username, String rawPassword, String email) {
        if (username == null || rawPassword == null) return false;

        username = username.trim();
        rawPassword = rawPassword.trim();
        if (username.length() < 3 || username.length() > 30 || rawPassword.length() < 6) {
            return false;
        }

        // 1. Sinh Salt ngẫu nhiên và băm mật khẩu
        String salt = SecurityUtil.generateSalt();
        String passwordHash = SecurityUtil.hashPassword(rawPassword, salt);
        String homeDir = username; // Thư mục Sandbox trùng với username

        String sql = 
            "INSERT INTO users (username, password_hash, salt, email, role_id, quota_bytes, home_dir, is_active) " +
            "VALUES (?, ?, ?, ?, 2, 104857600, ?, TRUE)"; // Role mặc định 2: ROLE_USER, Quota 100MB

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, salt);
            stmt.setString(4, (email != null && !email.trim().isEmpty()) ? email.trim() : null);
            stmt.setString(5, homeDir);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || e.getMessage().contains("Duplicate")) {
                System.err.println("[UserDAO] Đăng ký thất bại: Trùng username hoặc email (" + username + ")");
            } else {
                System.err.println("[UserDAO] Lỗi đăng ký: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Đổi mật khẩu cho người dùng
     *
     * @param userId      ID người dùng cần đổi
     * @param oldPassword Mật khẩu hiện tại
     * @param newPassword Mật khẩu mới (tối thiểu 6 ký tự)
     * @return true nếu đổi thành công, false nếu mật khẩu cũ sai
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        if (oldPassword == null || newPassword == null || newPassword.trim().length() < 6) {
            return false;
        }

        String querySql = "SELECT password_hash, salt FROM users WHERE user_id = ?";
        String updateSql = "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?";

        try (Connection conn = dbConnection.getConnection()) {
            // 1. Kiểm tra mật khẩu hiện tại
            try (PreparedStatement stmt = conn.prepareStatement(querySql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) return false;
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");

                    if (!SecurityUtil.verifyPassword(oldPassword, salt, storedHash)) {
                        return false; // Mật khẩu cũ không chính xác
                    }
                }
            }

            // 2. Sinh Salt mới và cập nhật mật khẩu mới
            String newSalt = SecurityUtil.generateSalt();
            String newHash = SecurityUtil.hashPassword(newPassword.trim(), newSalt);

            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, newHash);
                stmt.setString(2, newSalt);
                stmt.setInt(3, userId);
                return stmt.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi changePassword (ID " + userId + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy toàn bộ danh sách người dùng cho Quản trị viên
     */
    /**
     * Cập nhật vai trò (Role) của người dùng
     */
    public boolean updateRole(int userId, int roleId) {
        String sql = "UPDATE users SET role_id = ? WHERE user_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roleId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi updateRole: " + e.getMessage());
            return false;
        }
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = 
            "SELECT u.user_id, u.username, u.password_hash, u.salt, u.email, " +
            "       u.role_id, r.role_name, u.quota_bytes, u.used_bytes, u.home_dir, " +
            "       u.is_active, u.created_at " +
            "FROM users u " +
            "JOIN roles r ON u.role_id = r.role_id " +
            "ORDER BY u.user_id ASC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi getAllUsers: " + e.getMessage());
        }
        return list;
    }

    // =========================================================================
    // QUẢN TRỊ DUNG LƯỢNG QUOTA & CONCURRENCY CONTROL (PHASE 3)
    // =========================================================================

    /**
     * Kiểm tra và đặt trước dung lượng Quota theo nguyên tử (Atomic Check-and-Reserve).
     * Ngăn chặn tuyệt đối tình trạng Race Condition khi nhiều client upload đồng thời.
     *
     * @param userId      ID người dùng cần cấp dung lượng
     * @param bytesToAdd  Số bytes của tệp tin chuẩn bị upload
     * @return true nếu còn đủ dung lượng và đã trừ thành công, false nếu vượt hạn mức
     */
    public boolean checkAndReserveQuota(int userId, long bytesToAdd) {
        if (userId <= 0 || bytesToAdd < 0) return true; // Người dùng không quản lý quota

        String sql = 
            "UPDATE users " +
            "SET used_bytes = used_bytes + ? " +
            "WHERE user_id = ? AND (used_bytes + ?) <= quota_bytes";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, bytesToAdd);
            stmt.setInt(2, userId);
            stmt.setLong(3, bytesToAdd);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi checkAndReserveQuota (User " + userId + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Hoàn trả dung lượng Quota khi upload bị hủy, gặp lỗi hoặc khi xóa tệp.
     *
     * @param userId          ID người dùng
     * @param bytesToRelease  Số bytes hoàn trả
     */
    public void releaseQuota(int userId, long bytesToRelease) {
        if (userId <= 0 || bytesToRelease <= 0) return;

        String sql = 
            "UPDATE users " +
            "SET used_bytes = GREATEST(0, used_bytes - ?) " +
            "WHERE user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, bytesToRelease);
            stmt.setInt(2, userId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi releaseQuota (User " + userId + "): " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin dung lượng hiện tại của người dùng: [used_bytes, quota_bytes]
     */
    public long[] getQuotaInfo(int userId) {
        String sql = "SELECT used_bytes, quota_bytes FROM users WHERE user_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new long[]{rs.getLong("used_bytes"), rs.getLong("quota_bytes")};
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi getQuotaInfo (User " + userId + "): " + e.getMessage());
        }
        return new long[]{0, 104857600}; // Mặc định 100MB
    }

    /**
     * Ánh xạ một dòng ResultSet sang đối tượng User
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setEmail(rs.getString("email"));
        u.setRoleId(rs.getInt("role_id"));
        u.setRoleName(rs.getString("role_name"));
        u.setQuotaBytes(rs.getLong("quota_bytes"));
        u.setUsedBytes(rs.getLong("used_bytes"));
        u.setHomeDir(rs.getString("home_dir"));
        u.setActive(rs.getBoolean("is_active"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }

    public boolean updateUserActive(int userId, boolean isActive) {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi updateUserActive: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUserQuota(int userId, long quotaBytes) {
        String sql = "UPDATE users SET quota_bytes = ? WHERE user_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, quotaBytes);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi updateUserQuota: " + e.getMessage());
            return false;
        }
    }

    public User findById(int userId) {
        String sql = 
            "SELECT u.user_id, u.username, u.password_hash, u.salt, u.email, " +
            "       u.role_id, r.role_name, u.quota_bytes, u.used_bytes, u.home_dir, " +
            "       u.is_active, u.created_at " +
            "FROM users u " +
            "JOIN roles r ON u.role_id = r.role_id " +
            "WHERE u.user_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi findById: " + e.getMessage());
        }
        return null;
    }

    public static class SystemStats {
        public int totalUsers;
        public int activeUsers;
        public int lockedUsers;
        public long totalQuotaBytes;
        public long totalUsedBytes;
        public int totalFiles;
        public int totalOrders;
        public double totalRevenue;
    }

    public SystemStats getSystemStats() {
        SystemStats stats = new SystemStats();
        try (Connection conn = dbConnection.getConnection()) {
            // Users stats
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) AS total, " +
                         "SUM(CASE WHEN is_active = TRUE THEN 1 ELSE 0 END) AS active_cnt, " +
                         "SUM(CASE WHEN is_active = FALSE THEN 1 ELSE 0 END) AS locked_cnt, " +
                         "SUM(quota_bytes) AS sum_quota, SUM(used_bytes) AS sum_used FROM users")) {
                if (rs.next()) {
                    stats.totalUsers = rs.getInt("total");
                    stats.activeUsers = rs.getInt("active_cnt");
                    stats.lockedUsers = rs.getInt("locked_cnt");
                    stats.totalQuotaBytes = rs.getLong("sum_quota");
                    stats.totalUsedBytes = rs.getLong("sum_used");
                }
            }

            // Files count
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM files WHERE is_deleted = FALSE")) {
                if (rs.next()) stats.totalFiles = rs.getInt(1);
            }

            // Orders and revenue
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) AS ord_cnt, " +
                         "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN amount ELSE 0 END), 0) AS rev FROM orders")) {
                if (rs.next()) {
                    stats.totalOrders = rs.getInt("ord_cnt");
                    stats.totalRevenue = rs.getDouble("rev");
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi getSystemStats: " + e.getMessage());
        }
        return stats;
    }

}
