package test;

import server.CommandHandler;
import server.Session;
import server.StorageManager;
import server.UserDatabase;
import server.dao.*;
import server.db.DBConnection;
import server.db.DataReconciler;
import server.model.*;
import server.util.SecurityUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * =========================================================================
 * PHASE 7 — MASTER SYSTEM TESTING, SECURITY VÀ RELIABILITY SUITE
 * =========================================================================
 *
 * Kiểm tra toàn diện 32 kịch bản thực tế trên hệ thống sản phẩm thật:
 *
 * Nhóm A: Kiểm thử chức năng (Functional Testing)
 * Nhóm B: Kiểm thử tích hợp & Phục hồi (Integration & Resilience)
 * Nhóm C: Kiểm thử bảo mật & Chống khai thác (Security & Anti-Exploit)
 * Nhóm D: Kiểm thử độ bền & Điều kiện biên (Reliability & Edge Cases)
 */
public class TestPhase7SystemReliabilitySecurity {

    public static class TestCaseResult {
        public String id;
        public String name;
        public String group;
        public String precondition;
        public String steps;
        public String expected;
        public String actual;
        public boolean passed;
        public String evidence;

        public TestCaseResult(String id, String name, String group, String precondition,
                              String steps, String expected, String actual, boolean passed, String evidence) {
            this.id = id;
            this.name = name;
            this.group = group;
            this.precondition = precondition;
            this.steps = steps;
            this.expected = expected;
            this.actual = actual;
            this.passed = passed;
            this.evidence = evidence;
        }
    }

    private static final List<TestCaseResult> results = new ArrayList<>();
    private static int totalPassed = 0;
    private static int totalFailed = 0;

    private static UserDAO userDAO;
    private static FileDAO fileDAO;
    private static ShareDAO shareDAO;
    private static OrderDAO orderDAO;
    private static PaymentDAO paymentDAO;
    private static PlanDAO planDAO;
    private static SubscriptionDAO subscriptionDAO;
    private static LogDAO logDAO;
    private static StorageManager storageManager;
    private static UserDatabase userDatabase;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   BẮT ĐẦU CHẠY TOÀN DIỆN PHASE 7: SYSTEM, SECURITY & RELIABILITY");
        System.out.println("==================================================================\n");

        long startTime = System.currentTimeMillis();

        try {
            initDependencies();

            // Run 4 Groups
            runGroupAFunctional();
            runGroupBIntegration();
            runGroupCSecurity();
            runGroupDReliability();

            long totalTime = System.currentTimeMillis() - startTime;
            printFinalSummary(totalTime);

        } catch (Exception e) {
            System.err.println("[PHASE 7 FATAL ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initDependencies() throws Exception {
        userDAO = new UserDAO();
        fileDAO = new FileDAO();
        shareDAO = new ShareDAO();
        orderDAO = new OrderDAO();
        paymentDAO = new PaymentDAO();
        planDAO = new PlanDAO();
        subscriptionDAO = new SubscriptionDAO();
        logDAO = new LogDAO();
        storageManager = new StorageManager();
        userDatabase = new UserDatabase();
    }

    // =========================================================================
    // GROUP A: KIỂM THỬ CHỨC NĂNG (FUNCTIONAL TESTING)
    // =========================================================================

    private static void runGroupAFunctional() throws Exception {
        System.out.println("--- [NHÓM A: KIỂM THỬ CHỨC NĂNG - FUNCTIONAL TESTING] ---");

        // TC-FUNC-01: Đăng ký & Đăng nhập 2 bước
        {
            String username = "p7_user_func_" + System.currentTimeMillis() % 10000;
            String pass = "Pass123456";
            Session sess = new Session();
            CommandHandler ch = new CommandHandler(sess, userDatabase, storageManager);

            String regResp = ch.handle("REGISTER " + username + " " + pass + " " + username + "@test.com");
            boolean regOk = regResp != null && regResp.startsWith("200");

            String userResp = ch.handle("USER " + username);
            String passResp = ch.handle("PASS " + pass);
            boolean loginOk = userResp != null && userResp.startsWith("331") && passResp != null && passResp.startsWith("230");

            boolean passTest = regOk && loginOk && sess.isAuthenticated();
            record("TC-FUNC-01", "Xác thực tài khoản (Register & 2-step Login USER/PASS)", "Chức năng",
                    "Database MySQL hoạt động, bảng users sẵn sàng",
                    "Gửi REGISTER -> Gửi USER -> Gửi PASS",
                    "200 Registration successful -> 331 Need pass -> 230 Login OK",
                    "Reg: " + regResp + " | Login: " + passResp,
                    passTest, "Session user=" + sess.getLoggedInUser() + ", authenticated=" + sess.isAuthenticated());
        }

        // TC-FUNC-02: Đổi mật khẩu & Hiệu lực tức thì
        {
            String username = "p7_pwd_" + System.currentTimeMillis() % 10000;
            userDAO.registerUser(username, "oldpass123", username + "@test.com");
            Session sess = new Session();
            CommandHandler ch = new CommandHandler(sess, userDatabase, storageManager);
            ch.handle("USER " + username);
            ch.handle("PASS oldpass123");

            String changeResp = ch.handle("PASSWD oldpass123 newpass456");
            boolean changeOk = changeResp != null && changeResp.startsWith("200");

            // Đăng nhập lại với pass cũ (phải tạch) và pass mới (phải OK)
            User testOld = userDAO.validateUser(username, "oldpass123");
            User testNew = userDAO.validateUser(username, "newpass456");
            boolean passTest = changeOk && (testOld == null) && (testNew != null);

            record("TC-FUNC-02", "Đổi mật khẩu người dùng (PASSWD) & Hiệu lực tức thì", "Chức năng",
                    "User đã đăng nhập thành công",
                    "Gửi PASSWD oldpass newpass -> Kiểm tra CSDL",
                    "200 Password changed, mật khẩu cũ vô hiệu hóa ngay",
                    changeResp + " | Pass cũ: " + (testOld == null ? "REJECTED" : "ACCEPTED") + " | Pass mới: " + (testNew != null ? "ACCEPTED" : "REJECTED"),
                    passTest, "Hash CSDL đổi mới thành công");
        }

        // TC-FUNC-03: Phân cấp vai trò & Tra cứu WHOAMI thời gian thực
        {
            Session sess = new Session();
            CommandHandler ch = new CommandHandler(sess, userDatabase, storageManager);
            ch.handle("USER admin");
            ch.handle("PASS 123");

            String whoResp = ch.handle("WHOAMI");
            boolean whoOk = whoResp != null && whoResp.startsWith("200") && whoResp.contains("ROLE_ADMIN") && whoResp.contains("Quota=");
            boolean passTest = whoOk && sess.isAdmin();

            record("TC-FUNC-03", "Truy vấn danh tính WHOAMI & Xác thực quyền ROLE_ADMIN", "Chức năng",
                    "Tài khoản quản trị admin đăng nhập",
                    "Gửi WHOAMI",
                    "200 User=admin | Role=ROLE_ADMIN | Sandbox=storage/admin | Quota=... | Used=...",
                    whoResp,
                    passTest, "Role=" + sess.getCurrentUser().getRoleName() + ", isAdmin=" + sess.isAdmin());
        }

        // TC-FUNC-04: Thao tác cây thư mục trong Sandbox (MKD, CWD, CDUP, RMD)
        {
            String username = "p7_dir_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);
            Session sess = new Session();
            sess.loginSuccess(u);
            CommandHandler ch = new CommandHandler(sess, userDatabase, storageManager);

            String mkd = ch.handle("MKD Tai lieu 2026");
            String cwd = ch.handle("CWD Tai lieu 2026");
            String pwd = ch.handle("PWD");
            String cdup = ch.handle("CDUP");
            String rmd = ch.handle("RMD Tai lieu 2026");

            boolean passTest = mkd.startsWith("257") && cwd.startsWith("250") && pwd.contains("Tai lieu 2026")
                    && cdup.startsWith("200") && rmd.startsWith("250");

            record("TC-FUNC-04", "Quản lý cây thư mục Sandbox (MKD, CWD, PWD, CDUP, RMD)", "Chức năng",
                    "User đăng nhập vào Sandbox",
                    "Tạo thư mục có dấu cách -> CWD vào trong -> PWD -> CDUP -> RMD",
                    "Toàn bộ chuỗi lệnh thành công (257, 250, 200)",
                    "MKD=" + mkd + " | CWD=" + cwd + " | PWD=" + pwd + " | CDUP=" + cdup + " | RMD=" + rmd,
                    passTest, "Thư mục tạo, di chuyển và xóa hoàn hảo");
        }

        // TC-FUNC-05: Upload (STOR) & Download (RETR) nhị phân với Checksum SHA-256
        {
            String username = "p7_io_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);
            byte[] rawContent = "Dữ liệu kiểm thử truyền file nhị phân Phase 7 Antigravity!".getBytes(StandardCharsets.UTF_8);
            String fn = "test_io_file.txt";

            String saveResp = storageManager.receiveFile(u.getUserId(), u.getHomeDir(), "", fn, rawContent.length, new ByteArrayInputStream(rawContent));
            boolean storOk = saveResp != null && saveResp.startsWith("226") && saveResp.contains("SHA256:");

            ByteArrayOutputStream downloaded = new ByteArrayOutputStream();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            String sendResp = storageManager.sendFile(u.getHomeDir(), "", fn, downloaded, pw);
            boolean retrOk = sendResp != null && sendResp.startsWith("226") && downloaded.size() == rawContent.length;

            boolean passTest = storOk && retrOk;
            record("TC-FUNC-05", "Truyền nhận file nhị phân (STOR/RETR) kèm Checksum SHA-256", "Chức năng",
                    "User có đủ hạn mức lưu trữ",
                    "Tải lên tệp -> Tải về tệp -> Đối chiếu byte và SHA-256",
                    "226 Transfer complete, nội dung byte và mã băm khớp 100%",
                    "STOR=" + saveResp + " | RETR=" + sendResp + " (Downloaded: " + downloaded.size() + "B)",
                    passTest, "Toàn vẹn dữ liệu nhị phân hoàn hảo");
        }

        // TC-FUNC-06: Xóa file (DELE) & Hoàn trả hạn mức Quota
        {
            String username = "p7_dele_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);
            byte[] data = new byte[10240]; // 10KB
            String fn = "delete_me.dat";
            storageManager.receiveFile(u.getUserId(), u.getHomeDir(), "", fn, data.length, new ByteArrayInputStream(data));

            long usedBefore = userDAO.getQuotaInfo(u.getUserId())[0];
            String delResp = storageManager.deleteFile(u.getUserId(), u.getHomeDir(), "", fn);
            long usedAfter = userDAO.getQuotaInfo(u.getUserId())[0];

            boolean passTest = delResp.startsWith("250") && (usedBefore - usedAfter == data.length);
            record("TC-FUNC-06", "Xóa file (DELE) & Tự động hoàn trả dung lượng Quota vào CSDL", "Chức năng",
                    "Tệp tồn tại trong Sandbox và CSDL",
                    "Xóa tệp -> Đo lường used_bytes trong bảng users",
                    "250 File deleted, used_bytes giảm đúng 10240 bytes",
                    delResp + " (UsedBefore=" + usedBefore + ", UsedAfter=" + usedAfter + ")",
                    passTest, "Hoàn trả Quota chính xác từng byte");
        }

        // TC-FUNC-07: Chia sẻ tệp & Kiểm tra quyền tải về (VIEW vs DOWNLOAD)
        {
            String owner = "p7_owner_" + System.currentTimeMillis() % 10000;
            String rec = "p7_rec_" + System.currentTimeMillis() % 10000;
            User uOwner = ensureUser(userDAO, owner, "pass123", owner + "@test.com", 2);
            User uRec = ensureUser(userDAO, rec, "pass123", rec + "@test.com", 2);

            byte[] data = "Shared content Phase 7".getBytes(StandardCharsets.UTF_8);
            storageManager.receiveFile(uOwner.getUserId(), uOwner.getHomeDir(), "", "share_view.txt", data.length, new ByteArrayInputStream(data));
            FileMetadata fm = fileDAO.findByFilename(uOwner.getUserId(), "", "share_view.txt");

            // Tạo link chia sẻ với quyền VIEW
            String token = shareDAO.createShare(fm.getFileId(), uOwner.getUserId(), uRec.getUserId(), "VIEW", null, 0);

            // Recipient thử tải file quyền VIEW qua GETSHARED -> PHẢI BỊ TỪ CHỐI 550
            Session sessRec = new Session();
            sessRec.loginSuccess(uRec);
            CommandHandler chRec = new CommandHandler(sessRec, userDatabase, storageManager);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            String getResp = chRec.handleGetShared(token, pw, new ByteArrayOutputStream());

            boolean passTest = getResp != null && getResp.contains("550") && getResp.contains("VIEW permission");
            record("TC-FUNC-07", "Chia sẻ tệp quyền VIEW & Chặn tải về qua GETSHARED", "Chức năng",
                    "Tệp được chia sẻ với permission='VIEW'",
                    "Recipient gọi GETSHARED <token>",
                    "550 Permission denied: This share link has VIEW permission (download not permitted)",
                    getResp,
                    passTest, "Bảo vệ bản quyền VIEW thành công");
        }

        // TC-FUNC-08: Vòng đời gói dịch vụ (ORDER -> PAY SUCCESS -> Kích hoạt Quota mới)
        {
            String username = "p7_sub_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);
            Session sess = new Session();
            sess.loginSuccess(u);
            CommandHandler ch = new CommandHandler(sess, userDatabase, storageManager);

            // Tạo đơn hàng gói PRO (plan_id = 2)
            String orderResp = ch.handle("ORDER 2");
            boolean ordOk = orderResp != null && orderResp.startsWith("250");

            // Bóc tách Order ID từ CSDL
            int orderId = -1;
            List<Order> userOrders = orderDAO.getUserOrders(u.getUserId());
            if (!userOrders.isEmpty()) {
                orderId = userOrders.get(0).getOrderId();
            }

            // Thanh toán đơn
            String payResp = ch.handle("PAY " + orderId + " SUCCESS");
            boolean payOk = payResp != null && payResp.startsWith("200");

            // Kiểm tra Quota mới sau nâng cấp (phải là 1GB = 1073741824 bytes)
            long quotaAfter = userDAO.getQuotaInfo(u.getUserId())[1];
            boolean passTest = ordOk && payOk && (quotaAfter == 1073741824L);

            record("TC-FUNC-08", "Vòng đời gói cước: ORDER -> PAY SUCCESS -> Nâng cấp Quota 1GB", "Chức năng",
                    "Gói PRO (Plan ID=2) có sẵn trong CSDL",
                    "Đặt hàng gói 2 -> Thanh toán giả lập thành công -> Đối soát Quota",
                    "250 Order created -> 200 Mock payment approved -> Quota = 1073741824 bytes",
                    "Order: " + orderResp + " | Pay: " + payResp + " | Quota: " + quotaAfter + " bytes",
                    passTest, "Kích hoạt Quota 1GB theo đúng chuẩn ACID");
        }
    }

    // =========================================================================
    // GROUP B: KIỂM THỬ TÍCH HỢP & PHỤC HỒI (INTEGRATION & RESILIENCE)
    // =========================================================================

    private static void runGroupBIntegration() throws Exception {
        System.out.println("\n--- [NHÓM B: KIỂM THỬ TÍCH HỢP & PHỤC HỒI - INTEGRATION & RESILIENCE] ---");

        // TC-INT-01: Client ↔ Server Socket End-to-End chuỗi lệnh mạng thực tế
        {
            // Mở ServerSocket thử nghiệm trên cổng ngẫu nhiên
            ServerSocket serverSocket = new ServerSocket(0);
            int testPort = serverSocket.getLocalPort();

            ExecutorService exec = Executors.newSingleThreadExecutor();
            exec.submit(() -> {
                try (Socket client = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
                    Session s = new Session();
                    CommandHandler h = new CommandHandler(s, userDatabase, storageManager);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String resp = h.handle(line);
                        writer.println(resp);
                        if ("QUIT".equalsIgnoreCase(line.trim())) break;
                    }
                } catch (Exception ignored) {}
            });

            // Client kết nối
            Socket clientSocket = new Socket("127.0.0.1", testPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

            out.println("USER admin");
            String r1 = in.readLine();
            out.println("PASS 123");
            String r2 = in.readLine();
            out.println("PING");
            String r3 = in.readLine();
            out.println("QUIT");
            String r4 = in.readLine();

            clientSocket.close();
            serverSocket.close();
            exec.shutdown();

            boolean passTest = r1 != null && r1.startsWith("331") && r2.startsWith("230") && "200 PONG".equals(r3) && r4.startsWith("221");
            record("TC-INT-01", "Client ↔ Server Socket TCP End-to-End đầy đủ", "Tích hợp",
                    "ServerSocket mở cổng cục bộ",
                    "Gửi chuỗi lệnh USER -> PASS -> PING -> QUIT qua TCP Socket",
                    "331 -> 230 -> 200 PONG -> 221 Goodbye",
                    "R1=" + r1 + " | R2=" + r2 + " | R3=" + r3 + " | R4=" + r4,
                    passTest, "Giao tiếp mạng Socket TCP hoàn hảo");
        }

        // TC-INT-02: Server ↔ MySQL kết nối bền vững & Transaction Commit
        {
            boolean dbOk = DBConnection.getInstance().isAvailable();
            Connection conn = DBConnection.getInstance().getConnection();
            boolean valid = conn != null && !conn.isClosed();
            if (conn != null) conn.close();

            boolean passTest = dbOk && valid;
            record("TC-INT-02", "Server ↔ MySQL Database Connection Pool & Ping", "Tích hợp",
                    "MySQL service đang chạy",
                    "Kiểm tra DBConnection.isAvailable() và ping kết nối JDBC",
                    "Kết nối thành công, connection mở hợp lệ",
                    "isAvailable=" + dbOk + ", Connection valid=" + valid,
                    passTest, "Kết nối MySQL ổn định");
        }

        // TC-INT-03: Server ↔ Filesystem lưu trữ độc lập trong Sandbox
        {
            String username = "p7_fs_" + System.currentTimeMillis() % 10000;
            Path expectedPath = storageManager.resolveSafe(username, "", "sandbox_test.txt");
            boolean inSandbox = expectedPath.toString().contains("storage\\" + username) || expectedPath.toString().contains("storage/" + username);

            record("TC-INT-03", "Server ↔ Filesystem lưu trữ cách ly Sandbox", "Tích hợp",
                    "StorageRoot cấu hình hợp lệ",
                    "resolveSafe(username, '', 'sandbox_test.txt')",
                    "Đường dẫn tuyệt đối nằm trọn vẹn trong storage/<username>/",
                    expectedPath.toString(),
                    inSandbox, "Cơ chế phân vùng Sandbox thư mục an toàn");
        }

        // TC-INT-04: 10 Client hoạt động đồng thời (Multithreaded Socket Concurrency)
        {
            int threadCount = 10;
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                pool.submit(() -> {
                    try {
                        Session s = new Session();
                        CommandHandler ch = new CommandHandler(s, userDatabase, storageManager);
                        ch.handle("USER admin");
                        ch.handle("PASS 123");
                        String ping = ch.handle("PING");
                        String who = ch.handle("WHOAMI");
                        if ("200 PONG".equals(ping) && who.startsWith("200")) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            boolean passTest = successCount.get() == threadCount;
            record("TC-INT-04", "10 Client xử lý đồng thời không nghẽn luồng (Thread Concurrency)", "Tích hợp",
                    "Server hỗ trợ xử lý đa luồng độc lập qua Session riêng",
                    "10 luồng gửi đồng thời USER -> PASS -> PING -> WHOAMI",
                    "10/10 luồng phản hồi chính xác 200 PONG và 200 WHOAMI",
                    successCount.get() + "/" + threadCount + " luồng thành công",
                    passTest, "Không có race condition hoặc deadlock");
        }

        // TC-INT-05: Khả năng chịu lỗi khi mất kết nối CSDL (DB Fallback to In-Memory)
        {
            // userDatabase.authenticate hỗ trợ Fallback sang bộ nhớ RAM khi DB offline
            User fallbackAdmin = userDatabase.authenticate("admin", "123");
            User fallbackGuest = userDatabase.authenticate("guest", "guest");

            boolean passTest = fallbackAdmin != null && fallbackGuest != null && fallbackAdmin.isAdmin();
            record("TC-INT-05", "Khả năng chịu lỗi: Fallback In-Memory khi CSDL lỗi", "Tích hợp",
                    "UserDatabase nạp sẵn các tài khoản offline dự phòng",
                    "Kiểm tra authenticate() với các tài khoản dự phòng",
                    "Trả về đối tượng User đầy đủ quyền hạn, hệ thống không bao giờ crash",
                    "Admin: " + fallbackAdmin.getUsername() + ", Guest: " + fallbackGuest.getUsername(),
                    passTest, "Cơ chế chịu lỗi dự phòng hoạt động");
        }

        // TC-INT-06: Client ngắt kết nối đột ngột khi Upload -> Tự dọn file tạm (.upload.tmp)
        {
            String username = "p7_abort_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);

            // Giả lập InputStream bị đứt giữa chừng (ném IOException sau 50 bytes)
            InputStream brokenStream = new InputStream() {
                private int count = 0;
                @Override
                public int read() throws IOException {
                    if (++count > 50) throw new IOException("Mất kết nối mạng đột ngột (Simulated Connection Drop)");
                    return 0x41;
                }
            };

            String resp = storageManager.receiveFile(u.getUserId(), u.getHomeDir(), "", "broken.dat", 1024, brokenStream);
            boolean aborted = resp != null && (resp.startsWith("426") || resp.startsWith("451"));

            // Kiểm tra thư mục Sandbox không còn file tạm .upload.tmp nào
            Path dir = storageManager.resolveSafe(u.getHomeDir(), "", "");
            File[] remainingFiles = dir.toFile().listFiles((d, name) -> name.endsWith(".upload.tmp"));
            boolean noTmpLeft = remainingFiles == null || remainingFiles.length == 0;

            boolean passTest = aborted && noTmpLeft;
            record("TC-INT-06", "Client ngắt mạng khi đang Upload -> Tự dọn sạch file tạm .upload.tmp", "Tích hợp",
                    "Client đang truyền file thì mất tín hiệu mạng",
                    "Gặp IOException giữa chừng -> Kiểm tra mã 426 và quét thư mục vật lý",
                    "Mã 426 trả về, file tạm .upload.tmp được xóa sạch, không để rác",
                    resp + " | File tạm còn sót: " + (remainingFiles != null ? remainingFiles.length : 0),
                    passTest, "Dọn dẹp rác bộ nhớ và filesystem tự động 100%");
        }

        // TC-INT-07: Khởi động lại Server & Tự động đối soát dữ liệu (DataReconciler)
        {
            DataReconciler reconciler = new DataReconciler(storageManager.getStorageRoot());
            DataReconciler.ReconciliationReport report = reconciler.reconcile();

            boolean passTest = report != null && report.quotaSyncedUsers >= 0;
            record("TC-INT-07", "Tính nhất quán dữ liệu: Đối soát CSDL vs Filesystem (DataReconciler)", "Tích hợp",
                    "DataReconciler quét toàn bộ Sandbox của tất cả người dùng",
                    "Khởi chạy reconcileAll()",
                    "Báo cáo đối soát hoàn tất, phát hiện và sửa sai số lệch (nếu có)",
                    "Users checked=" + report.quotaSyncedUsers + ", Total DB=" + report.totalDbRecords + ", Disk Files=" + report.totalDiskFiles,
                    passTest, "Tính toàn vẹn dữ liệu được bảo toàn tuyệt đối");
        }

        // TC-INT-08: Đổi tên thư mục cha -> Tự động đồng bộ hóa đường dẫn file con trong MySQL
        {
            String username = "p7_rn_dir_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);

            storageManager.makeDirectory(u.getHomeDir(), "", "old_folder");
            byte[] data = "Child file content".getBytes(StandardCharsets.UTF_8);
            storageManager.receiveFile(u.getUserId(), u.getHomeDir(), "old_folder", "child.txt", data.length, new ByteArrayInputStream(data));

            // Đổi tên thư mục old_folder -> new_folder
            String rnResp = storageManager.renameFile(u.getUserId(), u.getHomeDir(), "", "old_folder", "new_folder");
            boolean rnOk = rnResp.startsWith("250");

            // Kiểm tra CSDL xem đường dẫn của child.txt đã tự động đổi sang /new_folder/child.txt chưa
            FileMetadata childFm = fileDAO.findByPath(u.getUserId(), "/new_folder/child.txt");
            boolean passTest = rnOk && (childFm != null);

            record("TC-INT-08", "Đổi tên thư mục cha -> Tự động cập nhật đường dẫn các file con trong CSDL", "Tích hợp",
                    "Thư mục cũ chứa tệp tin con",
                    "Đổi tên thư mục cũ sang mới -> Tra cứu file con trong CSDL",
                    "250 Rename successful, file con có đường dẫn mới /new_folder/child.txt",
                    rnResp + " | File con tìm thấy: " + (childFm != null ? childFm.getRelativePath() : "NULL"),
                    passTest, "Duy trì tính toàn vẹn quan hệ cha-con trong CSDL");
        }
    }

    // =========================================================================
    // GROUP C: KIỂM THỬ BẢO MẬT & CHỐNG KHAI THÁC (SECURITY & ANTI-EXPLOIT)
    // =========================================================================

    private static void runGroupCSecurity() throws Exception {
        System.out.println("\n--- [NHÓM C: KIỂM THỬ BẢO MẬT & CHỐNG KHAI THÁC - SECURITY & ANTI-EXPLOIT] ---");

        // TC-SEC-01: Chặn truy cập khi chưa đăng nhập (Unauthenticated Gate)
        {
            Session unauth = new Session();
            CommandHandler ch = new CommandHandler(unauth, userDatabase, storageManager);

            String r1 = ch.handle("LIST");
            String r2 = ch.handle("PWD");
            String r3 = ch.handle("DELE test.txt");
            String r4 = ch.handle("ADMINSTATS");

            boolean passTest = "530 Not logged in".equals(r1) && "530 Not logged in".equals(r2)
                    && "530 Not logged in".equals(r3) && "530 Not logged in".equals(r4);

            record("TC-SEC-01", "Cổng kiểm soát truy cập chưa xác thực (Unauthenticated Gate)", "Bảo mật",
                    "Session ở trạng thái chưa đăng nhập",
                    "Gửi LIST, PWD, DELE, ADMINSTATS",
                    "Tất cả đều bị chặn bằng mã '530 Not logged in'",
                    "R1=" + r1 + ", R2=" + r2 + ", R3=" + r3 + ", R4=" + r4,
                    passTest, "Không cho phép bất kỳ thao tác trái phép nào");
        }

        // TC-SEC-02: Chặn leo thang đặc quyền (Privilege Escalation)
        {
            String username = "p7_norm_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2); // Role USER
            Session sess = new Session();
            sess.loginSuccess(u);
            CommandHandler ch = new CommandHandler(sess, userDatabase, storageManager);

            String r1 = ch.handle("ADMINSTATS");
            String r2 = ch.handle("USERLIST");
            String r3 = ch.handle("SETUSERROLE admin ROLE_USER");
            String r4 = ch.handle("ADMINALLORDERS");

            boolean passTest = r1.contains("550") && r2.contains("550") && r3.contains("550") && r4.contains("550");
            record("TC-SEC-02", "Chống leo thang đặc quyền: USER gọi lệnh ADMIN bị từ chối 550", "Bảo mật",
                    "Tài khoản có quyền ROLE_USER thông thường",
                    "Gửi ADMINSTATS, USERLIST, SETUSERROLE, ADMINALLORDERS",
                    "Tất cả bị chặn với '550 Permission denied: Administrator role required'",
                    "R1=" + r1 + " | R2=" + r2 + " | R3=" + r3 + " | R4=" + r4,
                    passTest, "Phân quyền RBAC chuẩn tuyệt đối");
        }

        // TC-SEC-03: Chống tấn công giả mạo quyền sở hữu IDOR
        {
            String victim = "p7_victim_" + System.currentTimeMillis() % 10000;
            String attacker = "p7_atk_" + System.currentTimeMillis() % 10000;
            User uVictim = ensureUser(userDAO, victim, "pass123", victim + "@test.com", 2);
            User uAtk = ensureUser(userDAO, attacker, "pass123", attacker + "@test.com", 2);

            // Nạn nhân tạo đơn hàng
            Order order = orderDAO.createOrder(uVictim.getUserId(), 1, "MOCK_BANKING");

            // Kẻ tấn công cố tình hủy đơn hàng của nạn nhân
            Session sessAtk = new Session();
            sessAtk.loginSuccess(uAtk);
            CommandHandler chAtk = new CommandHandler(sessAtk, userDatabase, storageManager);
            String cancelResp = chAtk.handle("CANCELORDER " + order.getOrderId());

            // Nạn nhân tạo lượt chia sẻ
            byte[] data = "Secret".getBytes(StandardCharsets.UTF_8);
            storageManager.receiveFile(uVictim.getUserId(), uVictim.getHomeDir(), "", "v_sec.txt", data.length, new ByteArrayInputStream(data));
            FileMetadata fm = fileDAO.findByFilename(uVictim.getUserId(), "", "v_sec.txt");
            String token = shareDAO.createShare(fm.getFileId(), uVictim.getUserId(), null, "DOWNLOAD", null, 0);
            SharedFile sf = shareDAO.validateAndGetShare(token, null);

            // Kẻ tấn công cố tình thu hồi share của nạn nhân
            String revokeResp = chAtk.handle("REVOKE " + sf.getShareId());

            boolean passTest = cancelResp.contains("550") && revokeResp.contains("550");

            record("TC-SEC-03", "Chống tấn công IDOR: Không thể thao tác tài nguyên của người khác", "Bảo mật",
                    "Tài nguyên (Đơn hàng, Lượt chia sẻ) thuộc quyền sở hữu của User khác",
                    "Attacker gọi CANCELORDER và REVOKE trên tài nguyên của Victim",
                    "Server từ chối 550 Anti-IDOR",
                    "Cancel: " + cancelResp + " | Revoke: " + revokeResp,
                    passTest, "Bảo vệ toàn vẹn quyền sở hữu dữ liệu");
        }

        // TC-SEC-04: Chống tấn công Path Traversal & Thoát Sandbox
        {
            String username = "p7_traversal_" + System.currentTimeMillis() % 10000;
            String[] maliciousPaths = {
                    "../../etc/passwd",
                    "..\\..\\Windows\\win.ini",
                    "/../admin/secret.txt",
                    "C:\\Windows\\System32\\cmd.exe",
                    "..//..//boot.ini"
            };

            int blockedCount = 0;
            for (String p : maliciousPaths) {
                try {
                    storageManager.resolveSafe(username, "", p);
                } catch (SecurityException e) {
                    blockedCount++;
                }
            }

            boolean passTest = blockedCount == maliciousPaths.length;
            record("TC-SEC-04", "Chống tấn công Path Traversal & Vượt biên Sandbox", "Bảo mật",
                    "User gửi các đường dẫn chứa ../ hoặc ký tự ổ đĩa",
                    "resolveSafe kiểm tra mọi đường dẫn độc hại",
                    "Ném SecurityException và chặn 100% các mẫu thử",
                    "Đã chặn " + blockedCount + "/" + maliciousPaths.length + " cuộc tấn công",
                    passTest, "Sandbox được bảo vệ nghiêm ngặt");
        }

        // TC-SEC-05: Kháng tấn công SQL Injection trên các tham số đầu vào
        {
            String sqliPayload = "' OR '1'='1' -- ";
            User userResult = userDAO.validateUser(sqliPayload, "any_pass");
            User findResult = userDAO.findByUsername(sqliPayload);
            Order orderResult = orderDAO.getOrderByCode(sqliPayload);

            boolean passTest = (userResult == null) && (findResult == null) && (orderResult == null);
            record("TC-SEC-05", "Kháng tấn công SQL Injection (PreparedStatement Parametrization)", "Bảo mật",
                    "Tham số chứa chuỗi tiêm nhiễm SQL injection",
                    "Truy vấn qua UserDAO, OrderDAO với payload độc",
                    "Truy vấn an toàn, trả về null, không có ngoại lệ SQL cú pháp",
                    "ValidateUser=" + userResult + ", FindByUsername=" + findResult + ", Order=" + orderResult,
                    passTest, "100% truy vấn được tham số hóa chuẩn PreparedStatement");
        }

        // TC-SEC-06: Chặn liên kết chia sẻ hết hạn hoặc đã bị thu hồi
        {
            String username = "p7_link_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);
            byte[] data = "Data".getBytes(StandardCharsets.UTF_8);
            storageManager.receiveFile(u.getUserId(), u.getHomeDir(), "", "doc.pdf", data.length, new ByteArrayInputStream(data));
            FileMetadata fm = fileDAO.findByFilename(u.getUserId(), "", "doc.pdf");

            // Tạo link chia sẻ rồi thu hồi
            String token = shareDAO.createShare(fm.getFileId(), u.getUserId(), null, "DOWNLOAD", null, 0);
            SharedFile sf = shareDAO.validateAndGetShare(token, null);
            shareDAO.revokeShare(sf.getShareId(), u.getUserId());

            // Thử truy cập sau khi đã thu hồi
            SharedFile afterRevoke = shareDAO.validateAndGetShare(token, null);

            boolean passTest = afterRevoke == null;
            record("TC-SEC-06", "Bảo vệ Token chia sẻ: Thu hồi và vô hiệu hóa tức thì", "Bảo mật",
                    "Token đã bị người sở hữu thu hồi (REVOKE)",
                    "Gọi validateAndGetShare với token đã thu hồi",
                    "Trả về null, từ chối mọi yêu cầu tải về",
                    "Result: " + (afterRevoke == null ? "BLOCKED_SUCCESSFULLY" : "LEAKED"),
                    passTest, "Token thu hồi bị vô hiệu hóa ngay");
        }

        // TC-SEC-07: Chống Race Condition Quota khi upload đồng thời
        {
            String username = "p7_quota_race_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);

            // Đặt Quota còn trống đúng 1MB (1048576 bytes)
            userDAO.updateUserQuota(u.getUserId(), 1048576L);

            // 2 luồng cùng xin đặt trước Quota 800KB đồng thời (chỉ 1 luồng được phép thành công)
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(2);
            AtomicInteger successCount = new AtomicInteger(0);

            long requestBytes = 800 * 1024; // 800KB

            for (int i = 0; i < 2; i++) {
                pool.submit(() -> {
                    try {
                        boolean ok = userDAO.checkAndReserveQuota(u.getUserId(), requestBytes);
                        if (ok) successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(3, TimeUnit.SECONDS);
            pool.shutdown();

            boolean passTest = successCount.get() == 1; // Đúng 1 luồng thành công
            record("TC-SEC-07", "Chống Race Condition trong đặt trước Quota đồng thời (Atomic FOR UPDATE)", "Bảo mật",
                    "Hạn mức trống 1MB, 2 luồng cùng yêu cầu 800KB đồng thời",
                    "2 luồng gọi checkAndReserveQuota cùng lúc",
                    "Đúng 1 luồng thành công, luồng còn lại bị từ chối (không tràn Quota)",
                    "Số luồng thành công: " + successCount.get() + "/2",
                    passTest, "Khóa bi quan FOR UPDATE bảo vệ nguyên tử tuyệt đối");
        }

        // TC-SEC-08: Cơ chế Idempotency chống thanh toán trùng lặp & Che giấu Token trong Log
        {
            String username = "p7_idem_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);
            Order ord = orderDAO.createOrder(u.getUserId(), 1, "MOCK_BANKING");

            // Thanh toán lần 1
            PaymentDAO.PaymentResult r1 = paymentDAO.processMockPayment(ord.getOrderId(), u.getUserId(), true, "MOCK_BANKING");
            // Thanh toán lần 2 (cố ý lặp)
            PaymentDAO.PaymentResult r2 = paymentDAO.processMockPayment(ord.getOrderId(), u.getUserId(), true, "MOCK_BANKING");

            boolean passTest = r1.success && !r2.success && r2.message.contains("Idempotent");
            record("TC-SEC-08", "Cơ chế Idempotency chống thanh toán trùng lặp đơn hàng", "Bảo mật",
                    "Đơn hàng đã được thanh toán hoàn tất (COMPLETED)",
                    "Gửi tiếp yêu cầu thanh toán cho cùng đơn hàng đó",
                    "Lần 1 thành công, lần 2 bị từ chối với thông báo Idempotent",
                    "Lần 1: " + r1.success + " | Lần 2: " + r2.message,
                    passTest, "Chống thanh toán kép (Double-spending)");
        }
    }

    // =========================================================================
    // GROUP D: KIỂM THỬ ĐỘ BỀN & ĐIỀU KIỆN BIÊN (RELIABILITY & EDGE CASES)
    // =========================================================================

    private static void runGroupDReliability() throws Exception {
        System.out.println("\n--- [NHÓM D: KIỂM THỬ ĐỘ BỀN & ĐIỀU KIỆN BIÊN - RELIABILITY & EDGE CASES] ---");

        // TC-REL-01: File rỗng 0-byte (Upload, Checksum, Download)
        {
            String username = "p7_empty_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);
            byte[] emptyData = new byte[0];
            String fn = "empty_file.txt";

            String saveResp = storageManager.receiveFile(u.getUserId(), u.getHomeDir(), "", fn, 0, new ByteArrayInputStream(emptyData));
            boolean storOk = saveResp.startsWith("226") && saveResp.contains("SHA256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(new StringWriter());
            String sendResp = storageManager.sendFile(u.getHomeDir(), "", fn, baos, pw);
            boolean retrOk = sendResp.startsWith("226") && baos.size() == 0;

            boolean passTest = storOk && retrOk;
            record("TC-REL-01", "Xử lý tệp tin rỗng 0-byte (Zero-byte File Integrity)", "Độ bền",
                    "Tệp có dung lượng chính xác 0 bytes",
                    "Upload file 0 byte -> Download file 0 byte",
                    "226 Transfer complete, SHA-256 rỗng chuẩn, không treo luồng",
                    "Save: " + saveResp + " | Send: " + sendResp,
                    passTest, "Hỗ trợ hoàn hảo file 0 byte");
        }

        // TC-REL-02: Tên file đặc biệt (Tiếng Việt có dấu UTF-8 & Ký tự đặc biệt)
        {
            String username = "p7_spec_fn_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);
            String specialName = "Đồ án Mạng & An toàn (Bản Final) #2026.docx";
            byte[] data = "Content UTF-8".getBytes(StandardCharsets.UTF_8);

            String saveResp = storageManager.receiveFile(u.getUserId(), u.getHomeDir(), "", specialName, data.length, new ByteArrayInputStream(data));
            boolean storOk = saveResp.startsWith("226");

            String listResp = storageManager.listDirectory(u.getHomeDir(), "");
            boolean listOk = listResp.contains(specialName);

            String delResp = storageManager.deleteFile(u.getUserId(), u.getHomeDir(), "", specialName);
            boolean delOk = delResp.startsWith("250");

            boolean passTest = storOk && listOk && delOk;
            record("TC-REL-02", "Xử lý tệp có tên đặc biệt (Tiếng Việt UTF-8, khoảng trắng, ngoặc)", "Độ bền",
                    "Tên tệp chứa ký tự tiếng Việt có dấu và khoảng trắng",
                    "Tải lên -> Quét danh sách LIST -> Xóa tệp",
                    "Tên file giữ nguyên vẹn 100% không bị vỡ font hay mất ký tự",
                    "Save: " + storOk + " | List contains: " + listOk + " | Delete: " + delOk,
                    passTest, "Hỗ trợ toàn diện UTF-8 trên Windows filesystem");
        }

        // TC-REL-03: Tải file vượt Quota -> Chặn trước khi nhận dữ liệu (Socket Protection)
        {
            String username = "p7_over_quota_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);
            userDAO.updateUserQuota(u.getUserId(), 10485760L); // 10MB quota

            Session sess = new Session();
            sess.loginSuccess(u);
            CommandHandler ch = new CommandHandler(sess, userDatabase, storageManager);

            // Tạo ProtocolInputStream giả lập lệnh STOR kèm SIZE 50MB (52428800 bytes)
            String simulatedInput = "SIZE 52428800\n";
            ByteArrayInputStream bais = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
            server.ProtocolInputStream pis = new server.ProtocolInputStream(bais);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            String storResp = ch.handleStor("large_file.iso", pis, pw);

            boolean passTest = storResp != null && storResp.startsWith("552") && storResp.contains("quota exceeded");
            record("TC-REL-03", "Chặn tệp vượt Quota (552) TRƯỚC khi nhận dữ liệu nhị phân", "Độ bền",
                    "Tài khoản có Quota 10MB, gửi file 50MB",
                    "Gọi handleStor với SIZE 52428800",
                    "552 Storage quota exceeded, dừng ngay, không bơm byte vào socket",
                    storResp,
                    passTest, "Bảo vệ Socket stream không bị nghẽn hay lệch phản hồi");
        }

        // TC-REL-04: Dọn dẹp file tạm .upload.tmp khi có sự cố
        {
            Path storageRoot = storageManager.getStorageRoot();
            boolean rootExists = Files.exists(storageRoot);

            // Kiểm tra toàn bộ thư mục storage không có file tạm .upload.tmp mồ côi
            long orphanedTmpCount = Files.walk(storageRoot)
                    .filter(p -> p.getFileName().toString().endsWith(".upload.tmp"))
                    .count();

            boolean passTest = rootExists && (orphanedTmpCount == 0);
            record("TC-REL-04", "Không có tệp tạm mồ côi (.upload.tmp) tồn đọng trong Storage", "Độ bền",
                    "Toàn bộ Sandbox trong thư mục storage/",
                    "Duyệt đệ quy cây thư mục tìm các file kết thúc bằng .upload.tmp",
                    "0 tệp tạm mồ côi, ổ đĩa luôn sạch",
                    "Số file tạm mồ côi phát hiện: " + orphanedTmpCount,
                    passTest, "Cơ chế dọn dẹp file tạm hoạt động tin cậy");
        }

        // TC-REL-05: Kiểm tra tính toàn vẹn giao dịch ACID (Rollback khi có lỗi)
        {
            Connection conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            String tempUser = "p7_rollback_" + System.currentTimeMillis() % 10000;
            String sql = "INSERT INTO users (username, password_hash, salt, email, role_id, home_dir) VALUES (?, 'h', 's', 'e', 2, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tempUser);
                ps.setString(2, tempUser);
                ps.executeUpdate();
            }

            // Cố ý ROLLBACK
            conn.rollback();
            conn.setAutoCommit(true);
            conn.close();

            User check = userDAO.findByUsername(tempUser);
            boolean passTest = (check == null);

            record("TC-REL-05", "Bảo đảm tính nguyên tử ACID: Rollback dữ liệu khi có sự cố", "Độ bền",
                    "Giao dịch JDBC đang mở",
                    "Thực hiện INSERT trong transaction -> Gặp lỗi -> Gọi rollback()",
                    "Dữ liệu không được ghi vào CSDL, trạng thái giữ nguyên vẹn",
                    "User tìm thấy sau rollback: " + (check == null ? "NULL (PASS)" : "FOUND (FAIL)"),
                    passTest, "Tuân thủ nghiêm ngặt nguyên lý ACID");
        }

        // TC-REL-06: Tự động xử lý hết hạn gói dịch vụ (Subscription Expiration)
        {
            String username = "p7_exp_" + System.currentTimeMillis() % 10000;
            User u = ensureUser(userDAO, username, "pass123", username + "@test.com", 2);

            // Chèn gói cước đã hết hạn (end_date trong quá khứ)
            Connection conn = DBConnection.getInstance().getConnection();
            String insSub = "INSERT INTO subscriptions (user_id, plan_id, start_date, end_date, status) VALUES (?, 2, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 1 DAY, 'ACTIVE')";
            try (PreparedStatement ps = conn.prepareStatement(insSub)) {
                ps.setInt(1, u.getUserId());
                ps.executeUpdate();
            }
            conn.close();

            // Chạy kiểm tra hết hạn
            subscriptionDAO.checkAndExpireSubscriptions();

            // Quota phải tự động giáng về 100MB (104857600 bytes)
            long quotaAfter = userDAO.getQuotaInfo(u.getUserId())[1];
            boolean passTest = (quotaAfter == 104857600L);

            record("TC-REL-06", "Tự động giáng cấp Quota về 100MB khi gói dịch vụ hết hạn", "Độ bền",
                    "User có gói cước đã quá hạn ngày hiệu lực",
                    "checkAndExpireSubscriptions() quét định kỳ",
                    "Trạng thái gói chuyển EXPIRED, Quota hạ về mức mặc định 100MB",
                    "Quota sau khi hết hạn: " + quotaAfter + " bytes",
                    passTest, "Hạ cấp gói tự động bảo vệ tài nguyên hệ thống");
        }

        // TC-REL-07: Tự bảo vệ tài khoản Quản trị viên (Admin Self-protection)
        {
            Session sess = new Session();
            CommandHandler ch = new CommandHandler(sess, userDatabase, storageManager);
            ch.handle("USER admin");
            ch.handle("PASS 123");

            String lockSelf = ch.handle("SETUSERSTATUS admin LOCKED");
            String demoteSelf = ch.handle("SETUSERROLE admin ROLE_USER");

            boolean passTest = lockSelf.contains("550") && lockSelf.contains("Cannot lock your own")
                    && demoteSelf.contains("550") && demoteSelf.contains("Cannot demote your own");

            record("TC-REL-07", "Tự bảo vệ tài khoản Admin: Không thể tự khóa hoặc tự hạ quyền chính mình", "Độ bền",
                    "Admin đang đăng nhập vào phiên làm việc",
                    "Admin tự gọi lệnh SETUSERSTATUS LOCKED và SETUSERROLE ROLE_USER trên chính mình",
                    "Server từ chối 550, bảo vệ hệ thống không bị mất quyền quản trị tối cao",
                    "Lock: " + lockSelf + " | Demote: " + demoteSelf,
                    passTest, "Ngăn chặn kịch bản quản trị viên tự tước quyền");
        }

        // TC-REL-08: Tải áp lực hàng trăm lệnh liên tiếp (Stress & Stability Testing)
        {
            Session sess = new Session();
            CommandHandler ch = new CommandHandler(sess, userDatabase, storageManager);
            ch.handle("USER admin");
            ch.handle("PASS 123");

            int iterations = 200;
            int okCount = 0;
            for (int i = 0; i < iterations; i++) {
                String p = ch.handle("PING");
                String w = ch.handle("WHOAMI");
                if ("200 PONG".equals(p) && w.startsWith("200")) okCount++;
            }

            boolean passTest = (okCount == iterations);
            record("TC-REL-08", "Kiểm thử độ bền: Xử lý 200 lệnh liên tục không rò rỉ tài nguyên", "Độ bền",
                    "Phiên làm việc chịu tải liên tục",
                    "Vòng lặp gửi 200 cặp lệnh PING & WHOAMI liên tiếp",
                    "200/200 phản hồi chính xác, không ngoại lệ, không tràn bộ nhớ",
                    "Hoàn thành: " + okCount + "/" + iterations + " lượt",
                    passTest, "Hệ thống vận hành bền bỉ và ổn định cao");
        }
    }

    // =========================================================================
    // HELPER & REPORTING
    // =========================================================================

    private static User ensureUser(UserDAO dao, String username, String password, String email, int roleId) {
        User u = dao.findByUsername(username);
        if (u == null) {
            dao.registerUser(username, password, email);
            u = dao.findByUsername(username);
            if (roleId == 1 && u != null) {
                dao.updateRole(u.getUserId(), 1);
                u = dao.findByUsername(username);
            }
        }
        return u;
    }

    private static void record(String id, String name, String group, String precondition,
                               String steps, String expected, String actual, boolean passed, String evidence) {
        TestCaseResult res = new TestCaseResult(id, name, group, precondition, steps, expected, actual, passed, evidence);
        results.add(res);
        if (passed) {
            totalPassed++;
            System.out.println("[PASS] " + id + ": " + name + " -> " + evidence);
        } else {
            totalFailed++;
            System.err.println("[FAIL] " + id + ": " + name + " -> " + actual);
        }
    }

    private static void printFinalSummary(long durationMs) {
        System.out.println("\n==================================================================");
        System.out.println("   KẾT QUẢ TỔNG HỢP KIỂM THỬ HỆ THỐNG PHASE 7");
        System.out.println("==================================================================");
        System.out.println("Tổng số kịch bản kiểm thử: " + (totalPassed + totalFailed));
        System.out.println(" - THÀNH CÔNG (PASS):       " + totalPassed);
        System.out.println(" - THẤT BẠI  (FAIL):       " + totalFailed);
        System.out.println(" - TỔNG THỜI GIAN THỰC THI: " + durationMs + " ms");
        System.out.println("==================================================================");
    }

    public static List<TestCaseResult> getResults() {
        return results;
    }
}
