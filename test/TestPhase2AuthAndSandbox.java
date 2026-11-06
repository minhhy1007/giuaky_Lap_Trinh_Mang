package test;

import server.CommandHandler;
import server.Session;
import server.StorageManager;
import server.UserDatabase;
import server.dao.UserDAO;
import server.db.DBConnection;
import server.model.User;
import server.util.SecurityUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * BỘ KIỂM THỬ TỰ ĐỘNG TOÀN DIỆN CHO PHASE 2:
 * XÁC THỰC, PHÂN QUYỀN (RBAC) & CÁCH LY THƯ MỤC SANDBOX
 */
public class TestPhase2AuthAndSandbox {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   BẮT ĐẦU CHẠY BỘ KIỂM THỬ TỰ ĐỘNG PHASE 2: AUTH & SANDBOX");
        System.out.println("==================================================================\n");

        test01_RegistrationAndValidation();
        test02_AuthenticationSuccessAndFailure();
        test03_PasswordChangeAndVerification();
        test04_NoPlaintextPasswordInDB();
        test05_RBACServerAuthorization();
        test06_SandboxDirectoryIsolation();
        test07_CrossAccountAccessPrevention();
        test08_UnauthenticatedCommandRejection();

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

    // Test 1: Đăng ký tài khoản và kiểm tra dữ liệu đầu vào
    private static void test01_RegistrationAndValidation() {
        String name = "Test 1: Đăng ký tài khoản mới & Kiểm tra ràng buộc hợp lệ";
        UserDAO dao = new UserDAO();
        String testUser = "test_user_p2";
        String testPass = "password123";

        // Dọn dẹp tài khoản test cũ nếu có
        cleanupUser(testUser);

        // 1. Thử đăng ký hợp lệ
        boolean ok1 = dao.registerUser(testUser, testPass, "test_p2@mail.com");
        // 2. Thử đăng ký trùng username -> phải trả về false
        boolean duplicateFail = !dao.registerUser(testUser, "anotherpass", "dup@mail.com");
        // 3. Thử mật khẩu quá ngắn (< 6 ký tự) -> phải trả về false
        boolean shortPassFail = !dao.registerUser("user_short", "123", "short@mail.com");

        if (ok1 && duplicateFail && shortPassFail) {
            pass(name + " (Tạo tài khoản thành công, chặn trùng lặp và mật khẩu ngắn)");
        } else {
            fail(name, String.format("Kết quả: ok=%b, dupFail=%b, shortPassFail=%b", ok1, duplicateFail, shortPassFail));
        }
    }

    // Test 2: Xác thực đăng nhập thành công và thất bại
    private static void test02_AuthenticationSuccessAndFailure() {
        String name = "Test 2: Xác thực đăng nhập (Mật khẩu đúng vs Mật khẩu sai)";
        UserDAO dao = new UserDAO();
        String testUser = "test_user_p2";

        User validUser = dao.validateUser(testUser, "password123");
        User invalidPass = dao.validateUser(testUser, "wrong_password");
        User nonExistent = dao.validateUser("user_khong_ton_tai_xyz", "any_password");

        if (validUser != null && invalidPass == null && nonExistent == null) {
            pass(name + String.format(" (Đăng nhập đúng trả về User ID=%d, sai trả về null)", validUser.getUserId()));
        } else {
            fail(name, "Xác thực không chính xác");
        }
    }

    // Test 3: Đổi mật khẩu và xác thực mật khẩu mới
    private static void test03_PasswordChangeAndVerification() {
        String name = "Test 3: Nghiệp vụ Đổi mật khẩu (PASSWD) & Hiệu lực tức thì";
        UserDAO dao = new UserDAO();
        String testUser = "test_user_p2";

        User user = dao.findByUsername(testUser);
        if (user == null) {
            fail(name, "Không tìm thấy user để đổi mật khẩu");
            return;
        }

        // Đổi mật khẩu: password123 -> new_secret_789
        boolean changeOk = dao.changePassword(user.getUserId(), "password123", "new_secret_789");
        // Mật khẩu cũ không còn đăng nhập được
        User oldCheck = dao.validateUser(testUser, "password123");
        // Mật khẩu mới đăng nhập thành công
        User newCheck = dao.validateUser(testUser, "new_secret_789");

        if (changeOk && oldCheck == null && newCheck != null) {
            pass(name + " (Đổi mật khẩu thành công, mật khẩu cũ hết hiệu lực ngay lập tức)");
        } else {
            fail(name, "Lỗi kiểm tra đổi mật khẩu");
        }
    }

    // Test 4: Mật khẩu trong DB tuyệt đối không phải Plaintext
    private static void test04_NoPlaintextPasswordInDB() {
        String name = "Test 4: Bảo mật CSDL - Mật khẩu được băm SHA-256 kèm Salt, không lưu Plaintext";
        String testUser = "test_user_p2";
        String sql = "SELECT password_hash, salt FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, testUser);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    String salt = rs.getString("salt");

                    boolean notPlaintext = !hash.equals("new_secret_789") && !hash.equals("password123");
                    boolean is64Hex = hash.length() == 64;
                    boolean hasSalt = salt != null && !salt.isEmpty();

                    if (notPlaintext && is64Hex && hasSalt) {
                        pass(name + String.format(" (Hash 64 hex: %s..., Salt: %s)", hash.substring(0, 16), salt));
                    } else {
                        fail(name, "Mật khẩu không thỏa tiêu chuẩn băm an toàn");
                    }
                } else {
                    fail(name, "Không tìm thấy user trong CSDL");
                }
            }
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 5: Phân quyền Server-side (RBAC: USER không thể dùng lệnh ADMIN)
    private static void test05_RBACServerAuthorization() {
        String name = "Test 5: Phân quyền RBAC tại Server (Chặn ROLE_USER, cấp phép ROLE_ADMIN)";
        try {
            StorageManager sm = new StorageManager();
            UserDatabase udb = new UserDatabase();

            // 1. Client USER (alice - ROLE_USER)
            Session userSession = new Session();
            CommandHandler userHandler = new CommandHandler(userSession, udb, sm);
            userHandler.handle("USER alice");
            userHandler.handle("PASS alice123");

            // Alice thử gửi lệnh USERLIST của ADMIN
            String userResponse = userHandler.handle("USERLIST");

            // 2. Client ADMIN (admin - ROLE_ADMIN)
            Session adminSession = new Session();
            CommandHandler adminHandler = new CommandHandler(adminSession, udb, sm);
            adminHandler.handle("USER admin");
            adminHandler.handle("PASS 123");

            // Admin gửi lệnh USERLIST
            String adminResponse = adminHandler.handle("USERLIST");

            boolean userBlocked = userResponse != null && userResponse.startsWith("550 Permission denied");
            boolean adminAllowed = adminResponse != null && adminResponse.startsWith("150 User list");

            if (userBlocked && adminAllowed) {
                pass(name + " (Chặn mã 550 với USER thường, phản hồi 150 với Quản trị viên)");
            } else {
                fail(name, String.format("UserResp='%s', AdminResp='%s'", userResponse, adminResponse));
            }

        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 6: Cách ly Thư mục Sandbox (User A tạo file, file nằm riêng trong Sandbox của A)
    private static void test06_SandboxDirectoryIsolation() {
        String name = "Test 6: Không gian Lưu trữ Cách ly Sandbox (storage/<username>/)";
        try {
            StorageManager sm = new StorageManager();

            // User Alice tạo 1 file trong sandbox của mình
            Path aliceFile = sm.resolveSafe("alice", "", "alice_secret.txt");
            Files.writeString(aliceFile, "Đây là tài liệu mật của Alice");

            // User Bob xem danh sách trong Sandbox của Bob
            String bobList = sm.listDirectory("bob", "");

            // File của Alice phải nằm trong storage/alice/ và Bob KHÔNG THỂ nhìn thấy trong list của Bob
            boolean physicalFileInAliceDir = aliceFile.toString().contains("alice") && Files.exists(aliceFile);
            boolean bobCannotSeeAliceFile = !bobList.contains("alice_secret.txt");

            if (physicalFileInAliceDir && bobCannotSeeAliceFile) {
                pass(name + " (File lưu độc lập trong storage/alice/, Bob không thấy file của Alice)");
            } else {
                fail(name, "Thư mục không được cách ly đúng chuẩn");
            }

        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 7: Chống Path Traversal và Chặn Truy cập Chéo Tài khoản
    private static void test07_CrossAccountAccessPrevention() {
        String name = "Test 7: Chống Tấn công Path Traversal & Truy cập chéo tài khoản (Cross-Account)";
        try {
            StorageManager sm = new StorageManager();

            // Bob cố tình gửi đường dẫn leo thang để đọc file của Alice: "../../alice/alice_secret.txt"
            boolean blocked1 = false;
            try {
                sm.resolveSafe("bob", "", "../../alice/alice_secret.txt");
            } catch (SecurityException e) {
                blocked1 = true;
            }

            // Bob cố tình gửi đường dẫn leo thang ra hệ điều hành: "../../../Windows/win.ini"
            boolean blocked2 = false;
            try {
                sm.resolveSafe("bob", "", "../../../Windows/win.ini");
            } catch (SecurityException e) {
                blocked2 = true;
            }

            if (blocked1 && blocked2) {
                pass(name + " (Chặn đứng 100% mọi âm mưu truy cập chéo tài khoản hoặc thoát Sandbox)");
            } else {
                fail(name, String.format("Blocked1=%b, Blocked2=%b", blocked1, blocked2));
            }

        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 8: Chặn lệnh được bảo vệ khi chưa đăng nhập
    private static void test08_UnauthenticatedCommandRejection() {
        String name = "Test 8: Chặn mọi thao tác dữ liệu khi chưa xác thực (Unauthenticated Gate)";
        try {
            Session session = new Session();
            CommandHandler handler = new CommandHandler(session, new UserDatabase(), new StorageManager());

            String rList = handler.handle("LIST");
            String rDele = handler.handle("DELE test.txt");
            String rPwd  = handler.handle("PWD");
            String rPass = handler.handle("PASSWD old new");

            boolean allBlocked = "530 Not logged in".equals(rList) &&
                                 "530 Not logged in".equals(rDele) &&
                                 "530 Not logged in".equals(rPwd)  &&
                                 "530 Not logged in".equals(rPass);

            if (allBlocked) {
                pass(name + " (Tất cả lệnh LIST, DELE, PWD, PASSWD đều bị chặn mã 530 Not logged in)");
            } else {
                fail(name, "Server cho phép thực thi lệnh khi chưa login!");
            }
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    private static void cleanupUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (Exception ignored) {}
    }
}

