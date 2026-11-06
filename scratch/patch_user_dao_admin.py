# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\dao\UserDAO.java', 'r', encoding='utf-8') as f:
    content = f.read()

new_methods = """
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
"""

last_brace = content.rfind("}")
if last_brace != -1:
    content = content[:last_brace] + new_methods + "\n}\n"

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\dao\UserDAO.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] UserDAO.java patched with Admin methods!")

