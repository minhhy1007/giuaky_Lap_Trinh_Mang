package server.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * CẤU HÌNH CƠ SỞ DỮ LIỆU MYSQL (DATABASE CONFIG)
 *
 * Đọc thông tin cấu hình từ file 'config/db.properties' ở ngoài mã nguồn.
 * Giúp bảo mật thông tin tài khoản và dễ dàng thay đổi giữa các môi trường
 * (Local, Trường học, Máy chủ thực hành) mà không cần biên dịch lại code.
 */
public class DatabaseConfig {

    private static final String CONFIG_FILE = "config/db.properties";

    private String host     = "localhost";
    private int    port     = 3306;
    private String dbName   = "custom_ftp_db";
    private String user     = "root";
    private String password = ""; // Mặc định thử mật khẩu trống hoặc từ file

    private boolean useSSL                = false;
    private boolean allowPublicKeyRetrieval = true;
    private String  serverTimezone        = "Asia/Ho_Chi_Minh";
    private String  characterEncoding     = "UTF-8";

    private static DatabaseConfig instance;

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
            instance.loadConfig();
        }
        return instance;
    }

    private DatabaseConfig() {}

    /**
     * Tải cấu hình từ config/db.properties nếu tồn tại.
     * Nếu không tìm thấy, sử dụng các giá trị mặc định đã khai báo.
     */
    public void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(in);

                this.host     = props.getProperty("db.host", this.host);
                this.port     = Integer.parseInt(props.getProperty("db.port", String.valueOf(this.port)));
                this.dbName   = props.getProperty("db.name", this.dbName);
                this.user     = props.getProperty("db.user", this.user);
                this.password = props.getProperty("db.password", this.password);

                this.useSSL                = Boolean.parseBoolean(props.getProperty("db.useSSL", "false"));
                this.allowPublicKeyRetrieval = Boolean.parseBoolean(props.getProperty("db.allowPublicKeyRetrieval", "true"));
                this.serverTimezone        = props.getProperty("db.serverTimezone", "Asia/Ho_Chi_Minh");
                this.characterEncoding     = props.getProperty("db.characterEncoding", "UTF-8");

                System.out.println("[DatabaseConfig] Đã nạp cấu hình từ: " + file.getAbsolutePath());
            } catch (IOException | NumberFormatException e) {
                System.err.println("[DatabaseConfig] Lỗi đọc " + CONFIG_FILE + ", sử dụng mặc định: " + e.getMessage());
            }
        } else {
            System.out.println("[DatabaseConfig] Không tìm thấy " + CONFIG_FILE + ", áp dụng cấu hình mặc định.");
        }
    }

    /**
     * Tạo chuỗi JDBC Connection URL hoàn chỉnh với đầy đủ tham số tối ưu.
     */
    public String getJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=%b&allowPublicKeyRetrieval=%b&serverTimezone=%s&characterEncoding=%s",
            host, port, dbName, useSSL, allowPublicKeyRetrieval, serverTimezone, characterEncoding
        );
    }

    // Getters
    public String getHost()     { return host; }
    public int    getPort()     { return port; }
    public String getDbName()   { return dbName; }
    public String getUser()     { return user; }
    public String getPassword() { return password; }

    // Setters phục vụ kiểm thử (unit testing)
    public void setHost(String host)         { this.host = host; }
    public void setPort(int port)             { this.port = port; }
    public void setDbName(String dbName)     { this.dbName = dbName; }
    public void setUser(String user)         { this.user = user; }
    public void setPassword(String password) { this.password = password; }
}

