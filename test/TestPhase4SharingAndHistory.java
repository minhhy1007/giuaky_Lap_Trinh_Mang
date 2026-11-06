package test;

import server.CommandHandler;
import server.Session;
import server.StorageManager;
import server.UserDatabase;
import server.dao.FileDAO;
import server.dao.LogDAO;
import server.dao.ShareDAO;
import server.dao.UserDAO;
import server.model.FileMetadata;
import server.model.SharedFile;
import server.model.TransferLog;
import server.model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;

/**
 * BỘ KIỂM THỬ TỰ ĐỘNG PHASE 4: SHARING, ACCESS CONTROL VÀ ACTIVITY HISTORY
 *
 * Kiểm tra các chức năng:
 *   1. Chia sẻ tệp đích danh theo tài khoản (Account-based Sharing)
 *   2. Kiểm soát truy cập: Người không được chỉ định bị từ chối truy cập (403/550)
 *   3. Thu hồi quyền chia sẻ (Revoke Share) có hiệu lực tức thì
 *   4. Chặn liên kết/Token đã hết hạn (Expired Token Protection)
 *   5. Chặn liên kết đã đạt số lượt tải tối đa (Max Downloads Limit)
 *   6. Chống tấn công giả mạo IDOR: Không thể thu hồi share của người khác
 *   7. Phân quyền kiểm toán RBAC: USER chỉ thấy log của mình, ADMIN xem toàn bộ
 *   8. Bảo mật kiểm toán: Không để rò rỉ Token bí mật vào transfer_logs
 */
public class TestPhase4SharingAndHistory {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   BẮT ĐẦU CHẠY BỘ KIỂM THỬ TỰ ĐỘNG PHASE 4: SHARING & AUDIT LOGS");
        System.out.println("==================================================================\n");

        try {
            StorageManager sm = new StorageManager();
            UserDAO userDAO = new UserDAO();
            FileDAO fileDAO = new FileDAO();
            ShareDAO shareDAO = new ShareDAO();
            LogDAO logDAO = new LogDAO();

            // Chuẩn bị 3 User test: Alice (Owner), Bob (Recipient), Charlie (Attacker)
            User alice = ensureUser(userDAO, "p4_alice", "alice123", "p4_alice@test.com", 2);
            User bob = ensureUser(userDAO, "p4_bob", "bob123", "p4_bob@test.com", 2);
            User charlie = ensureUser(userDAO, "p4_charlie", "charlie123", "p4_charlie@test.com", 2);
            User admin = ensureUser(userDAO, "p4_admin", "admin123", "p4_admin@test.com", 1);

            // Chuẩn bị 1 file mẫu thuộc sở hữu của Alice
            byte[] fileData = "Nội dung tệp tài liệu chia sẻ bảo mật Phase 4.".getBytes(StandardCharsets.UTF_8);
            String filename = "p4_confidential.pdf";
            sm.receiveFile(alice.getUserId(), alice.getHomeDir(), "", filename, fileData.length, new ByteArrayInputStream(fileData));
            FileMetadata meta = fileDAO.findByFilename(alice.getUserId(), "", filename);

            testAccountBasedSharing(shareDAO, meta, alice, bob);
            testUnauthorizedAccessRejected(shareDAO, meta, alice, bob, charlie);
            testRevokeShare(shareDAO, meta, alice, bob);
            testExpiredTokenProtection(shareDAO, meta, alice);
            testMaxDownloadsLimit(shareDAO, sm, meta, alice);
            testIdorProtection(shareDAO, meta, alice, charlie);
            testRbacAuditLogs(logDAO, alice, admin);
            testAuditLogTokenSanitization(logDAO, shareDAO, alice);

            // Dọn dẹp tệp mẫu
            sm.deleteFile(alice.getUserId(), alice.getHomeDir(), "", filename);

        } catch (Throwable t) {
            System.err.println("❌ LỖI NGHIÊM TRỌNG TRONG BỘ TEST PHASE 4: " + t.getMessage());
            t.printStackTrace();
            testsFailed++;
        }

        System.out.println("\n==================================================================");
        System.out.printf("   KẾT QUẢ KIỂM THỬ PHASE 4: %d PASSED, %d FAILED\n", testsPassed, testsFailed);
        System.out.println("==================================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static User ensureUser(UserDAO dao, String username, String pass, String email, int roleId) {
        User u = dao.findByUsername(username);
        if (u == null) {
            dao.registerUser(username, pass, email);
            u = dao.findByUsername(username);
            if (roleId == 1 && u != null) {
                // Đổi role thành ADMIN nếu cần
                dao.updateRole(u.getUserId(), 1);
                u = dao.findByUsername(username);
            }
        }
        return u;
    }

    private static void pass(String testName, String detail) {
        testsPassed++;
        System.out.printf("[PASS] %s (%s)\n", testName, detail);
    }

    private static void fail(String testName, String reason) {
        testsFailed++;
        System.err.printf("[FAIL] %s - NGUYÊN NHÂN: %s\n", testName, reason);
    }

    // -------------------------------------------------------------------------
    // Test 1: Chia sẻ đích danh theo tài khoản
    // -------------------------------------------------------------------------
    private static void testAccountBasedSharing(ShareDAO shareDAO, FileMetadata file, User owner, User recipient) {
        String testName = "Test 1: Chia sẻ tệp tin đích danh theo tài khoản (Account-based Sharing)";
        try {
            // Alice chia sẻ cho Bob với quyền DOWNLOAD, hạn 24 giờ
            Timestamp expires = new Timestamp(System.currentTimeMillis() + 24 * 3600 * 1000L);
            String token = shareDAO.createShare(file.getFileId(), owner.getUserId(), recipient.getUserId(), "DOWNLOAD", expires, 0);

            if (token == null || token.length() < 32) {
                fail(testName, "Không tạo được share_token hợp lệ: " + token);
                return;
            }

            // Bob truy vấn danh sách file được chia sẻ cho mình
            List<SharedFile> sharedWithBob = shareDAO.getSharedWithUser(recipient.getUserId());
            boolean found = sharedWithBob.stream().anyMatch(s -> s.getShareToken().equals(token));

            if (!found) {
                fail(testName, "Bob không thấy file được Alice chia sẻ trong danh sách Shared with me!");
                return;
            }

            // Bob xác thực truy cập bằng token
            SharedFile valid = shareDAO.validateAndGetShare(token, recipient.getUserId());
            if (valid == null || !"DOWNLOAD".equals(valid.getPermission())) {
                fail(testName, "Server không cấp quyền truy cập hợp lệ cho Bob!");
                return;
            }

            pass(testName, "Alice chia sẻ cho Bob thành công, Bob thấy trong danh sách và token hợp lệ");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 2: Người không được chỉ định bị từ chối
    // -------------------------------------------------------------------------
    private static void testUnauthorizedAccessRejected(ShareDAO shareDAO, FileMetadata file, User owner, User recipient, User charlie) {
        String testName = "Test 2: Kiểm soát truy cập (Chặn người không được chia sẻ sử dụng Token đích danh)";
        try {
            String token = shareDAO.createShare(file.getFileId(), owner.getUserId(), recipient.getUserId(), "DOWNLOAD", null, 0);

            // Charlie (kẻ tấn công) cố tình dùng token của Bob
            SharedFile unauthorized = shareDAO.validateAndGetShare(token, charlie.getUserId());
            if (unauthorized != null) {
                fail(testName, "LỖ HỔNG BẢO MẬT: Charlie được phép truy cập file chỉ chia sẻ cho Bob!");
                return;
            }

            pass(testName, "Server từ chối Charlie truy cập token đích danh của Bob (Access Denied)");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 3: Thu hồi quyền chia sẻ (Revoke Share)
    // -------------------------------------------------------------------------
    private static void testRevokeShare(ShareDAO shareDAO, FileMetadata file, User owner, User recipient) {
        String testName = "Test 3: Thu hồi quyền chia sẻ (Revoke Share có hiệu lực tức thì)";
        try {
            String token = shareDAO.createShare(file.getFileId(), owner.getUserId(), recipient.getUserId(), "DOWNLOAD", null, 0);
            SharedFile sf = shareDAO.validateAndGetShare(token, recipient.getUserId());

            // Alice thu hồi quyền
            boolean revoked = shareDAO.revokeShare(sf.getShareId(), owner.getUserId());
            if (!revoked) {
                fail(testName, "Thu hồi quyền chia sẻ thất bại!");
                return;
            }

            // Bob thử truy cập lại sau khi đã thu hồi
            SharedFile afterRevoke = shareDAO.validateAndGetShare(token, recipient.getUserId());
            if (afterRevoke != null) {
                fail(testName, "File đã bị thu hồi nhưng vẫn cho phép truy cập!");
                return;
            }

            pass(testName, "Sau khi chủ sở hữu thu hồi, token lập tức bị vô hiệu hóa 100%");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 4: Chặn liên kết / Token đã hết hạn
    // -------------------------------------------------------------------------
    private static void testExpiredTokenProtection(ShareDAO shareDAO, FileMetadata file, User owner) {
        String testName = "Test 4: Chặn liên kết chia sẻ đã hết hạn (Expired Token Protection)";
        try {
            // Tạo link công khai nhưng thời hạn ở 1 giờ trước trong quá khứ
            Timestamp expiredTime = new Timestamp(System.currentTimeMillis() - 3600 * 1000L);
            String token = shareDAO.createShare(file.getFileId(), owner.getUserId(), null, "DOWNLOAD", expiredTime, 0);

            SharedFile sf = shareDAO.validateAndGetShare(token, null);
            if (sf != null) {
                fail(testName, "Token đã hết hạn thời gian nhưng Server vẫn cho phép truy cập!");
                return;
            }

            pass(testName, "Server tự động từ chối truy cập khi thời hạn expires_at đã qua");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 5: Giới hạn số lượt tải tối đa
    // -------------------------------------------------------------------------
    private static void testMaxDownloadsLimit(ShareDAO shareDAO, StorageManager sm, FileMetadata file, User owner) {
        String testName = "Test 5: Giới hạn số lượt tải tối đa (Max Downloads Limit)";
        try {
            // Giới hạn chỉ được tải tối đa 1 lần
            String token = shareDAO.createShare(file.getFileId(), owner.getUserId(), null, "DOWNLOAD", null, 1);

            // Lần 1: Hợp lệ
            SharedFile sf1 = shareDAO.validateAndGetShare(token, null);
            if (sf1 == null) {
                fail(testName, "Lần tải đầu tiên bị từ chối!");
                return;
            }
            shareDAO.incrementAccessCount(sf1.getShareId()); // Giả lập đã tải xong 1 lần

            // Lần 2: Bị từ chối
            SharedFile sf2 = shareDAO.validateAndGetShare(token, null);
            if (sf2 != null) {
                fail(testName, "Đã vượt quá số lượt tải tối đa (1 lần) nhưng vẫn cho phép tải tiếp!");
                return;
            }

            pass(testName, "Chặn thành công lượt tải thứ 2 khi đã đạt max_downloads = 1");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 6: Chống tấn công giả mạo IDOR
    // -------------------------------------------------------------------------
    private static void testIdorProtection(ShareDAO shareDAO, FileMetadata file, User owner, User attacker) {
        String testName = "Test 6: Chống tấn công IDOR (Không cho người dùng thu hồi chia sẻ của người khác)";
        try {
            String token = shareDAO.createShare(file.getFileId(), owner.getUserId(), null, "DOWNLOAD", null, 0);
            SharedFile sf = shareDAO.validateAndGetShare(token, null);

            // Attacker (Charlie) cố tình gửi lệnh thu hồi shareId của Alice
            boolean ok = shareDAO.revokeShare(sf.getShareId(), attacker.getUserId());
            if (ok) {
                fail(testName, "LỖ HỔNG IDOR: Charlie có thể thu hồi lượt chia sẻ của Alice!");
                return;
            }

            // Kiểm tra lượt chia sẻ vẫn còn nguyên hiệu lực
            SharedFile stillValid = shareDAO.validateAndGetShare(token, null);
            if (stillValid == null) {
                fail(testName, "Chia sẻ bị hủy dù người thu hồi không có quyền sở hữu!");
                return;
            }

            pass(testName, "Server chặn đứng hành vi thu hồi trái phép, bảo vệ toàn vẹn quyền sở hữu");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 7: Phân quyền Nhật ký hoạt động RBAC
    // -------------------------------------------------------------------------
    private static void testRbacAuditLogs(LogDAO logDAO, User user, User admin) {
        String testName = "Test 7: Phân quyền kiểm toán RBAC (USER chỉ xem log của mình, ADMIN xem toàn bộ)";
        try {
            // Ghi 1 log cho User và 1 log cho Admin
            logDAO.log(user.getUserId(), "127.0.0.1", "UPLOAD", "user_file.txt", 100, "SUCCESS", "User upload");
            logDAO.log(admin.getUserId(), "127.0.0.1", "DELETE", "admin_file.txt", 200, "SUCCESS", "Admin delete");

            // 1. User thường xem log
            List<TransferLog> userLogs = logDAO.getLogsByUser(user.getUserId(), 50, 0, "ALL");
            boolean containsAdminLog = userLogs.stream().anyMatch(l -> (l.getUserId() != null && l.getUserId() == admin.getUserId()));
            if (containsAdminLog) {
                fail(testName, "LỖ HỔNG RIÊNG TƯ: User thường thấy được nhật ký của Admin!");
                return;
            }

            // 2. Admin xem log toàn bộ
            List<TransferLog> adminLogs = logDAO.getAllLogs(50, 0, "ALL", null);
            boolean seesUserLog = adminLogs.stream().anyMatch(l -> (l.getUserId() != null && l.getUserId() == user.getUserId()));
            boolean seesAdminLog = adminLogs.stream().anyMatch(l -> (l.getUserId() != null && l.getUserId() == admin.getUserId()));

            if (!seesUserLog || !seesAdminLog) {
                fail(testName, "Admin không thể xem được toàn bộ nhật ký hệ thống!");
                return;
            }

            pass(testName, "User chỉ thấy log cá nhân, Admin có toàn quyền tra cứu nhật ký hệ thống");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 8: Bảo mật kiểm toán (Không lưu lộ liễu Token bí mật)
    // -------------------------------------------------------------------------
    private static void testAuditLogTokenSanitization(LogDAO logDAO, ShareDAO shareDAO, User user) {
        String testName = "Test 8: Bảo mật nhật ký (Không ghi Token bí mật vào transfer_logs)";
        try {
            String fakeSecretToken = "token=1234567890abcdef1234567890abcdef";
            logDAO.log(user.getUserId(), "127.0.0.1", "SHARE", "secret.doc", 500, "SUCCESS", "Tạo chia sẻ " + fakeSecretToken);

            List<TransferLog> logs = logDAO.getLogsByUser(user.getUserId(), 10, 0, "SHARE");
            TransferLog targetLog = logs.stream().filter(l -> "secret.doc".equals(l.getFilename())).findFirst().orElse(null);

            if (targetLog != null && targetLog.getMessage() != null) {
                if (targetLog.getMessage().contains("1234567890abcdef1234567890abcdef")) {
                    fail(testName, "LỖI BẢO MẬT: Token bí mật bị lộ nguyên dạng trong thông điệp nhật ký!");
                    return;
                }
            }

            pass(testName, "Thông điệp log đã được tự động khử trùng (Sanitize), che giấu token an toàn");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }
}
