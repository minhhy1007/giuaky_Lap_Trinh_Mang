package test;

import server.db.DBConnection;
import server.db.DatabaseConfig;
import server.db.DataReconciler;
import server.util.SecurityUtil;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * BỘ KIỂM THỬ TỰ ĐỘNG CHO PHASE 1: CƠ SỞ DỮ LIỆU & JDBC
 *
 * Chạy độc lập từ dòng lệnh, tự động đếm số ca PASS / FAIL.
 * Tuân thủ quy tắc: Tự dọn dẹp dữ liệu test (Clean-up), không làm bẩn CSDL.
 */
public class TestPhase1Database {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   BẮT ĐẦU CHẠY BỘ KIỂM THỬ TỰ ĐỘNG PHASE 1: CSDL & JDBC");
        System.out.println("==================================================================\n");

        test01_DriverLoaded();
        test02_ConnectionSuccess();
        test03_QuerySeedData();
        test04_UniqueConstraint();
        test05_ForeignKeyConstraint();
        test06_TransactionRollback();
        test07_WrongCredentialsHandling();
        test08_SecurityUtilHashing();
        test09_DataReconciliation();

        System.out.println("\n==================================================================");
        System.out.println(String.format("   KẾT QUẢ KIỂM THỬ: %d PASSED, %d FAILED", passedCount, failedCount));
        System.out.println("==================================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void pass(String testName) {
        passedCount++;
        System.out.println("[PASS] " + testName);
    }

    private static void fail(String testName, String reason) {
        failedCount++;
        System.err.println("[FAIL] " + testName + " -> LÝ DO: " + reason);
    }

    // Test 1: Kiểm tra nạp Driver
    private static void test01_DriverLoaded() {
        String name = "Test 1: Nạp MySQL Connector/J JDBC Driver";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            pass(name);
        } catch (ClassNotFoundException e) {
            fail(name, "Không tìm thấy com.mysql.cj.jdbc.Driver trong Classpath: " + e.getMessage());
        }
    }

    // Test 2: Kiểm tra kết nối tới MySQL
    private static void test02_ConnectionSuccess() {
        String name = "Test 2: Mở kết nối JDBC tới Database custom_ftp_db";
        try {
            DBConnection db = DBConnection.getInstance();
            Connection conn = db.getConnection();
            if (conn != null && !conn.isClosed() && conn.isValid(2)) {
                pass(name);
            } else {
                fail(name, "Connection null hoặc không hợp lệ.");
            }
        } catch (SQLException e) {
            fail(name, e.getMessage());
        }
    }

    // Test 3: Truy vấn dữ liệu mẫu (Roles & Users)
    private static void test03_QuerySeedData() {
        String name = "Test 3: Truy vấn dữ liệu khởi tạo (Seed Data)";
        String sqlRoles = "SELECT COUNT(*) FROM roles";
        String sqlUsers = "SELECT COUNT(*) FROM users WHERE username IN ('admin', 'alice', 'bob', 'guest')";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(sqlRoles);
             ResultSet rs1 = stmt1.executeQuery();
             PreparedStatement stmt2 = conn.prepareStatement(sqlUsers);
             ResultSet rs2 = stmt2.executeQuery()) {

            rs1.next();
            int roleCount = rs1.getInt(1);

            rs2.next();
            int userCount = rs2.getInt(1);

            if (roleCount >= 2 && userCount == 4) {
                pass(name + String.format(" (Roles: %d, Seed Users: %d)", roleCount, userCount));
            } else {
                fail(name, String.format("Dữ liệu không khớp: Roles=%d, Users=%d", roleCount, userCount));
            }
        } catch (SQLException e) {
            fail(name, e.getMessage());
        }
    }

    // Test 4: Ràng buộc Unique Constraint
    private static void test04_UniqueConstraint() {
        String name = "Test 4: Kiểm tra Ràng buộc Duy nhất (Unique Constraint)";
        String insertSql = "INSERT INTO users (username, password_hash, home_dir) VALUES ('admin', 'dummy_hash', 'admin_dir')";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.executeUpdate();
            fail(name, "Hệ thống cho phép chèn trùng username 'admin'!");
        } catch (SQLException e) {
            // Mong đợi lỗi duplicate entry (mã lỗi MySQL 1062)
            if (e.getErrorCode() == 1062 || e.getMessage().contains("Duplicate")) {
                pass(name + " (Bắt được lỗi Duplicate Entry chính xác)");
            } else {
                fail(name, "Ngoại lệ không đúng mã lỗi Duplicate: " + e.getMessage());
            }
        }
    }

    // Test 5: Ràng buộc Foreign Key
    private static void test05_ForeignKeyConstraint() {
        String name = "Test 5: Kiểm tra Ràng buộc Khóa ngoại (Foreign Key Constraint)";
        // Thử chèn file với user_id không tồn tại (999999)
        String sql = "INSERT INTO files (user_id, filename, relative_path, file_size) VALUES (999999, 'bad.txt', '/bad.txt', 100)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            fail(name, "Hệ thống cho phép chèn file với user_id không tồn tại!");
        } catch (SQLException e) {
            // Mã lỗi FK MySQL là 1452
            if (e.getErrorCode() == 1452 || e.getMessage().contains("foreign key")) {
                pass(name + " (Chặn thành công lỗi vi phạm Khóa ngoại FK)");
            } else {
                fail(name, "Lỗi không đúng kiểu Foreign Key: " + e.getMessage());
            }
        }
    }

    // Test 6: Kiểm tra Transaction Rollback
    private static void test06_TransactionRollback() {
        String name = "Test 6: Kiểm tra Transaction Atomic Rollback";
        DBConnection db = DBConnection.getInstance();

        try {
            Connection conn = db.getConnection();
            long initialQuota = 0;

            // Đọc quota hiện tại của user 'alice'
            try (PreparedStatement stmt = conn.prepareStatement("SELECT quota_bytes FROM users WHERE username = 'alice'")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) initialQuota = rs.getLong(1);
            }

            // Bắt đầu Transaction
            db.beginTransaction();

            // Cập nhật tăng quota
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET quota_bytes = quota_bytes + 5000 WHERE username = 'alice'")) {
                stmt.executeUpdate();
            }

            // Thực hiện Rollback (hủy bỏ)
            db.rollback();

            // Đọc lại quota sau khi Rollback
            long afterRollbackQuota = 0;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT quota_bytes FROM users WHERE username = 'alice'")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) afterRollbackQuota = rs.getLong(1);
            }

            if (initialQuota == afterRollbackQuota) {
                pass(name + " (Rollback thành công, dữ liệu trở về nguyên vẹn)");
            } else {
                fail(name, String.format("Quota bị thay đổi sau rollback: Trước=%d, Sau=%d", initialQuota, afterRollbackQuota));
            }

        } catch (SQLException e) {
            fail(name, e.getMessage());
        }
    }

    // Test 7: Xử lý sai thông tin kết nối
    private static void test07_WrongCredentialsHandling() {
        String name = "Test 7: Xử lý an toàn khi sai thông tin kết nối CSDL";
        DatabaseConfig config = DatabaseConfig.getInstance();
        String originalPassword = config.getPassword();

        try {
            // Cố tình đặt mật khẩu sai
            config.setPassword("sai_mat_khau_123456_random");
            String testUrl = config.getJdbcUrl();

            try (Connection badConn = java.sql.DriverManager.getConnection(testUrl, config.getUser(), config.getPassword())) {
                fail(name, "Kết nối thành công với mật khẩu sai!");
            } catch (SQLException expected) {
                pass(name + " (Bắt được lỗi xác thực mật khẩu an toàn: " + expected.getErrorCode() + ")");
            }
        } finally {
            // Khôi phục lại cấu hình chuẩn
            config.setPassword(originalPassword);
        }
    }

    // Test 8: Tiện ích bảo mật SecurityUtil
    private static void test08_SecurityUtilHashing() {
        String name = "Test 8: Kiểm tra Hashing & Password Verification (SecurityUtil)";
        String rawPass = "alice123";
        String salt = SecurityUtil.generateSalt();
        String hashWithSalt = SecurityUtil.hashPassword(rawPass, salt);

        boolean verified = SecurityUtil.verifyPassword(rawPass, salt, hashWithSalt);
        boolean wrongCheck = SecurityUtil.verifyPassword("sai_mat_khau", salt, hashWithSalt);

        if (verified && !wrongCheck && salt.length() == 32 && hashWithSalt.length() == 64) {
            pass(name + " (Băm SHA-256 + Salt 32 hex + đối soát mật khẩu chính xác)");
        } else {
            fail(name, "Xác thực mật khẩu băm không chính xác.");
        }
    }

    // Test 9: Đối soát dữ liệu DataReconciler
    private static void test09_DataReconciliation() {
        String name = "Test 9: Kiểm tra Đối soát Dữ liệu Filesystem & DB (DataReconciler)";
        try {
            DataReconciler reconciler = new DataReconciler(Paths.get("storage").toAbsolutePath().normalize());
            DataReconciler.ReconciliationReport report = reconciler.reconcile();
            if (report != null && report.quotaSyncedUsers >= 0) {
                pass(name + " (Chạy đối soát thành công. Báo cáo: " + report.quotaSyncedUsers + " users đã đồng bộ)");
            } else {
                fail(name, "Báo cáo đối soát rỗng hoặc bị lỗi.");
            }
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }
}

