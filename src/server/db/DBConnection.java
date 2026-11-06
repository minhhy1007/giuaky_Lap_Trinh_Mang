package server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * QUẢN TRỊ KẾT NỐI CƠ SỞ DỮ LIỆU MYSQL (DATABASE CONNECTION MANAGER)
 *
 * Mẫu thiết kế: Singleton Pattern kết hợp ThreadLocal Connection Isolation
 * Trách nhiệm:
 *   - Nạp Driver JDBC 'com.mysql.cj.jdbc.Driver'
 *   - Cung cấp kết nối độc lập, an toàn tuyệt đối cho từng luồng (Thread-Safe)
 *   - Tự động phát hiện đứt kết nối (connection timeout/closed) và tự kết nối lại
 *   - Cung cấp tiện ích quản trị Transaction (Commit / Rollback) theo từng luồng
 *   - Đóng tài nguyên an toàn (closeQuietly)
 */
public class DBConnection {

    private static DBConnection instance;
    private final DatabaseConfig config;
    private boolean driverLoaded = false;
    private final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

    private DBConnection() {
        this.config = DatabaseConfig.getInstance();
        loadDriver();
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Nạp MySQL Connector/J JDBC Driver vào JVM
     */
    private void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.driverLoaded = true;
            System.out.println("[DBConnection] Đã nạp thành công MySQL JDBC Driver.");
        } catch (ClassNotFoundException e) {
            this.driverLoaded = false;
            System.err.println("[DBConnection] LỖI: Không tìm thấy com.mysql.cj.jdbc.Driver trong Classpath!");
        }
    }

    /**
     * Lấy đối tượng Connection còn sống cho luồng hiện tại.
     * Tự động kết nối lại nếu kết nối của luồng trước đó bị đóng hoặc timeout.
     *
     * @return Connection hợp lệ
     * @throws SQLException nếu không thể kết nối tới MySQL
     */
    public Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            loadDriver();
            if (!driverLoaded) {
                throw new SQLException("MySQL JDBC Driver chưa được nạp.");
            }
        }

        Connection conn = threadConnection.get();
        if (conn == null || conn.isClosed() || !conn.isValid(2)) {
            String url = config.getJdbcUrl();
            conn = DriverManager.getConnection(url, config.getUser(), config.getPassword());
            threadConnection.set(conn);
        }

        return conn;
    }

    /**
     * Kiểm tra trạng thái khả dụng của Cơ sở dữ liệu (Health Check)
     * Không ném ngoại lệ ra ngoài.
     *
     * @return true nếu kết nối hoạt động tốt, false nếu mất kết nối
     */
    public boolean isAvailable() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Kiểm tra CSDL thất bại: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // QUẢN TRỊ GIAO DỊCH (TRANSACTION MANAGEMENT)
    // =========================================================================

    /**
     * Bắt đầu một Transaction mới: Tắt chế độ Auto-Commit trên luồng hiện tại.
     */
    public void beginTransaction() throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);
    }

    /**
     * Xác nhận và lưu vĩnh viễn các thay đổi trong Transaction (Commit).
     */
    public void commit() throws SQLException {
        Connection conn = threadConnection.get();
        if (conn != null && !conn.isClosed() && !conn.getAutoCommit()) {
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    /**
     * Hủy bỏ toàn bộ các thao tác dở dang trong Transaction khi gặp sự cố (Rollback).
     */
    public void rollback() {
        try {
            Connection conn = threadConnection.get();
            if (conn != null && !conn.isClosed() && !conn.getAutoCommit()) {
                conn.rollback();
                conn.setAutoCommit(true);
                System.out.println("[DBConnection] Đã Rollback giao dịch thành công.");
            }
        } catch (SQLException e) {
            System.err.println("[DBConnection] Lỗi khi Rollback giao dịch: " + e.getMessage());
        }
    }

    /**
     * Đóng an toàn các tài nguyên JDBC (Statement, ResultSet, Connection)
     */
    public static void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable res : resources) {
            if (res != null) {
                try {
                    res.close();
                } catch (Exception ignored) {}
            }
        }
    }
}
