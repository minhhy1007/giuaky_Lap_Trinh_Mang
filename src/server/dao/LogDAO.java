package server.dao;

import server.db.DBConnection;
import server.model.TransferLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * TẦNG TRUY XUẤT NHẬT KÝ KIỂM TOÁN (AUDIT LOG DATA ACCESS OBJECT)
 *
 * Ghi vết và truy vấn lịch sử hoạt động vào bảng 'transfer_logs' trong MySQL:
 *   - Các sự kiện: LOGIN, LOGOUT, UPLOAD, DOWNLOAD, DELETE, RENAME, MKDIR, RMDIR,
 *                  SHARE, REVOKE, ACCESS_DENIED, DOWNLOAD_SHARED
 *   - Phân quyền kiểm toán (RBAC Audit):
 *       + USER thường chỉ được xem nhật ký của chính mình.
 *       + ADMIN được phép xem và tra cứu toàn bộ lịch sử hệ thống.
 *   - Không bao giờ lưu token bí mật hay mật khẩu vào nhật ký.
 */
public class LogDAO {

    private final DBConnection dbConnection;

    public LogDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    /**
     * Ghi nhận một sự kiện vào bảng transfer_logs.
     * Tự động lọc sạch payload nhạy cảm.
     */
    public void log(Integer userId, String clientIp, String actionType,
                    String filename, long bytesTransferred, String status, String message) {
        String sql = 
            "INSERT INTO transfer_logs (user_id, client_ip, action_type, filename, bytes_transferred, status, message) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (userId != null && userId > 0) {
                stmt.setInt(1, userId);
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }

            stmt.setString(2, (clientIp != null) ? clientIp : "127.0.0.1");
            stmt.setString(3, (actionType != null) ? actionType : "UNKNOWN");
            stmt.setString(4, filename);
            stmt.setLong(5, Math.max(0, bytesTransferred));
            stmt.setString(6, (status != null) ? status : "SUCCESS");
            stmt.setString(7, sanitizeMessage(message));

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[LogDAO] Lỗi ghi nhật ký: " + e.getMessage());
        }
    }

    /**
     * Loại bỏ các token hoặc mật khẩu nhạy cảm khỏi thông điệp log
     */
    private String sanitizeMessage(String msg) {
        if (msg == null) return null;
        // Ẩn token 32-64 ký tự nếu có trong thông điệp
        return msg.replaceAll("(?i)(token[:=]\\s*)([a-f0-9]{32,64})", "$1***HIDDEN***");
    }

    /**
     * Lấy nhật ký của một người dùng cụ thể (phân trang và lọc theo hành động)
     */
    public List<TransferLog> getLogsByUser(int userId, int limit, int offset, String actionFilter) {
        List<TransferLog> list = new ArrayList<>();
        if (userId <= 0) return list;

        StringBuilder sb = new StringBuilder(
            "SELECT l.*, u.username " +
            "FROM transfer_logs l " +
            "LEFT JOIN users u ON l.user_id = u.user_id " +
            "WHERE l.user_id = ? "
        );

        if (actionFilter != null && !actionFilter.trim().isEmpty() && !actionFilter.equalsIgnoreCase("ALL")) {
            sb.append("AND l.action_type = ? ");
        }
        sb.append("ORDER BY l.created_at DESC LIMIT ? OFFSET ?");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sb.toString())) {

            int idx = 1;
            stmt.setInt(idx++, userId);
            if (actionFilter != null && !actionFilter.trim().isEmpty() && !actionFilter.equalsIgnoreCase("ALL")) {
                stmt.setString(idx++, actionFilter.trim().toUpperCase());
            }
            stmt.setInt(idx++, Math.max(1, limit));
            stmt.setInt(idx++, Math.max(0, offset));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[LogDAO] Lỗi getLogsByUser: " + e.getMessage());
        }
        return list;
    }

    /**
     * Đếm tổng số bản ghi log của người dùng phục vụ phân trang
     */
    public int countLogsByUser(int userId, String actionFilter) {
        if (userId <= 0) return 0;

        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM transfer_logs WHERE user_id = ? ");
        if (actionFilter != null && !actionFilter.trim().isEmpty() && !actionFilter.equalsIgnoreCase("ALL")) {
            sb.append("AND action_type = ? ");
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sb.toString())) {

            int idx = 1;
            stmt.setInt(idx++, userId);
            if (actionFilter != null && !actionFilter.trim().isEmpty() && !actionFilter.equalsIgnoreCase("ALL")) {
                stmt.setString(idx++, actionFilter.trim().toUpperCase());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[LogDAO] Lỗi countLogsByUser: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Lấy toàn bộ nhật ký hệ thống (Dành riêng cho ADMIN)
     */
    public List<TransferLog> getAllLogs(int limit, int offset, String actionFilter, String userFilter) {
        List<TransferLog> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT l.*, u.username " +
            "FROM transfer_logs l " +
            "LEFT JOIN users u ON l.user_id = u.user_id " +
            "WHERE 1=1 "
        );

        if (actionFilter != null && !actionFilter.trim().isEmpty() && !actionFilter.equalsIgnoreCase("ALL")) {
            sb.append("AND l.action_type = ? ");
        }
        if (userFilter != null && !userFilter.trim().isEmpty()) {
            sb.append("AND u.username LIKE ? ");
        }
        sb.append("ORDER BY l.created_at DESC LIMIT ? OFFSET ?");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sb.toString())) {

            int idx = 1;
            if (actionFilter != null && !actionFilter.trim().isEmpty() && !actionFilter.equalsIgnoreCase("ALL")) {
                stmt.setString(idx++, actionFilter.trim().toUpperCase());
            }
            if (userFilter != null && !userFilter.trim().isEmpty()) {
                stmt.setString(idx++, "%" + userFilter.trim() + "%");
            }
            stmt.setInt(idx++, Math.max(1, limit));
            stmt.setInt(idx++, Math.max(0, offset));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[LogDAO] Lỗi getAllLogs: " + e.getMessage());
        }
        return list;
    }

    private TransferLog mapResultSetToLog(ResultSet rs) throws SQLException {
        TransferLog log = new TransferLog();
        log.setLogId(rs.getInt("log_id"));
        int uid = rs.getInt("user_id");
        log.setUserId(rs.wasNull() ? null : uid);
        log.setUsername(rs.getString("username"));
        log.setClientIp(rs.getString("client_ip"));
        log.setActionType(rs.getString("action_type"));
        log.setFilename(rs.getString("filename"));
        log.setBytesTransferred(rs.getLong("bytes_transferred"));
        log.setStatus(rs.getString("status"));
        log.setMessage(rs.getString("message"));
        log.setCreatedAt(rs.getTimestamp("created_at"));
        return log;
    }
}
