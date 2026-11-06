package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * PHASE 5 - Client Handler với File Transfer
 *
 * Thay đổi so với Phase 4:
 *   - Thay BufferedReader bằng ProtocolInputStream (không buffer ahead)
 *   - Phát hiện STOR command → gọi handleStor() thay vì handle()
 *   - Truyền StorageManager vào CommandHandler
 *
 * Tại sao phải xử lý STOR khác biệt?
 *   STOR cần:
 *     1. Gửi "125 Ready" (intermediate response) trong lúc xử lý
 *     2. Đọc thêm dòng "SIZE <n>"
 *     3. Đọc raw bytes (binary data)
 *   Những việc này cần truy cập trực tiếp vào stream → không thể
 *   trả về qua handle() bình thường.
 *
 * Sơ đồ:
 *   ClientHandler
 *     ├── ProtocolInputStream  (đọc từng byte, không buffer)
 *     ├── PrintWriter          (ghi response)
 *     └── CommandHandler
 *           ├── Session        (riêng)
 *           ├── UserDatabase   (chung)
 *           └── StorageManager (chung)
 */
public class ClientHandler implements Runnable {



    private final Socket         clientSocket;
    private final CommandHandler handler;
    private final String         clientId;

    /**
     * @param clientSocket   Socket của Client này
     * @param clientId       ID hiển thị trên console
     * @param userDatabase   DB tài khoản (chung, read-only)
     * @param storageManager Quản lý file (chung, thread-safe)
     */
    public ClientHandler(Socket clientSocket, String clientId,
                         UserDatabase userDatabase, StorageManager storageManager) {
        this.clientSocket = clientSocket;
        this.clientId     = clientId;

        // Tạo Session RIÊNG cho Client này
        Session session = new Session();

        // Inject tất cả dependency vào CommandHandler
        this.handler = new CommandHandler(session, userDatabase, storageManager);
        if (clientSocket != null && clientSocket.getInetAddress() != null) {
            this.handler.setClientIp(clientSocket.getInetAddress().getHostAddress());
        }
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();

        System.out.println("[" + clientId + "] Kết nối mới"
                + " | IP: "     + clientSocket.getInetAddress().getHostAddress()
                + ":"           + clientSocket.getPort()
                + " | Thread: " + threadName);

        try (Socket socket = clientSocket) {

            // ── PHASE 5: Dùng ProtocolInputStream thay vì BufferedReader ──
            // Không buffer ahead → an toàn khi mix text + binary
            ProtocolInputStream lineReader =
                    new ProtocolInputStream(socket.getInputStream());

            // PrintWriter dùng UTF-8 chuẩn
            PrintWriter writer = new PrintWriter(
                    new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);

            String commandLine;

            while ((commandLine = lineReader.readLine()) != null) {

                if (commandLine.trim().isEmpty()) continue;

                System.out.println("[" + clientId + "] <<< \"" + commandLine + "\"");

                String response;

                // ── Phát hiện STOR hoặc RETR → xử lý đặc biệt ─────────────
                if (commandLine.trim().toUpperCase().startsWith("STOR")) {
                    String[] parts   = commandLine.trim().split("\\s+", 2);
                    String filename  = (parts.length > 1) ? parts[1].trim() : "";
                    response = handler.handleStor(filename, lineReader, writer);

                } else if (commandLine.trim().toUpperCase().startsWith("RETR")) {
                    String[] parts   = commandLine.trim().split("\\s+", 2);
                    String filename  = (parts.length > 1) ? parts[1].trim() : "";
                    // handleRetr() gửi 150 -> bơm bytes -> trả response 226
                    response = handler.handleRetr(filename, writer, socket.getOutputStream());

                } else if (commandLine.trim().toUpperCase().startsWith("GETSHARED")) {
                    String[] parts   = commandLine.trim().split("\\s+", 2);
                    String token     = (parts.length > 1) ? parts[1].trim() : "";
                    response = handler.handleGetShared(token, writer, socket.getOutputStream());

                } else {
                    // ── Command thông thường → handle() bình thường ───────
                    response = handler.handle(commandLine);
                }

                // Gửi response cuối (nếu null thì bỏ qua)
                if (response != null) {
                    writer.println(response);
                    System.out.println("[" + clientId + "] >>> \"" + response + "\"");
                }

                // Kiểm tra QUIT
                if (handler.isQuitResponse(response)) {
                    System.out.println("[" + clientId + "] Đóng kết nối (QUIT).");
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("[" + clientId + "] Mất kết nối: " + e.getMessage());
        } finally {
            System.out.println("[" + clientId + "] Thread "
                    + Thread.currentThread().getName() + " kết thúc.\n");
        }
    }
}
