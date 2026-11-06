package client;

import java.io.*;
import java.security.MessageDigest;
import server.util.SecurityUtil;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * PHASE 7 — FTP Client Service Layer
 *
 * Đây là lớp TRUNG GIAN giữa GUI (LoginFrame, MainFrame) và tầng mạng Socket.
 *
 * Nguyên lý thiết kế:
 *   GUI  ──gọi method──►  FtpClientService  ──gửi/nhận bytes──►  Server
 *
 * GUI hoàn toàn không biết gì về Socket, InputStream, PrintWriter.
 * FtpClientService bọc kín toàn bộ giao tiếp FTP vào các method có tên rõ ràng.
 *
 * Mọi thao tác đọc text đều dùng readLine(InputStream) từng byte một
 * để tránh lỗi Buffer-Ahead khi trộn text commands với binary data.
 */
public class FtpClientService {

    // ── Trạng thái kết nối ────────────────────────────────────────────
    private Socket         socket;
    private InputStream    socketIn;
    private PrintWriter    writer;
    private boolean        connected  = false;
    private boolean        loggedIn   = false;
    private String         currentDir = "/";
    private String         loggedUser = "";
    private String         loggedRole = "ROLE_USER";

    // ── Interface callback để báo tiến độ Upload/Download ─────────────
    /** GUI implements interface này để nhận cập nhật tiến độ (%) */
    public interface ProgressListener {
        void onProgress(long transferred, long total);
    }

    /**
     * Dọn sạch các byte hoặc phản hồi cũ còn sót trong socket stream trước khi gửi lệnh mới
     * giúp đồng bộ hóa socket tuyệt đối, chống lỗi lệch dòng phản hồi giữa các lệnh.
     */
    public synchronized void clearPendingInput() {
        try {
            if (socketIn != null) {
                int avail = socketIn.available();
                if (avail > 0) {
                    socketIn.skip(avail);
                }
            }
        } catch (Exception ignored) {}
    }

    // ================================================================
    // Kết nối / Ngắt kết nối
    // ================================================================

    /**
     * Mở kết nối TCP đến FTP Server.
     *
     * @param host Địa chỉ Server (ví dụ: "localhost" hoặc "192.168.1.10")
     * @param port Cổng Server (mặc định: 6789)
     * @return true nếu kết nối thành công
     */
    public synchronized boolean connect(String host, int port) {
        try {
            socket    = new Socket(host, port);
            socketIn  = socket.getInputStream();
            writer    = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
            connected = true;
            return true;
        } catch (IOException e) {
            connected = false;
            return false;
        }
    }

    /**
     * Gửi QUIT và đóng Socket hoàn toàn.
     */
    public synchronized void disconnect() {
        if (!connected) return;
        try {
            if (writer != null) writer.println("QUIT");
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        connected  = false;
        loggedIn   = false;
        currentDir = "/";
        loggedUser = "";
    }

    // ================================================================
    // Xác thực danh tính
    // ================================================================

    /**
     * Thực hiện đăng nhập 2 bước: USER + PASS.
     *
     * @return Response cuối cùng từ Server ("230 Login successful..." hoặc "530...")
     */
    public synchronized String login(String username, String password) throws IOException {
        if (!connected) return "NOT_CONNECTED";
        clearPendingInput();

        // Bước 1: Gửi USER
        writer.println("USER " + username);
        String resp1 = readLine(socketIn);
        if (resp1 == null || !resp1.startsWith("331")) return resp1;

        // Bước 2: Gửi PASS
        writer.println("PASS " + password);
        String resp2 = readLine(socketIn);

        if (resp2 != null && resp2.startsWith("230")) {
            loggedIn   = true;
            loggedUser = username;
            if (resp2.toUpperCase().contains("ROLE_ADMIN")) {
                loggedRole = "ROLE_ADMIN";
            } else {
                loggedRole = "ROLE_USER";
            }
        }
        return resp2;
    }

    /**
     * Đăng xuất: gửi LOGOUT nhưng giữ kết nối Socket mở.
     */
    public synchronized String logout() throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();
        writer.println("LOGOUT");
        String resp = readLine(socketIn);
        loggedIn   = false;
        loggedUser = "";
        loggedRole = "ROLE_USER";
        currentDir = "/";
        return resp;
    }

    /**
     * Đăng ký tài khoản người dùng mới (REGISTER).
     */
    public synchronized String register(String username, String password, String email) throws IOException {
        if (!connected) return "NOT_CONNECTED";
        clearPendingInput();
        String emailArg = (email != null && !email.trim().isEmpty()) ? " " + email.trim() : "";
        writer.println("REGISTER " + username + " " + password + emailArg);
        return readLine(socketIn);
    }

    /**
     * Đổi mật khẩu tài khoản (PASSWD).
     */
    public synchronized String changePassword(String oldPass, String newPass) throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();
        writer.println("PASSWD " + oldPass + " " + newPass);
        return readLine(socketIn);
    }

    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(loggedRole);
    }

    public String getLoggedRole() {
        return loggedRole;
    }

    // ================================================================
    // Lệnh thư mục
    // ================================================================

    /**
     * Gửi PWD — lấy đường dẫn thư mục hiện tại.
     */
    /**
     * Tạo thư mục mới (MKD).
     */
    public synchronized String mkd(String dirName) throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();
        writer.println("MKD " + dirName);
        return readLine(socketIn);
    }

    /**
     * Xóa thư mục rỗng (RMD).
     */
    public synchronized String rmd(String dirName) throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();
        writer.println("RMD " + dirName);
        return readLine(socketIn);
    }

    /**
     * Đổi thư mục làm việc (CWD).
     */
    public synchronized String cwd(String dirName) throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();
        writer.println("CWD " + dirName);
        return readLine(socketIn);
    }

    /**
     * Di chuyển lên thư mục cha (CDUP).
     */
    public synchronized String cdup() throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();
        writer.println("CDUP");
        return readLine(socketIn);
    }

    /**
     * Lấy thông tin user, vai trò và hạn mức dung lượng (WHOAMI).
     */
    public synchronized String whoami() throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();
        writer.println("WHOAMI");
        return readLine(socketIn);
    }

    public synchronized String pwd() throws IOException {
        clearPendingInput();
        writer.println("PWD");
        String resp = readLine(socketIn);
        // Format trả về: 257 "/" is current directory
        if (resp != null && resp.startsWith("257")) {
            int start = resp.indexOf('"');
            int end   = resp.lastIndexOf('"');
            if (start >= 0 && end > start) {
                currentDir = resp.substring(start + 1, end);
            }
        }
        return resp;
    }

    /**
     * Gửi LIST — lấy danh sách file/thư mục trong currentDir.
     * Parse thành List<FileEntry> để GUI hiển thị vào JTable.
     *
     * Format mỗi dòng từ Server:
     *   <DIR>              -    yyyy-MM-dd HH:mm  tenThuMuc
     *        12345 bytes   .    yyyy-MM-dd HH:mm  tenFile.txt
     */
    public synchronized List<FileEntry> list() throws IOException {
        clearPendingInput();
        List<FileEntry> entries = new ArrayList<>();
        writer.println("LIST");

        String firstLine = readLine(socketIn);
        if (firstLine == null || !firstLine.startsWith("150")) return entries;

        String line;
        while ((line = readLine(socketIn)) != null) {
            if (line.startsWith("226")) break; // kết thúc listing
            if (line.trim().isEmpty() || line.contains("(empty")) continue;
            FileEntry entry = FileEntry.parse(line);
            if (entry != null) entries.add(entry);
        }
        return entries;
    }

    // ================================================================
    // Upload (STOR)
    // ================================================================

    /**
     * Upload file lên Server.
     * Giao thức: STOR → 125 Ready → SIZE n → [n bytes] → 226
     *
     * @param localFile        File local cần upload
     * @param progressListener Callback nhận tiến độ (có thể null)
     * @return Response cuối cùng từ Server
     */
    public synchronized String stor(File localFile, ProgressListener progressListener) throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();

        String filename = localFile.getName();
        long   fileSize = localFile.length();

        // Bước 1: Gửi STOR <filename>
        writer.println("STOR " + filename);

        // Bước 2: Đọc "125 Ready..."
        String ready = readLine(socketIn);
        if (ready == null || !ready.startsWith("125")) return ready;

        // Bước 3: Gửi SIZE <bytes>
        writer.println("SIZE " + fileSize);

        // Bước 3b: Đọc phản hồi kiểm tra Quota từ Server (150 hoặc 552 Quota exceeded)
        String sizeResp = readLine(socketIn);
        if (sizeResp == null || !sizeResp.startsWith("150")) {
            return sizeResp != null ? sizeResp : "426 Server rejected transfer or connection closed";
        }

        // Bước 4: Quota hợp lệ -> Bắt đầu bơm raw bytes và tính toán mã băm SHA-256
        OutputStream rawOut = socket.getOutputStream();
        byte[] buffer = new byte[4096];
        long totalSent = 0;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            md = null;
        }

        try (FileInputStream fis = new FileInputStream(localFile)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                rawOut.write(buffer, 0, bytesRead);
                if (md != null) md.update(buffer, 0, bytesRead);
                totalSent += bytesRead;
                if (progressListener != null) {
                    progressListener.onProgress(totalSent, fileSize);
                }
            }
            rawOut.flush();
        }

        // Bước 5: Đọc "226 Transfer complete"
        String resp = readLine(socketIn);
        if (resp != null && resp.startsWith("226") && md != null && resp.contains("SHA256:")) {
            String clientHash = SecurityUtil.bytesToHex(md.digest());
            String srvHash = resp.substring(resp.indexOf("SHA256:") + 7).trim();
            if (!srvHash.equalsIgnoreCase(clientHash)) {
                return "426 Checksum mismatch! Client SHA256: " + clientHash + " != Server: " + srvHash;
            }
        }
        return resp;
    }

    // ================================================================
    // Download (RETR)
    // ================================================================

    /**
     * Download file từ Server về thư mục saveDir trên máy Client.
     * Giao thức: RETR → 150 (SIZE n) → [n bytes] → 226
     *
     * @param filename         Tên file trên Server
     * @param saveDir          Thư mục lưu file tải về (ví dụ: "downloads/")
     * @param progressListener Callback nhận tiến độ (có thể null)
     * @return Response cuối cùng từ Server
     */
    public synchronized String retr(String filename, File saveDir, ProgressListener progressListener) throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();

        // Bước 1: Gửi RETR <filename>
        writer.println("RETR " + filename);

        // Bước 2: Đọc "150 Opening binary... (SIZE <bytes>)"
        String ready = readLine(socketIn);
        if (ready == null || !ready.startsWith("150")) return ready;

        // Bóc tách fileSize từ response
        long fileSize = extractSize(ready);
        if (fileSize < 0) return "426 Cannot parse file size from: " + ready;

        // Xác định file đích lưu trữ
        File outFile;
        if (saveDir.isDirectory() || !saveDir.getName().contains(".")) {
            saveDir.mkdirs();
            outFile = new File(saveDir, filename);
        } else {
            if (saveDir.getParentFile() != null) {
                saveDir.getParentFile().mkdirs();
            }
            outFile = saveDir;
        }

        // Bước 3: Nhận raw bytes
        byte[] buffer = new byte[4096];
        long remaining     = fileSize;
        long totalReceived = 0;

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            while (remaining > 0) {
                int toRead    = (int) Math.min(buffer.length, remaining);
                int bytesRead = socketIn.read(buffer, 0, toRead);

                if (bytesRead == -1) {
                    fos.close();
                    outFile.delete();
                    return "426 Connection closed; transfer aborted";
                }

                fos.write(buffer, 0, bytesRead);
                totalReceived += bytesRead;
                remaining     -= bytesRead;

                if (progressListener != null) {
                    progressListener.onProgress(totalReceived, fileSize);
                }
            }
            fos.flush();
        }

        // Bước 4: Đọc "226 Transfer complete"
        return readLine(socketIn);
    }

    // ================================================================
    // Xóa file (DELE)
    // ================================================================

    /**
     * Gửi DELE <filename> để xóa file trên Server.
     */
    public synchronized String dele(String filename) throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();
        writer.println("DELE " + filename);
        return readLine(socketIn);
    }

    // ================================================================
    // Đổi tên (RNFR + RNTO)
    // ================================================================

    /**
     * Đổi tên file 2 bước: RNFR rồi RNTO.
     *
     * @param oldName Tên file hiện tại
     * @param newName Tên file mới
     * @return Response cuối cùng từ Server ("250 Rename successful" hoặc lỗi)
     */
    public synchronized String rename(String oldName, String newName) throws IOException {
        if (!loggedIn) return "530 Not logged in";
        clearPendingInput();

        // Bước 1: RNFR
        writer.println("RNFR " + oldName);
        String resp1 = readLine(socketIn);
        if (resp1 == null || !resp1.startsWith("350")) return resp1;

        // Bước 2: RNTO
        writer.println("RNTO " + newName);
        return readLine(socketIn);
    }

    // ================================================================
    // Getters
    // ================================================================

    public boolean isConnected()  { return connected;  }
    public boolean isLoggedIn()   { return loggedIn;   }
    public String  getCurrentDir(){ return currentDir; }
    public String  getLoggedUser(){ return loggedUser; }

    // ================================================================
    // Utility: Đọc text từng byte — KHÔNG buffer-ahead
    // ================================================================

    /**
     * Đọc 1 dòng text từ InputStream đến ký tự '\n'.
     * Bỏ qua '\r'. Không dùng BufferedReader để tránh lỗi buffer-ahead
     * khi trộn text command với binary data.
     */
    public static String readLine(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
            if (b == '\r') continue;
            baos.write(b);
        }
        return baos.size() > 0 ? baos.toString(java.nio.charset.StandardCharsets.UTF_8) : null;
    }

    /**
     * Bóc tách fileSize từ chuỗi "... (SIZE <bytes>)".
     * Trả về -1 nếu không tìm thấy.
     */
    private long extractSize(String response) {
        int idx = response.lastIndexOf("(SIZE ");
        if (idx == -1) return -1;
        int end = response.indexOf(")", idx);
        if (end == -1) return -1;
        try {
            return Long.parseLong(response.substring(idx + 6, end).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ================================================================
    // FileEntry — Đại diện 1 dòng trong kết quả LIST
    // ================================================================

    /**
     * Lưu thông tin 1 item (file hoặc thư mục) từ lệnh LIST.
     * GUI sẽ dùng class này để đổ dữ liệu vào JTable.
     */
    public static class FileEntry {
        public final boolean isDirectory;
        public final String  name;
        public final long    size;   // -1 nếu là thư mục
        public final String  date;

        public FileEntry(boolean isDirectory, String name, long size, String date) {
            this.isDirectory = isDirectory;
            this.name        = name;
            this.size        = size;
            this.date        = date;
        }

        /**
         * Parse 1 dòng từ LIST response:
         *   <DIR>              -    2026-09-20 14:00  folder
         *        12345 bytes   .    2026-09-20 13:30  file.txt
         */
        public static FileEntry parse(String line) {
            try {
                if (line == null) return null;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("150") || line.startsWith("226") || line.contains("(empty")) {
                    return null;
                }
                boolean isDir = line.startsWith("<DIR>");

                // Format chuẩn từ Server: String.format("  %s  %s bytes  %s  %s%n", type, size, date, f.getName())
                // Trích xuất timestamp yyyy-MM-dd HH:mm(:ss) và tên chính xác kể cả có khoảng trắng/dấu tiếng Việt
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}(?::\\d{2})?)\\s+(.*)$").matcher(line);
                String date = "";
                String name = "";
                if (m.find()) {
                    date = m.group(1).trim();
                    name = m.group(2).trim();
                } else {
                    String[] parts = line.split("\\s{2,}");
                    if (parts.length < 2) return null;
                    name = parts[parts.length - 1].trim();
                }

                long size = -1;
                if (!isDir) {
                    java.util.regex.Matcher sm = java.util.regex.Pattern.compile("(\\d+)\\s+bytes").matcher(line);
                    if (sm.find()) {
                        try { size = Long.parseLong(sm.group(1)); } catch (NumberFormatException ignored) {}
                    }
                }

                return new FileEntry(isDir, name, size, date);
            } catch (Exception e) {
                return null;
            }
        }

        public String getIcon()     { return isDirectory ? "📁" : "📄"; }
        public String getType()     { return isDirectory ? "Thư mục"  : "File";   }
        public String getSizeText() { return isDirectory ? "-" : size + " bytes"; }
    }

    // ================================================================
    // PHASE 4: CHIA SẺ, PHÂN QUYỀN VÀ LỊCH SỬ KIỂM TOÁN
    // ================================================================

    public synchronized String share(String filename, String recipient, String permission, int expiresHours, int maxDownloads) throws IOException {
        clearPendingInput();
        String target = (recipient == null || recipient.trim().isEmpty()) ? "PUBLIC" : recipient.trim();
        String safeName = (filename != null && filename.contains(" ")) ? ("\"" + filename.trim() + "\"") : filename;
        String cmd = String.format("SHARE %s %s %s %d %d", safeName, target, permission, expiresHours, maxDownloads);
        writer.println(cmd);
        return readLine(socketIn);
    }

    public synchronized String revoke(int shareId) throws IOException {
        clearPendingInput();
        writer.println("REVOKE " + shareId);
        return readLine(socketIn);
    }

    public synchronized List<String> getSharedWithMe() throws IOException {
        clearPendingInput();
        writer.println("SHAREDLIST");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("530")) break;
        }
        return lines;
    }

    public synchronized List<String> getMyShares() throws IOException {
        clearPendingInput();
        writer.println("MYSHARES");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("530")) break;
        }
        return lines;
    }

    public synchronized List<String> getAuditLogs(String filter, int page) throws IOException {
        clearPendingInput();
        writer.println("AUDITLOGS " + filter + " " + page);
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("530")) break;
        }
        return lines;
    }

    public synchronized boolean downloadShared(String token, File localDest, ProgressListener listener) throws Exception {
        clearPendingInput();
        writer.println("GETSHARED " + token);
        String resp150 = readLine(socketIn);
        if (resp150 == null || !resp150.startsWith("150")) {
            throw new IOException("Server từ chối GETSHARED: " + resp150);
        }

        long fileSize = extractSize(resp150);
        File outFile = localDest;
        if (localDest.isDirectory() || !localDest.getName().contains(".")) {
            localDest.mkdirs();
            outFile = new File(localDest, "shared_file");
        } else if (localDest.getParentFile() != null) {
            localDest.getParentFile().mkdirs();
        }

        byte[] buf = new byte[4096];
        long received = 0;
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            while (fileSize < 0 || received < fileSize) {
                int toRead = (fileSize > 0) ? (int) Math.min(buf.length, fileSize - received) : buf.length;
                int n = socketIn.read(buf, 0, toRead);
                if (n == -1) break;
                fos.write(buf, 0, n);
                received += n;
                if (listener != null) listener.onProgress(received, fileSize);
                if (fileSize >= 0 && received >= fileSize) break;
            }
            fos.flush();
        }

        String resp226 = readLine(socketIn);
        return resp226 != null && resp226.startsWith("226");
    }


    // ================================================================
    // PHASE 5: GÓI CƯỚC, ĐƠN HÀNG VÀ THANH TOÁN MÔ PHỎNG (MOCK PAYMENT)
    // ================================================================

    public synchronized List<String> getPlans() throws IOException {
        clearPendingInput();
        writer.println("PLANS");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("500")) break;
        }
        return lines;
    }

    public synchronized String getMyPlan() throws IOException {
        clearPendingInput();
        writer.println("MYPLAN");
        return readLine(socketIn);
    }

    public synchronized String createOrder(int planId) throws IOException {
        clearPendingInput();
        writer.println("ORDER " + planId);
        return readLine(socketIn);
    }

    public synchronized String cancelOrder(String orderIdOrCode) throws IOException {
        clearPendingInput();
        writer.println("CANCELORDER " + orderIdOrCode);
        return readLine(socketIn);
    }

    public synchronized String payOrder(String orderIdOrCode, boolean simulateSuccess) throws IOException {
        clearPendingInput();
        writer.println("PAY " + orderIdOrCode + (simulateSuccess ? " SUCCESS" : " FAIL"));
        return readLine(socketIn);
    }

    public synchronized List<String> getMyOrders() throws IOException {
        clearPendingInput();
        writer.println("MYORDERS");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("500")) break;
        }
        return lines;
    }

    // ================================================================
    // PHASE 6: ADMIN DASHBOARD COMMANDS
    // ================================================================

    public synchronized List<String> getUserList() throws IOException {
        clearPendingInput();
        writer.println("USERLIST");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("500") || line.startsWith("530")) break;
        }
        return lines;
    }

    public synchronized String setUserStatus(String targetUser, boolean active) throws IOException {
        clearPendingInput();
        writer.println("SETUSERSTATUS " + targetUser + " " + (active ? "ACTIVE" : "LOCKED"));
        return readLine(socketIn);
    }

    public synchronized String setUserRole(String targetUser, String role) throws IOException {
        clearPendingInput();
        writer.println("SETUSERROLE " + targetUser + " " + role);
        return readLine(socketIn);
    }

    public synchronized String setUserQuota(String targetUser, long quotaMb) throws IOException {
        clearPendingInput();
        writer.println("SETUSERQUOTA " + targetUser + " " + quotaMb);
        return readLine(socketIn);
    }

    public synchronized String getAdminStats() throws IOException {
        clearPendingInput();
        writer.println("ADMINSTATS");
        return readLine(socketIn);
    }

    public synchronized List<String> getAdminAllOrders() throws IOException {
        clearPendingInput();
        writer.println("ADMINALLORDERS");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("500") || line.startsWith("530")) break;
        }
        return lines;
    }

}
