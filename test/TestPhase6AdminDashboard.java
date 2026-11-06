package test;

import server.CommandHandler;
import server.Session;
import server.StorageManager;
import server.UserDatabase;
import server.dao.OrderDAO;
import server.dao.UserDAO;
import server.model.User;

import java.io.IOException;

/**
 * BỘ KIỂM THỬ TỰ ĐỘNG TOÀN DIỆN CHO PHASE 6:
 * PROFESSIONAL UI/UX VÀ ADMIN DASHBOARD (RBAC & MANAGEMENT)
 *
 * Kiểm tra 10 kịch bản cốt lõi:
 * 1. Admin gọi ADMINSTATS thành công (200 STATS)
 * 2. User thường bị chặn khi gọi ADMINSTATS (550 Permission Denied)
 * 3. User thường bị chặn khi gọi USERLIST (550 Permission Denied)
 * 4. Admin gọi USERLIST thành công (150 User list follows)
 * 5. Admin khóa tài khoản (SETUSERSTATUS LOCKED) và xác thực đăng nhập bị chặn
 * 6. Admin mở khóa tài khoản (SETUSERSTATUS ACTIVE) và đăng nhập bình thường
 * 7. Admin không thể tự khóa tài khoản chính mình (Self-protection)
 * 8. Phân quyền vai trò: Nâng quyền ROLE_ADMIN và hạ quyền ROLE_USER
 * 9. Điều chỉnh hạn mức Quota từ Admin (SETUSERQUOTA)
 * 10. Truy xuất toàn bộ đơn hàng hệ thống (ADMINALLORDERS) và tính toán KPI SystemStats
 */
public class TestPhase6AdminDashboard {

    private static int passedCount = 0;
    private static int failedCount = 0;

    private static final UserDAO userDAO = new UserDAO();
    private static final OrderDAO orderDAO = new OrderDAO();
    private static StorageManager storageManager;
    private static final UserDatabase userDatabase = new UserDatabase();

    private static User adminUser;
    private static User normalUser;
    private static User targetTestUser;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   BẮT ĐẦU CHẠY BỘ KIỂM THỬ TỰ ĐỘNG PHASE 6: ADMIN DASHBOARD");
        System.out.println("==================================================================\n");

        try {
            storageManager = new StorageManager();
        } catch (IOException e) {
            System.err.println("Không thể khởi tạo StorageManager: " + e.getMessage());
            System.exit(1);
        }

        setupTestData();

        try {
            test01_AdminStatsEndpointAllowedForAdmin();
            test02_AdminStatsEndpointForbiddenForNormalUser();
            test03_UserListSecurityVerification();
            test04_LockUserAndVerifyLoginBlocked();
            test05_UnlockUserAndVerifyLoginRestored();
            test06_AdminCannotLockSelf();
            test07_SetUserRolePromotionAndDemotion();
            test08_AdminCannotDemoteSelf();
            test09_SetUserQuota();
            test10_AdminAllOrdersAndSystemStatsKPI();
        } finally {
            cleanUpTestData();
        }

        System.out.println("\n==================================================================");
        System.out.println(String.format("   KẾT QUẢ KIỂM THỬ PHASE 6: %d PASSED, %d FAILED", passedCount, failedCount));
        System.out.println("==================================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void setupTestData() {
        System.out.println("[SETUP] Chuẩn bị dữ liệu kiểm thử...");
        adminUser = userDAO.findByUsername("admin");
        if (adminUser == null) {
            userDAO.registerUser("admin", "123", "admin@ftp.local");
            adminUser = userDAO.findByUsername("admin");
        }
        // Đảm bảo admin có ROLE_ADMIN (roleId = 1)
        userDAO.updateRole(adminUser.getUserId(), 1);
        adminUser = userDAO.findByUsername("admin");

        normalUser = userDAO.findByUsername("p6_normal_user");
        if (normalUser == null) {
            userDAO.registerUser("p6_normal_user", "pass123", "p6_normal@ftp.local");
            normalUser = userDAO.findByUsername("p6_normal_user");
        }
        // Đảm bảo user thường là ROLE_USER (roleId = 2)
        userDAO.updateRole(normalUser.getUserId(), 2);
        normalUser = userDAO.findByUsername("p6_normal_user");

        targetTestUser = userDAO.findByUsername("p6_target_user");
        if (targetTestUser == null) {
            userDAO.registerUser("p6_target_user", "testpass", "p6_target@ftp.local");
            targetTestUser = userDAO.findByUsername("p6_target_user");
        }
        userDAO.updateRole(targetTestUser.getUserId(), 2);
        userDAO.updateUserActive(targetTestUser.getUserId(), true);
        targetTestUser = userDAO.findByUsername("p6_target_user");

        System.out.println("[SETUP] Hoàn tất chuẩn bị tài khoản admin, normalUser và targetTestUser.\n");
    }

    private static void cleanUpTestData() {
        System.out.println("\n[CLEANUP] Dọn dẹp dữ liệu kiểm thử test user...");
        if (targetTestUser != null) {
            userDAO.updateUserActive(targetTestUser.getUserId(), true);
            userDAO.updateRole(targetTestUser.getUserId(), 2);
        }
        System.out.println("[CLEANUP] Đã hoàn tất dọn dẹp.");
    }

    private static CommandHandler createHandlerForUser(User user) {
        Session session = new Session();
        session.loginSuccess(user);
        return new CommandHandler(session, userDatabase, storageManager);
    }

    private static void test01_AdminStatsEndpointAllowedForAdmin() {
        System.out.print("[TEST 01] Admin gọi ADMINSTATS hợp lệ: ");
        CommandHandler adminHandler = createHandlerForUser(adminUser);
        String resp = adminHandler.handle("ADMINSTATS");
        if (resp != null && resp.startsWith("200 TotalUsers: ") && resp.contains("TotalStorage:")) {
            System.out.println("PASSED! (" + resp + ")");
            passedCount++;
        } else {
            System.out.println("FAILED! Phản hồi không đúng: " + resp);
            failedCount++;
        }
    }

    private static void test02_AdminStatsEndpointForbiddenForNormalUser() {
        System.out.print("[TEST 02] User thường gọi ADMINSTATS bị từ chối 550: ");
        CommandHandler userHandler = createHandlerForUser(normalUser);
        String resp = userHandler.handle("ADMINSTATS");
        if (resp != null && resp.startsWith("550")) {
            System.out.println("PASSED! (Server chặn chính xác: " + resp + ")");
            passedCount++;
        } else {
            System.out.println("FAILED! Server không chặn user thường: " + resp);
            failedCount++;
        }
    }

    private static void test03_UserListSecurityVerification() {
        System.out.print("[TEST 03] Phân quyền lệnh USERLIST (User bị chặn, Admin được phép): ");
        CommandHandler userHandler = createHandlerForUser(normalUser);
        String userResp = userHandler.handle("USERLIST");
        boolean userBlocked = (userResp != null && userResp.startsWith("550"));

        CommandHandler adminHandler = createHandlerForUser(adminUser);
        String adminResp = adminHandler.handle("USERLIST");
        boolean adminAllowed = (adminResp != null && adminResp.startsWith("150"));

        if (userBlocked && adminAllowed) {
            System.out.println("PASSED! (User bị 550, Admin nhận 150)");
            passedCount++;
        } else {
            System.out.println("FAILED! userBlocked=" + userBlocked + ", adminAllowed=" + adminAllowed);
            failedCount++;
        }
    }

    private static void test04_LockUserAndVerifyLoginBlocked() {
        System.out.print("[TEST 04] Admin khóa tài khoản (SETUSERSTATUS LOCKED) & Chặn đăng nhập: ");
        CommandHandler adminHandler = createHandlerForUser(adminUser);
        String resp = adminHandler.handle("SETUSERSTATUS " + targetTestUser.getUsername() + " LOCKED");

        User authResult = userDAO.validateUser(targetTestUser.getUsername(), "testpass");

        if (resp != null && resp.startsWith("200") && authResult == null) {
            System.out.println("PASSED! (Tài khoản đã bị khóa, validateUser() trả về null)");
            passedCount++;
        } else {
            System.out.println("FAILED! resp=" + resp + ", authResult=" + authResult);
            failedCount++;
        }
    }

    private static void test05_UnlockUserAndVerifyLoginRestored() {
        System.out.print("[TEST 05] Admin mở khóa tài khoản (SETUSERSTATUS ACTIVE) & Đăng nhập phục hồi: ");
        CommandHandler adminHandler = createHandlerForUser(adminUser);
        String resp = adminHandler.handle("SETUSERSTATUS " + targetTestUser.getUsername() + " ACTIVE");

        User authResult = userDAO.validateUser(targetTestUser.getUsername(), "testpass");

        if (resp != null && resp.startsWith("200") && authResult != null && authResult.isActive()) {
            System.out.println("PASSED! (Tài khoản mở khóa thành công, đăng nhập bình thường)");
            passedCount++;
        } else {
            System.out.println("FAILED! resp=" + resp + ", authResult=" + authResult);
            failedCount++;
        }
    }

    private static void test06_AdminCannotLockSelf() {
        System.out.print("[TEST 06] Tự bảo vệ hệ thống: Admin không thể tự khóa chính mình: ");
        CommandHandler adminHandler = createHandlerForUser(adminUser);
        String resp = adminHandler.handle("SETUSERSTATUS " + adminUser.getUsername() + " LOCKED");

        if (resp != null && resp.startsWith("550") && resp.contains("own admin")) {
            System.out.println("PASSED! (Server từ chối với: " + resp + ")");
            passedCount++;
        } else {
            System.out.println("FAILED! Server cho phép admin tự khóa mình: " + resp);
            failedCount++;
        }
    }

    private static void test07_SetUserRolePromotionAndDemotion() {
        System.out.print("[TEST 07] Thay đổi vai trò (SETUSERROLE ROLE_ADMIN / ROLE_USER): ");
        CommandHandler adminHandler = createHandlerForUser(adminUser);

        // Nâng cấp quyền
        String promoteResp = adminHandler.handle("SETUSERROLE " + targetTestUser.getUsername() + " ROLE_ADMIN");
        User updatedUser1 = userDAO.findByUsername(targetTestUser.getUsername());
        boolean promoteOk = promoteResp.startsWith("200") && "ROLE_ADMIN".equals(updatedUser1.getRoleName());

        // Hạ quyền lại
        String demoteResp = adminHandler.handle("SETUSERROLE " + targetTestUser.getUsername() + " ROLE_USER");
        User updatedUser2 = userDAO.findByUsername(targetTestUser.getUsername());
        boolean demoteOk = demoteResp.startsWith("200") && "ROLE_USER".equals(updatedUser2.getRoleName());

        if (promoteOk && demoteOk) {
            System.out.println("PASSED! (Thăng cấp và hạ quyền thành công)");
            passedCount++;
        } else {
            System.out.println("FAILED! promoteOk=" + promoteOk + ", demoteOk=" + demoteOk);
            failedCount++;
        }
    }

    private static void test08_AdminCannotDemoteSelf() {
        System.out.print("[TEST 08] Tự bảo vệ hệ thống: Admin không thể tự hạ quyền chính mình: ");
        CommandHandler adminHandler = createHandlerForUser(adminUser);
        String resp = adminHandler.handle("SETUSERROLE " + adminUser.getUsername() + " ROLE_USER");

        if (resp != null && resp.startsWith("550") && resp.contains("own admin")) {
            System.out.println("PASSED! (Server từ chối hạ quyền tài khoản hiện tại: " + resp + ")");
            passedCount++;
        } else {
            System.out.println("FAILED! Server cho phép admin tự hạ quyền: " + resp);
            failedCount++;
        }
    }

    private static void test09_SetUserQuota() {
        System.out.print("[TEST 09] Thay đổi Quota từ Admin (SETUSERQUOTA 350): ");
        CommandHandler adminHandler = createHandlerForUser(adminUser);
        String resp = adminHandler.handle("SETUSERQUOTA " + targetTestUser.getUsername() + " 350");

        User updatedUser = userDAO.findByUsername(targetTestUser.getUsername());
        long expectedQuotaBytes = 350L * 1024L * 1024L;

        if (resp != null && resp.startsWith("200") && updatedUser != null && updatedUser.getQuotaBytes() == expectedQuotaBytes) {
            System.out.println("PASSED! (Quota cập nhật chuẩn xác: " + updatedUser.getQuotaBytes() + " bytes)");
            passedCount++;
        } else {
            System.out.println("FAILED! resp=" + resp + ", quota=" + (updatedUser != null ? updatedUser.getQuotaBytes() : "null"));
            failedCount++;
        }
    }

    private static void test10_AdminAllOrdersAndSystemStatsKPI() {
        System.out.print("[TEST 10] Truy xuất ADMINALLORDERS & KPI thống kê hệ thống (getSystemStats): ");
        CommandHandler adminHandler = createHandlerForUser(adminUser);
        String ordersResp = adminHandler.handle("ADMINALLORDERS");

        UserDAO.SystemStats stats = userDAO.getSystemStats();
        boolean hasStats = (stats != null && stats.totalUsers >= 1);

        boolean ordersOk = (ordersResp != null && ordersResp.startsWith("150 System orders list"));

        if (hasStats && ordersOk) {
            System.out.println("PASSED! (KPI: Users=" + stats.totalUsers + ", Active=" + stats.activeUsers + ", Orders=" + stats.totalOrders + ")");
            passedCount++;
        } else {
            System.out.println("FAILED! ordersOk=" + ordersOk + ", hasStats=" + hasStats);
            failedCount++;
        }
    }
}
