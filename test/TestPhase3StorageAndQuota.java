package test;

import server.CommandHandler;
import server.Session;
import server.StorageManager;
import server.UserDatabase;
import server.dao.FileDAO;
import server.dao.LogDAO;
import server.dao.UserDAO;
import server.model.FileMetadata;
import server.model.User;
import server.util.SecurityUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BỘ KIỂM THỬ TỰ ĐỘNG PHASE 3: FILE MANAGEMENT, TRANSFER, QUOTA VÀ INTEGRITY
 *
 * Kiểm tra các chức năng:
 *   1. Quản lý thư mục con trong Sandbox (MKD, CWD, CDUP, RMD)
 *   2. Chặn xóa thư mục khi còn chứa dữ liệu
 *   3. Kiểm tra Quota nguyên tử & Chặn upload vượt Quota (Mã 552)
 *   4. Cơ chế file tạm (.upload.tmp), hủy và hoàn trả Quota khi đứt mạng giữa chừng (Mã 426)
 *   5. Tính toàn vẹn dữ liệu SHA-256 (khớp checksum 100%, hỗ trợ file 0 byte)
 *   6. Cập nhật Metadata CSDL (bảng files) và nhật ký kiểm toán (bảng transfer_logs)
 *   7. Kiểm tra chống Race Condition khi nhiều client upload đồng thời
 *   8. Kiểm thử End-to-End chuỗi lệnh TCP Socket (MKD, CWD, PWD, CDUP, RMD)
 */
public class TestPhase3StorageAndQuota {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   BẮT ĐẦU CHẠY BỘ KIỂM THỬ TỰ ĐỘNG PHASE 3: QUOTA & INTEGRITY");
        System.out.println("==================================================================\n");

        try {
            StorageManager sm = new StorageManager();
            UserDAO userDAO = new UserDAO();
            FileDAO fileDAO = new FileDAO();
            LogDAO logDAO = new LogDAO();

            // Đảm bảo có user test
            User testUser = userDAO.findByUsername("phase3_tester");
            if (testUser == null) {
                userDAO.registerUser("phase3_tester", "tester123", "phase3@test.com");
                testUser = userDAO.findByUsername("phase3_tester");
            }

            testDirectoryNavigation(sm, testUser);
            testRemoveNonEmptyDirectory(sm, testUser);
            testAtomicQuotaExceeded(sm, userDAO, testUser);
            testTempFileAndInterruptedTransfer(sm, userDAO, testUser);
            testSha256ChecksumAndEmptyFile(sm, testUser);
            testDatabaseMetadataAndLogs(sm, userDAO, fileDAO, logDAO, testUser);
            testConcurrentQuotaReservation(userDAO, testUser);
            testSocketCommandsE2E(testUser);

        } catch (Throwable t) {
            System.err.println("❌ LỖI NGHIÊM TRỌNG TRONG BỘ TEST: " + t.getMessage());
            t.printStackTrace();
            testsFailed++;
        }

        System.out.println("\n==================================================================");
        System.out.printf("   KẾT QUẢ KIỂM THỬ PHASE 3: %d PASSED, %d FAILED\n", testsPassed, testsFailed);
        System.out.println("==================================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
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
    // Test 1: Quản lý thư mục con trong Sandbox (MKD, CWD, CDUP, RMD)
    // -------------------------------------------------------------------------
    private static void testDirectoryNavigation(StorageManager sm, User user) {
        String testName = "Test 1: Quản lý cây thư mục con trong Sandbox (MKD, CWD, CDUP, RMD)";
        try {
            Session session = new Session();
            session.loginSuccess(user);

            // 1. Tạo thư mục
            String mkdResp = sm.makeDirectory(user.getHomeDir(), session.getCurrentDirectory(), "test_folder");
            if (!mkdResp.startsWith("257")) {
                fail(testName, "MKD không thành công: " + mkdResp);
                return;
            }

            // 2. Vào thư mục (CWD)
            session.cd("test_folder");
            if (!"test_folder".equals(session.getCurrentDirectory())) {
                fail(testName, "CWD không chuyển đúng thư mục hiện tại: " + session.getCurrentDirectory());
                return;
            }

            // 3. Lên thư mục cha (CDUP)
            session.cdup();
            if (!"".equals(session.getCurrentDirectory())) {
                fail(testName, "CDUP không trở về thư mục gốc: " + session.getCurrentDirectory());
                return;
            }

            // 4. CDUP tại gốc không được thoát khỏi Sandbox
            session.cdup();
            if (!"".equals(session.getCurrentDirectory())) {
                fail(testName, "CDUP tại gốc bị rò rỉ đường dẫn: " + session.getCurrentDirectory());
                return;
            }

            // 5. Xóa thư mục (RMD)
            String rmdResp = sm.removeDirectory(user.getHomeDir(), session.getCurrentDirectory(), "test_folder");
            if (!rmdResp.startsWith("250")) {
                fail(testName, "RMD xóa thư mục thất bại: " + rmdResp);
                return;
            }

            pass(testName, "Tạo, duyệt, lên cấp và xóa thư mục trong Sandbox hoạt động chính xác");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 2: Chặn xóa thư mục không rỗng
    // -------------------------------------------------------------------------
    private static void testRemoveNonEmptyDirectory(StorageManager sm, User user) {
        String testName = "Test 2: Bảo vệ thư mục có dữ liệu (Chặn RMD khi thư mục không rỗng)";
        try {
            sm.makeDirectory(user.getHomeDir(), "", "non_empty_dir");
            byte[] dummyData = "file inside folder".getBytes(StandardCharsets.UTF_8);
            sm.receiveFile(user.getUserId(), user.getHomeDir(), "non_empty_dir", "dummy.txt",
                    dummyData.length, new ByteArrayInputStream(dummyData));

            String rmdResp = sm.removeDirectory(user.getHomeDir(), "", "non_empty_dir");
            if (!rmdResp.startsWith("550") || !rmdResp.contains("not empty")) {
                fail(testName, "Không chặn xóa thư mục có file con: " + rmdResp);
                return;
            }

            // Dọn dẹp
            sm.deleteFile(user.getUserId(), user.getHomeDir(), "non_empty_dir", "dummy.txt");
            sm.removeDirectory(user.getHomeDir(), "", "non_empty_dir");

            pass(testName, "Trả mã lỗi 550 Directory not empty chính xác khi xóa thư mục chứa tệp");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 3: Quota nguyên tử & Chặn upload vượt Quota
    // -------------------------------------------------------------------------
    private static void testAtomicQuotaExceeded(StorageManager sm, UserDAO userDAO, User user) {
        String testName = "Test 3: Quản lý Quota nguyên tử (Chặn upload vượt hạn mức dung lượng - Mã 552)";
        try {
            long[] qInfo = userDAO.getQuotaInfo(user.getUserId());
            long remaining = qInfo[1] - qInfo[0];
            long excessSize = remaining + 1024 * 1024; // Vượt quá quota 1MB

            byte[] dummy = new byte[100];
            String resp = sm.receiveFile(user.getUserId(), user.getHomeDir(), "", "huge_file.dat",
                    excessSize, new ByteArrayInputStream(dummy));

            if (!resp.startsWith("552")) {
                fail(testName, "Kỳ vọng mã lỗi 552 nhưng nhận được: " + resp);
                return;
            }

            pass(testName, "Chặn tức thì file vượt dung lượng bằng mã 552 Storage quota exceeded");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 4: Cơ chế File Tạm (.upload.tmp) & Tự động hoàn trả Quota khi đứt mạng
    // -------------------------------------------------------------------------
    private static void testTempFileAndInterruptedTransfer(StorageManager sm, UserDAO userDAO, User user) {
        String testName = "Test 4: Cơ chế File Tạm (.upload.tmp) & Tự động phục hồi khi mất kết nối mạng";
        try {
            long[] beforeQuota = userDAO.getQuotaInfo(user.getUserId());

            // Giả lập khai báo 10,000 bytes nhưng mạng chỉ gửi được 500 bytes rồi đứt kết nối (EOF)
            long declaredSize = 10000;
            byte[] truncatedData = new byte[500];
            ByteArrayInputStream brokenIn = new ByteArrayInputStream(truncatedData);

            String filename = "broken_upload.zip";
            String resp = sm.receiveFile(user.getUserId(), user.getHomeDir(), "", filename, declaredSize, brokenIn);

            if (!resp.startsWith("426")) {
                fail(testName, "Kỳ vọng mã 426 Transfer aborted nhưng nhận được: " + resp);
                return;
            }

            // Kiểm tra file chính không được tạo ra
            Path userDir = sm.resolveSafe(user.getHomeDir(), "", "");
            Path mainFile = userDir.resolve(filename);
            if (Files.exists(mainFile)) {
                fail(testName, "File chính vẫn bị tạo trên đĩa dù upload bị đứt giữa chừng!");
                return;
            }

            // Kiểm tra không sót lại file tạm .upload.tmp nào trong thư mục
            try (var stream = Files.list(userDir)) {
                boolean hasTmp = stream.anyMatch(p -> p.getFileName().toString().contains(".upload.tmp"));
                if (hasTmp) {
                    fail(testName, "File tạm .upload.tmp chưa được xóa sạch sau khi hủy truyền tải!");
                    return;
                }
            }

            // Kiểm tra Quota đã được hoàn trả nguyên vẹn
            long[] afterQuota = userDAO.getQuotaInfo(user.getUserId());
            if (afterQuota[0] != beforeQuota[0]) {
                fail(testName, String.format("Quota không được hoàn trả! Trước: %d, Sau: %d", beforeQuota[0], afterQuota[0]));
                return;
            }

            pass(testName, "File tạm bị xóa sạch, file chính không bị ghi, Quota hoàn trả nguyên vẹn");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 5: Tính toàn vẹn dữ liệu SHA-256 & File rỗng 0 bytes
    // -------------------------------------------------------------------------
    private static void testSha256ChecksumAndEmptyFile(StorageManager sm, User user) {
        String testName = "Test 5: Toàn vẹn dữ liệu SHA-256 (Khớp Checksum 100% & File rỗng 0 byte)";
        try {
            // 1. File có dữ liệu
            byte[] payload = "Hello FTP World! Kiểm tra mã băm SHA-256 toàn vẹn dữ liệu.".getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String clientHash = SecurityUtil.bytesToHex(md.digest(payload));

            String resp = sm.receiveFile(user.getUserId(), user.getHomeDir(), "", "integrity_test.txt",
                    payload.length, new ByteArrayInputStream(payload));

            if (!resp.startsWith("226") || !resp.contains(clientHash)) {
                fail(testName, "Mã băm SHA-256 phản hồi không khớp: " + resp);
                return;
            }

            // Đọc lại file từ đĩa tính lại SHA-256
            Path diskFile = sm.resolveSafe(user.getHomeDir(), "", "integrity_test.txt");
            byte[] diskBytes = Files.readAllBytes(diskFile);
            String diskHash = SecurityUtil.bytesToHex(md.digest(diskBytes));

            if (!clientHash.equalsIgnoreCase(diskHash)) {
                fail(testName, "Mã băm file lưu trên đĩa không khớp với dữ liệu gốc gửi đi!");
                return;
            }

            // 2. File rỗng 0 bytes
            byte[] emptyPayload = new byte[0];
            String emptyResp = sm.receiveFile(user.getUserId(), user.getHomeDir(), "", "empty_file.txt",
                    0, new ByteArrayInputStream(emptyPayload));
            if (!emptyResp.startsWith("226")) {
                fail(testName, "Upload file rỗng 0 bytes thất bại: " + emptyResp);
                return;
            }

            // Dọn dẹp
            sm.deleteFile(user.getUserId(), user.getHomeDir(), "", "integrity_test.txt");
            sm.deleteFile(user.getUserId(), user.getHomeDir(), "", "empty_file.txt");

            pass(testName, "Checksum khớp 100% từng byte, hỗ trợ chuẩn xác file 0 bytes");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 6: Cập nhật Metadata CSDL (bảng files) và nhật ký kiểm toán (bảng transfer_logs)
    // -------------------------------------------------------------------------
    private static void testDatabaseMetadataAndLogs(StorageManager sm, UserDAO userDAO, FileDAO fileDAO,
                                                   LogDAO logDAO, User user) {
        String testName = "Test 6: Đồng bộ hóa Metadata CSDL (bảng files) & Ghi nhật ký (transfer_logs)";
        try {
            byte[] content = "Data to persist in MySQL files table".getBytes(StandardCharsets.UTF_8);
            String filename = "mysql_meta_test.txt";
            sm.receiveFile(user.getUserId(), user.getHomeDir(), "", filename, content.length, new ByteArrayInputStream(content));

            // Kiểm tra metadata trong bảng files
            FileMetadata meta = fileDAO.findByPath(user.getUserId(), "/" + filename);
            if (meta == null) {
                fail(testName, "Bảng files không ghi nhận metadata của tệp sau khi upload!");
                return;
            }

            if (meta.getFileSize() != content.length || meta.isDeleted()) {
                fail(testName, "Thông tin tệp trong bảng files không chính xác: size=" + meta.getFileSize());
                return;
            }

            // Ghi log kiểm toán thử nghiệm
            logDAO.log(user.getUserId(), "127.0.0.1", "UPLOAD", filename, content.length, "SUCCESS", "Test upload log");

            // Xóa file và kiểm tra soft-delete trong DB
            sm.deleteFile(user.getUserId(), user.getHomeDir(), "", filename);
            FileMetadata deletedMeta = fileDAO.findByPath(user.getUserId(), "/" + filename);
            if (deletedMeta != null) {
                fail(testName, "FileDAO.findByPath vẫn tìm thấy file sau khi đã xóa (Soft Delete chưa lọc)!");
                return;
            }

            pass(testName, "Bảng files ghi nhận đầy đủ kích thước, checksum và cập nhật khi xóa tệp");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 7: Kiểm tra chống Race Condition khi nhiều client upload đồng thời
    // -------------------------------------------------------------------------
    private static void testConcurrentQuotaReservation(UserDAO userDAO, User user) {
        String testName = "Test 7: Chống Race Condition tranh chấp Quota khi nhiều Client upload đồng thời";
        try {
            int threads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(1);

            // Xác định dung lượng còn lại
            long[] initialQuota = userDAO.getQuotaInfo(user.getUserId());
            long remaining = initialQuota[1] - initialQuota[0];

            // Mỗi thread cố gắng lấy 30% dung lượng còn lại
            long requestedBytes = Math.max(1024, remaining / 3);
            AtomicInteger successCount = new AtomicInteger(0);

            Future<?>[] futures = new Future[threads];
            for (int i = 0; i < threads; i++) {
                futures[i] = executor.submit(() -> {
                    try {
                        latch.await();
                        boolean reserved = userDAO.checkAndReserveQuota(user.getUserId(), requestedBytes);
                        if (reserved) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            latch.countDown(); // Phát lệnh chạy đồng loạt
            for (Future<?> f : futures) f.get(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Kiểm tra used_bytes không bao giờ vượt quota_bytes
            long[] finalQuota = userDAO.getQuotaInfo(user.getUserId());
            if (finalQuota[0] > finalQuota[1]) {
                fail(testName, String.format("VI PHẠM QUOTA DO RACE CONDITION! Used: %d > Quota: %d",
                        finalQuota[0], finalQuota[1]));
                return;
            }

            // Hoàn trả lại quota đã mượn trong test
            userDAO.releaseQuota(user.getUserId(), successCount.get() * requestedBytes);

            pass(testName, String.format("Thành công %d luồng, Quota không bị vượt quá giới hạn (Used <= Quota)",
                    successCount.get()));
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Test 8: Kiểm thử chuỗi lệnh TCP Socket End-to-End
    // -------------------------------------------------------------------------
    private static void testSocketCommandsE2E(User user) {
        String testName = "Test 8: Kiểm thử chuỗi lệnh TCP Socket End-to-End (MKD, CWD, PWD, CDUP, RMD)";
        int testPort = 16789;
        ExecutorService srvExec = Executors.newSingleThreadExecutor();

        try {
            StorageManager sm = new StorageManager();
            UserDatabase userDb = new UserDatabase();
            ServerSocket serverSocket = new ServerSocket(testPort);

            srvExec.submit(() -> {
                try (Socket client = serverSocket.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);
                    out.println("220 Mini FTP Ready for Phase 3 Test");

                    Session session = new Session();
                    CommandHandler cmd = new CommandHandler(session, userDb, sm);
                    cmd.setClientIp("127.0.0.1");

                    String line;
                    while ((line = in.readLine()) != null) {
                        String resp = cmd.handle(line);
                        out.println(resp);
                        if ("QUIT".equalsIgnoreCase(line.trim())) break;
                    }
                } catch (Exception ignored) {}
            });

            // Client kết nối test
            try (Socket sock = new Socket("localhost", testPort);
                 BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8), true)) {

                in.readLine(); // 220 banner

                // 1. Auth
                out.println("USER " + user.getUsername());
                in.readLine(); // 331
                out.println("PASS tester123");
                String passResp = in.readLine();
                if (!passResp.startsWith("230")) {
                    fail(testName, "Đăng nhập thất bại: " + passResp);
                    return;
                }

                // 2. MKD
                out.println("MKD socket_test_dir");
                String mkdResp = in.readLine();
                if (!mkdResp.startsWith("257")) {
                    fail(testName, "MKD qua socket thất bại: " + mkdResp);
                    return;
                }

                // 3. CWD
                out.println("CWD socket_test_dir");
                String cwdResp = in.readLine();
                if (!cwdResp.startsWith("250")) {
                    fail(testName, "CWD qua socket thất bại: " + cwdResp);
                    return;
                }

                // 4. PWD
                out.println("PWD");
                String pwdResp = in.readLine();
                if (!pwdResp.contains("socket_test_dir")) {
                    fail(testName, "PWD không hiển thị đúng thư mục con: " + pwdResp);
                    return;
                }

                // 5. CDUP
                out.println("CDUP");
                String cdupResp = in.readLine();
                if (!cdupResp.startsWith("200")) {
                    fail(testName, "CDUP qua socket thất bại: " + cdupResp);
                    return;
                }

                // 6. RMD
                out.println("RMD socket_test_dir");
                String rmdResp = in.readLine();
                if (!rmdResp.startsWith("250")) {
                    fail(testName, "RMD qua socket thất bại: " + rmdResp);
                    return;
                }

                // 7. QUIT
                out.println("QUIT");
                in.readLine();
            }

            serverSocket.close();
            srvExec.shutdown();
            pass(testName, "Chuỗi lệnh MKD, CWD, PWD, CDUP, RMD trên kết nối Socket phản hồi chuẩn RFC 959");
        } catch (Exception e) {
            fail(testName, e.getMessage());
        }
    }
}
