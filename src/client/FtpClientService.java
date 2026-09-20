package client;

import java.io.*;
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

    // ── Interface callback để báo tiến độ Upload/Download ─────────────
    /** GUI implements interface này để nhận cập nhật tiến độ (%) */
    public interface ProgressListener {
        void onProgress(long transferred, long total);
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
    public boolean connect(String host, int port) {
        try {
            socket    = new Socket(host, port);
            socketIn  = socket.getInputStream();
            writer    = new PrintWriter(socket.getOutputStream(), true);
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
    public void disconnect() {
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
    public String login(String username, String password) throws IOException {
        if (!connected) return "NOT_CONNECTED";

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
        }
        return resp2;
    }

    /**
     * Đăng xuất: gửi LOGOUT nhưng giữ kết nối Socket mở.
     */
    public String logout() throws IOException {
        if (!loggedIn) return "530 Not logged in";
        writer.println("LOGOUT");
        String resp = readLine(socketIn);
        loggedIn   = false;
        loggedUser = "";
        currentDir = "/";
        return resp;
    }

    // ================================================================
    // Lệnh thư mục
    // ================================================================

    /**
     * Gửi PWD — lấy đường dẫn thư mục hiện tại.
     */
    public String pwd() throws IOException {
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
    public List<FileEntry> list() throws IOException {
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
    public String stor(File localFile, ProgressListener progressListener) throws IOException {
        if (!loggedIn) return "530 Not logged in";

        String filename = localFile.getName();
        long   fileSize = localFile.length();

        // Bước 1: Gửi STOR <filename>
        writer.println("STOR " + filename);

        // Bước 2: Đọc "125 Ready..."
        String ready = readLine(socketIn);
        if (ready == null || !ready.startsWith("125")) return ready;

        // Bước 3: Gửi SIZE <bytes>
        writer.println("SIZE " + fileSize);

        // Bước 4: Gửi raw bytes
        OutputStream rawOut = socket.getOutputStream();
        byte[] buffer = new byte[4096];
        long totalSent = 0;

        try (FileInputStream fis = new FileInputStream(localFile)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                rawOut.write(buffer, 0, bytesRead);
                totalSent += bytesRead;
                if (progressListener != null) {
                    progressListener.onProgress(totalSent, fileSize);
                }
            }
            rawOut.flush();
        }

        // Bước 5: Đọc "226 Transfer complete"
        return readLine(socketIn);
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
    public String retr(String filename, File saveDir, ProgressListener progressListener) throws IOException {
        if (!loggedIn) return "530 Not logged in";

        // Bước 1: Gửi RETR <filename>
        writer.println("RETR " + filename);

        // Bước 2: Đọc "150 Opening binary... (SIZE <bytes>)"
        String ready = readLine(socketIn);
        if (ready == null || !ready.startsWith("150")) return ready;

        // Bóc tách fileSize từ response
        long fileSize = extractSize(ready);
        if (fileSize < 0) return "426 Cannot parse file size from: " + ready;

        // Tạo thư mục nếu chưa có
        saveDir.mkdirs();
        File outFile = new File(saveDir, filename);

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
    public String dele(String filename) throws IOException {
        if (!loggedIn) return "530 Not logged in";
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
    public String rename(String oldName, String newName) throws IOException {
        if (!loggedIn) return "530 Not logged in";

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
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') return sb.toString();
            if (b == '\r') continue;
            sb.append((char) b);
        }
        return sb.length() > 0 ? sb.toString() : null;
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
                line = line.trim();
                boolean isDir = line.startsWith("<DIR>");

                // Tìm tên file: phần cuối sau khoảng trắng nhiều lần
                // Format: <type>  <size>  <date time>  <name>
                // Tách bằng khoảng trắng nhiều — lấy 2 token đầu cho date+time, cuối cho name
                String[] parts = line.trim().split("\\s{2,}");

                if (parts.length < 2) return null;

                String name = parts[parts.length - 1].trim();
                long   size = -1;
                String date = "";

                if (!isDir) {
                    // Lấy size từ phần có "bytes"
                    for (String p : parts) {
                        if (p.contains("bytes")) {
                            String numStr = p.replace("bytes", "").trim();
                            try { size = Long.parseLong(numStr); } catch (NumberFormatException ignored) {}
                        }
                        if (p.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                            date = p.trim();
                        }
                    }
                } else {
                    for (String p : parts) {
                        if (p.matches("\\d{4}-\\d{2}-\\d{2}.*")) date = p.trim();
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
}
