package client;

import java.io.*;
import java.net.Socket;

/**
 * PHASE 6 - TCP Client với File Download (RETR) & Upload (STOR)
 *
 * Điểm cải tiến cốt lõi Phase 6:
 *   - Thay thế hoàn toàn BufferedReader trên Socket bằng readLine(InputStream in)
 *     đọc từng byte một. Điều này loại bỏ 100% lỗi "Buffer-Ahead" khi nhận dữ liệu
 *     nhị phân của file tải về (RETR).
 *   - Bổ sung lệnh RETR <filename> để tải file từ Server về thư mục local `downloads/`.
 *   - Hiển thị thanh tiến độ tải (Progress Bar) theo từng chunk Buffer 4096 bytes.
 */
public class TCPClient {

    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 6789;

    public static void main(String[] args) {

        System.out.println("=== FTP Project - Phase 6: FTP Client ===");
        System.out.println("[Client] Kết nối đến " + SERVER_HOST + ":" + SERVER_PORT + "...");

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {

            System.out.println("[Client] ✓ Kết nối thành công!");
            System.out.println();

            // InputStream của Socket (dùng cho cả đọc text và đọc binary)
            InputStream socketIn = socket.getInputStream();

            // Ghi command đến Server (text lines, auto-flush)
            PrintWriter serverWriter = new PrintWriter(
                    socket.getOutputStream(), true);

            // Đọc input từ bàn phím
            BufferedReader keyboard = new BufferedReader(
                    new InputStreamReader(System.in));

            printHelp();

            while (true) {
                System.out.print("[ftp]> ");
                String userInput = keyboard.readLine();

                if (userInput == null) break;
                userInput = userInput.trim();
                if (userInput.isEmpty()) continue;

                // ── Phát hiện STOR → xử lý upload đặc biệt ───────────────
                if (userInput.toUpperCase().startsWith("STOR ")) {
                    String[] parts = userInput.split("\\s+", 2);
                    String localFilePath = parts.length > 1 ? parts[1].trim() : "";
                    handleUpload(localFilePath, serverWriter, socketIn, socket);
                    continue;
                }

                // ── Phát hiện RETR → xử lý download đặc biệt (PHASE 6) ───
                if (userInput.toUpperCase().startsWith("RETR ")) {
                    String[] parts = userInput.split("\\s+", 2);
                    String remoteFilename = parts.length > 1 ? parts[1].trim() : "";
                    handleDownload(remoteFilename, serverWriter, socketIn, socket);
                    continue;
                }

                // ── Command thông thường (USER, PASS, PWD, LIST, QUIT...) ─
                serverWriter.println(userInput);
                System.out.println("[Client] → Gửi: \"" + userInput + "\"");

                // Đọc response — có thể là multi-line (như LIST)
                String response = readResponse(socketIn);
                System.out.println("[Client] ← Nhận:\n" + response);
                System.out.println();

                // Kiểm tra QUIT
                if (response != null && response.startsWith("221 Goodbye")) {
                    break;
                }
            }

            System.out.println("[Client] Kết thúc.");

        } catch (java.net.ConnectException e) {
            System.err.println("[Client] LỖI: Server chưa chạy trên port " + SERVER_PORT);
        } catch (IOException e) {
            System.err.println("[Client] LỖI I/O: " + e.getMessage());
        }
    }

    // ================================================================
    // Download file (RETR) — MỚI TRONG PHASE 6
    // ================================================================

    /**
     * Thực hiện download file từ Server về thư mục downloads/ local.
     *
     * Giao thức:
     *   1. Gửi: RETR <filename>
     *   2. Server trả: 150 Opening binary mode... (SIZE <bytes>) hoặc lỗi (550)
     *   3. Client trích xuất file size từ mã 150
     *   4. Client đọc đúng <fileSize> raw bytes từ socketIn ghi vào FileOutputStream
     *   5. Server gửi dòng: 226 Transfer complete...
     *   6. Client đọc mã 226 và hoàn tất
     */
    private static void handleDownload(String remoteFilename,
                                       PrintWriter serverWriter,
                                       InputStream socketIn,
                                       Socket socket) throws IOException {
        if (remoteFilename.isEmpty()) {
            System.out.println("[Client] LỖI: Cần chỉ định tên file. Cú pháp: RETR <filename>");
            return;
        }

        // Tạo thư mục downloads/ nếu chưa tồn tại
        File downloadDir = new File("downloads");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        File localFile = new File(downloadDir, remoteFilename);

        // ── Bước 1: Gửi RETR <filename> ──────────────────────────────
        serverWriter.println("RETR " + remoteFilename);
        System.out.println("[Client] → RETR " + remoteFilename);

        // ── Bước 2: Đọc response chuẩn bị từ Server (150 hoặc 550) ────
        String ready = readLine(socketIn);
        System.out.println("[Client] ← " + ready);

        if (ready == null || !ready.startsWith("150")) {
            System.out.println("[Client] Download bị từ chối hoặc lỗi: " + ready);
            System.out.println();
            return;
        }

        // Bóc tách fileSize từ chuỗi "... (SIZE <bytes>)"
        long fileSize = -1;
        int sizeIdx = ready.lastIndexOf("(SIZE ");
        if (sizeIdx != -1) {
            int endIdx = ready.indexOf(")", sizeIdx);
            if (endIdx != -1) {
                try {
                    fileSize = Long.parseLong(ready.substring(sizeIdx + 6, endIdx).trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (fileSize < 0) {
            System.out.println("[Client] LỖI: Không bóc tách được kích thước file từ Server!");
            return;
        }

        System.out.println("[Client] Đang tải: " + remoteFilename + " (" + fileSize + " bytes)");

        // ── Bước 3: Nhận raw bytes qua Buffer 4096 ────────────────────
        byte[] buffer = new byte[4096];
        long remaining = fileSize;
        long totalReceived = 0;

        try (FileOutputStream fos = new FileOutputStream(localFile)) {
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int bytesRead = socketIn.read(buffer, 0, toRead);

                if (bytesRead == -1) {
                    fos.close();
                    localFile.delete(); // xóa file dở dang
                    System.out.println("\n[Client] LỖI: Mất kết nối giữa chừng. Đã xóa file tải dở dang.");
                    return;
                }

                fos.write(buffer, 0, bytesRead);
                totalReceived += bytesRead;
                remaining -= bytesRead;

                // Tiến độ tải
                long percent = fileSize > 0 ? (totalReceived * 100 / fileSize) : 100;
                System.out.print("\r[Client] Tiến độ tải: " + totalReceived + "/" + fileSize
                        + " bytes (" + percent + "%)");
            }
            fos.flush();
        }

        System.out.println(); // xuống dòng

        // ── Bước 4: Đọc phản hồi hoàn thành (226) ─────────────────────
        String finalResponse = readLine(socketIn);
        System.out.println("[Client] ← " + finalResponse);

        if (finalResponse != null && finalResponse.startsWith("226")) {
            System.out.println("[Client] ✓ Download thành công: " + localFile.getPath()
                    + " (" + totalReceived + " bytes)");
        } else {
            System.out.println("[Client] ✗ Kết thúc có cảnh báo: " + finalResponse);
        }
        System.out.println();
    }

    // ================================================================
    // Upload file (STOR)
    // ================================================================

    /**
     * Thực hiện upload file lên Server.
     */
    private static void handleUpload(String localPath,
                                     PrintWriter serverWriter,
                                     InputStream socketIn,
                                     Socket socket) throws IOException {
        // ── Kiểm tra file local ────────────────────────────────────────
        File localFile = new File(localPath);
        if (!localFile.exists()) {
            System.out.println("[Client] LỖI: File không tồn tại: " + localPath);
            return;
        }
        if (!localFile.isFile()) {
            System.out.println("[Client] LỖI: Không phải file: " + localPath);
            return;
        }

        long fileSize = localFile.length();
        String filename = localFile.getName();

        System.out.println("[Client] Uploading: " + localPath
                + " (" + fileSize + " bytes) → " + filename);

        // ── Bước 1: Gửi STOR <filename> ──────────────────────────────
        serverWriter.println("STOR " + filename);
        System.out.println("[Client] → STOR " + filename);

        // ── Bước 2: Đọc response 125 ─────────────────────────────────
        String ready = readLine(socketIn);
        System.out.println("[Client] ← " + ready);

        if (ready == null || !ready.startsWith("125")) {
            System.out.println("[Client] Upload bị từ chối: " + ready);
            return;
        }

        // ── Bước 3: Gửi SIZE <bytes> ─────────────────────────────────
        serverWriter.println("SIZE " + fileSize);
        System.out.println("[Client] → SIZE " + fileSize);

        // ── Bước 4: Gửi raw bytes của file ───────────────────────────
        OutputStream rawOut = socket.getOutputStream();

        byte[] buffer = new byte[4096];
        long totalSent = 0;

        try (FileInputStream fis = new FileInputStream(localFile)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                rawOut.write(buffer, 0, bytesRead);
                totalSent += bytesRead;

                if (totalSent % (100 * 1024) == 0 || totalSent == fileSize) {
                    long percent = fileSize > 0 ? (totalSent * 100 / fileSize) : 100;
                    System.out.print("\r[Client] Tiến độ: " + totalSent + "/" + fileSize
                            + " bytes (" + percent + "%)");
                }
            }
            rawOut.flush();
        }

        System.out.println(); // xuống dòng sau progress bar

        // ── Bước 5: Đọc response cuối (226 hoặc lỗi) ─────────────────
        String finalResponse = readLine(socketIn);
        System.out.println("[Client] ← " + finalResponse);

        if (finalResponse != null && finalResponse.startsWith("226")) {
            System.out.println("[Client] ✓ Upload thành công: " + filename
                    + " (" + totalSent + " bytes)");
        } else {
            System.out.println("[Client] ✗ Upload thất bại: " + finalResponse);
        }
        System.out.println();
    }

    // ================================================================
    // Đọc text theo từng byte một — KHÔNG buffer-ahead
    // ================================================================

    /**
     * Đọc một dòng text từ InputStream kết thúc bởi '\n'.
     * Bỏ qua '\r'. Không dùng BufferedReader để không bị nuốt byte nhị phân.
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
     * Đọc response từ Server (hỗ trợ cả single-line và multi-line như LIST).
     */
    private static String readResponse(InputStream in) throws IOException {
        String firstLine = readLine(in);
        if (firstLine == null) return null;

        // Multi-line response (LIST: 150 ... 226)
        if (firstLine.startsWith("150")) {
            StringBuilder sb = new StringBuilder(firstLine).append("\n");
            String line;
            while ((line = readLine(in)) != null) {
                sb.append(line).append("\n");
                if (line.startsWith("226")) break;
            }
            return sb.toString().trim();
        }

        return firstLine;
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  USER <username>   - đăng nhập (bước 1)");
        System.out.println("  PASS <password>   - đăng nhập (bước 2)");
        System.out.println("  PWD               - hiển thị thư mục hiện tại");
        System.out.println("  LIST              - liệt kê files/folders");
        System.out.println("  STOR <localfile>  - upload file lên Server");
        System.out.println("  RETR <filename>   - download file từ Server về downloads/");
        System.out.println("  PING              - kiểm tra kết nối");
        System.out.println("  QUIT              - thoát");
        System.out.println("=".repeat(45));
        System.out.println();
    }
}
