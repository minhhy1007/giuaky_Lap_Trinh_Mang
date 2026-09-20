package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * PHASE 5 - Command Handler với FTP Commands + File Operations
 *
 * Thay đổi so với Phase 4:
 *   - Inject StorageManager → xử lý LIST, STOR
 *   - Thêm routing cho LIST, STOR, PWD
 *   - Thêm stub (placeholder) cho RETR, DELE, RNFR, RNTO
 *   - STOR cần stream riêng → method handleStor() tách biệt
 *
 * Protocol đầy đủ Phase 5:
 *   Command          Auth?    Response
 *   ──────────────────────────────────────────────────────────────
 *   USER <name>      Không    331 Username accepted
 *   PASS <pw>        Không    230 Login OK | 530 Login incorrect
 *   QUIT             Không    221 Goodbye
 *   LOGOUT           Có       230 Logged out
 *   PING             Có       200 PONG
 *   ECHO <text>      Có       200 <text>
 *   PWD              Có       257 "/<dir>" is current directory
 *   LIST             Có       150 ... (listing) ... 226 Complete
 *   STOR <file>      Có       [handled via handleStor()]
 *   RETR <file>      Có       550 Not implemented yet
 *   DELE <file>      Có       550 Not implemented yet
 *   RNFR <file>      Có       550 Not implemented yet
 *   RNTO <file>      Có       550 Not implemented yet
 */
public class CommandHandler {

    private final Session         session;
    private final UserDatabase    userDatabase;
    private final StorageManager  storageManager;  // PHASE 5: quản lý file

    /**
     * Constructor Phase 5: thêm StorageManager.
     */
    public CommandHandler(Session session, UserDatabase userDatabase,
                          StorageManager storageManager) {
        this.session        = session;
        this.userDatabase   = userDatabase;
        this.storageManager = storageManager;
    }

    // ================================================================
    // handle() — router chính: parse + phân nhánh
    // ================================================================

    /**
     * Xử lý một dòng command thông thường (text-only, không cần stream).
     * STOR không đi qua đây — được xử lý riêng bởi handleStor().
     *
     * @param rawInput  Dòng text từ Client
     * @return          Response gửi về Client
     */
    public String handle(String rawInput) {

        if (rawInput == null || rawInput.trim().isEmpty()) {
            return "500 Empty command";
        }
        String input = rawInput.trim();

        // Tách command và argument
        // "LIST"         → ["LIST"]
        // "ECHO Hello"   → ["ECHO", "Hello"]
        String[] parts   = input.split("\\s+", 2);
        String command   = parts[0].toUpperCase();
        String argument  = (parts.length > 1) ? parts[1].trim() : "";

        // ── Commands KHÔNG cần authentication ─────────────────────────
        switch (command) {
            case "USER": return handleUser(argument);
            case "PASS": return handlePass(argument);
            case "QUIT": return handleQuit();
        }

        // ── Tất cả commands còn lại → CẦN authentication ─────────────
        if (!session.isAuthenticated()) {
            return "530 Not logged in";
        }

        // ── Commands cần authentication ────────────────────────────────
        switch (command) {

            // ── Utility commands (từ Phase 2) ──────────────────────────
            case "LOGOUT":
                return handleLogout();

            case "PING":
                return "200 PONG";

            case "ECHO":
                if (argument.isEmpty()) return "500 ECHO requires a message";
                return "200 " + argument;

            // ── Directory commands (Phase 5) ───────────────────────────

            case "PWD":
                // Print Working Directory — hiển thị thư mục hiện tại
                return "257 \"" + storageManager.formatDir(session.getCurrentDirectory())
                        + "\" is current directory";

            case "LIST":
                // Liệt kê nội dung thư mục hiện tại
                // Trả về multi-line string: 150 ... \n entries \n 226
                return storageManager.listDirectory(session.getCurrentDirectory());

            // ── File Upload (Phase 5) ──────────────────────────────────

            case "STOR":
                // STOR cần stream → KHÔNG xử lý ở đây
                // ClientHandler phát hiện STOR và gọi handleStor() riêng
                return "500 Internal error: STOR should use handleStor()";

            // ── Stubs — chưa implement, sẽ làm Phase 6 ────────────────

            case "RETR":
                // RETR cần stream → KHÔNG xử lý ở đây
                // ClientHandler phát hiện RETR và gọi handleRetr() riêng
                return "500 Internal error: RETR should use handleRetr()";

            case "DELE":
                // Xóa file — implement Phase 7
                if (argument.isEmpty()) return "501 Syntax error: DELE requires a filename";
                return storageManager.deleteFile(session.getCurrentDirectory(), argument);

            case "RNFR":
                // Rename From — ghi nhớ file nguồn vào Session
                if (argument.isEmpty()) return "501 Syntax error: RNFR requires a filename";
                return handleRnfr(argument);

            case "RNTO":
                // Rename To — thực hiện đổi tên với file đã RNFR
                if (argument.isEmpty()) return "501 Syntax error: RNTO requires a filename";
                return handleRnto(argument);

            // ── Unknown ────────────────────────────────────────────────
            default:
                return "500 Unknown command: " + parts[0];
        }
    }

    // ================================================================
    // handleStor() — STOR cần direct stream access
    // Được gọi BỞI ClientHandler, không qua handle()
    // ================================================================

    /**
     * Xử lý STOR <filename> — upload file từ Client về Server.
     *
     * Giao thức upload (trên cùng một TCP connection):
     *   Server → Client: "125 Ready, send: SIZE <bytes>"
     *   Client → Server: "SIZE 12345" (text line)
     *   Client → Server: [12345 raw bytes của file]
     *   Server → Client: "226 Transfer complete" (returned, ClientHandler gửi)
     *
     * @param filename      Tên file (từ argument của STOR command)
     * @param lineReader    ProtocolInputStream để đọc dòng "SIZE <n>"
     * @param writer        PrintWriter để gửi response trung gian (125)
     * @return              Final response (226 hoặc 5xx error)
     */
    public String handleStor(String filename, ProtocolInputStream lineReader,
                             PrintWriter writer) {
        // ── Kiểm tra authentication ────────────────────────────────────
        if (!session.isAuthenticated()) {
            return "530 Not logged in";
        }

        // ── Kiểm tra filename ──────────────────────────────────────────
        if (filename == null || filename.isEmpty()) {
            return "501 Syntax error: STOR requires a filename. Usage: STOR <filename>";
        }

        // ── Gửi 125: báo Client ta sẵn sàng, yêu cầu gửi SIZE ─────────
        // 125 = "Data connection already open; transfer starting"
        // Ta dùng nghĩa: "Ready, send file size then data"
        writer.println("125 Ready for '" + filename + "'. Send: SIZE <bytes>");
        // writer auto-flush → Client nhận được ngay

        // ── Đọc dòng SIZE <n> từ Client ──────────────────────────────
        String sizeLine;
        try {
            sizeLine = lineReader.readLine();
        } catch (IOException e) {
            return "426 Connection closed; transfer aborted";
        }

        if (sizeLine == null) {
            return "426 Connection closed; transfer aborted";
        }

        // Kiểm tra đúng format "SIZE <số>"
        if (!sizeLine.toUpperCase().startsWith("SIZE ")) {
            return "501 Expected 'SIZE <bytes>', got: " + sizeLine;
        }

        long fileSize;
        try {
            fileSize = Long.parseLong(sizeLine.substring(5).trim());
        } catch (NumberFormatException e) {
            return "501 Invalid file size: " + sizeLine;
        }

        if (fileSize < 0) {
            return "501 File size cannot be negative";
        }

        // ── Nhận raw bytes và ghi file qua StorageManager ─────────────
        // InputStream này KHÔNG có buffer → bytes của file còn nguyên
        InputStream rawIn = lineReader.getRawInputStream();

        return storageManager.receiveFile(
                session.getCurrentDirectory(), filename, fileSize, rawIn);
    }

    // ================================================================
    // handleRetr() — RETR cần direct stream access
    // Được gọi BỞI ClientHandler, không qua handle()
    // ================================================================

    /**
     * Xử lý RETR <filename> — download file từ Server về Client.
     *
     * Giao thức download:
     *   Server → Client: "150 Opening binary mode data connection for '<file>' (SIZE <bytes>)"
     *   Server → Client: [<bytes> raw bytes của file qua rawOut]
     *   Server → Client: "226 Transfer complete..." (returned, ClientHandler gửi)
     *
     * @param filename  Tên file cần download
     * @param writer    PrintWriter để gửi response 150
     * @param rawOut    OutputStream để bơm binary data
     * @return          Response cuối cùng (226 hoặc 5xx/4xx)
     */
    public String handleRetr(String filename, PrintWriter writer, OutputStream rawOut) {
        if (!session.isAuthenticated()) {
            return "530 Not logged in";
        }
        if (filename == null || filename.isEmpty()) {
            return "501 Syntax error: RETR requires a filename. Usage: RETR <filename>";
        }
        return storageManager.sendFile(
                session.getCurrentDirectory(), filename, rawOut, writer);
    }

    // ================================================================
    // RNFR / RNTO handlers (Phase 7)
    // ================================================================

    private String handleRnfr(String filename) {
        // Kiểm tra file nguồn tồn tại (dùng StorageManager)
        // Lưu tên vào session để RNTO dùng sau
        session.setRenameFrom(filename);
        return "350 File exists, ready for destination name (RNTO)";
    }

    private String handleRnto(String newName) {
        if (!session.hasRenameFrom()) {
            return "503 Bad sequence of commands — send RNFR first";
        }
        String oldName = session.getRenameFrom();
        session.clearRenameFrom();
        return storageManager.renameFile(session.getCurrentDirectory(), oldName, newName);
    }

    // ================================================================
    // Auth handlers (từ Phase 4, không đổi)
    // ================================================================

    private String handleUser(String username) {
        if (username.isEmpty()) return "501 Syntax error: USER requires a username";
        session.setPendingUsername(username);
        return "331 Username accepted, enter password";
    }

    private String handlePass(String password) {
        if (session.getState() != Session.State.WAITING_PASSWORD) {
            return "503 Bad sequence — send USER first";
        }
        if (password.isEmpty()) return "501 Syntax error: PASS requires a password";

        String username = session.getPendingUsername();
        if (userDatabase.validate(username, password)) {
            session.loginSuccess();
            return "230 Login successful. Welcome, " + username + "!";
        } else {
            session.reset();
            return "530 Login incorrect";
        }
    }

    private String handleLogout() {
        if (!session.isAuthenticated()) return "530 Not logged in";
        String who = session.getLoggedInUser();
        session.reset();
        return "230 Goodbye, " + who + ". Logged out (connection stays open)";
    }

    private String handleQuit() {
        return "221 Goodbye";
    }

    /**
     * Kiểm tra response có phải tín hiệu đóng kết nối không.
     * Chỉ "221 Goodbye" (từ QUIT) mới đóng connection.
     */
    public boolean isQuitResponse(String response) {
        return response != null && response.startsWith("221 Goodbye");
    }
}
