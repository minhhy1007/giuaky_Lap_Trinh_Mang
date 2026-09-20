package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PHASE 3 - TCP Server hỗ trợ Multi-Client
 *
 * Thay đổi so với Phase 2:
 *   - Vòng lặp accept() chạy VÔ HẠN (while true)
 *   - Mỗi Client được giao cho ClientHandler trong Thread riêng
 *   - Dùng ExecutorService (Thread Pool) để quản lý Thread
 *   - Server KHÔNG BAO GIỜ dừng vì một Client ngắt kết nối
 *
 * Sơ đồ hoạt động:
 *
 * PHASE 4 - TCP Server với Authentication
 *
 * Thay đổi so với Phase 3:
 *   - Tạo UserDatabase MỘT LẦN khi khởi động (dùng chung, read-only)
 *   - Truyền UserDatabase vào từng ClientHandler
 *   - Mỗi ClientHandler tự tạo Session riêng
 *
 * UserDatabase dùng chung vì:
 *   - Chỉ đọc (read-only) → không có race condition
 *   - Tạo một lần, không tốn bộ nhớ
 */
public class TCPServer {

    private static final int PORT = 6789;

    // ----------------------------------------------------------------
    // AtomicInteger: bộ đếm an toàn trong môi trường đa luồng.
    // Dùng để đặt tên cho từng Client: Client#1, Client#2, ...
    //
    // Tại sao không dùng int thường?
    //   Nếu 2 Thread cùng đọc/tăng một int thông thường cùng lúc
    //   → có thể bị "race condition" → số đếm sai.
    //   AtomicInteger đảm bảo mỗi lần tăng là một thao tác NGUYÊN TỬ.
    // ----------------------------------------------------------------
    private static final AtomicInteger clientCount = new AtomicInteger(0);

    public static void main(String[] args) {

        System.out.println("=== FTP Project - Phase 5: File Transfer Server ===");
        System.out.println("[Server] Đang khởi động...");

        // ----------------------------------------------------------------
        // Tạo ExecutorService với CachedThreadPool.
        // newCachedThreadPool():
        //   - Tạo Thread mới khi cần (không giới hạn số Thread)
        //   - Tái sử dụng Thread nhàn rỗi thay vì tạo mới
        //   - Tự thu hồi Thread nhàn rỗi sau 60 giây
        //
        // Phù hợp cho ứng dụng có nhiều task nhỏ, ngắn hạn.
        // ----------------------------------------------------------------
        // ----------------------------------------------------------------
        // PHASE 4: Tạo UserDatabase MỘT LẦN khi Server khởi động.
        // Tất cả ClientHandler dùng CHUNG database này (read-only).
        // An toàn vì UserDatabase không có write operation.
        // ----------------------------------------------------------------
        UserDatabase userDatabase = new UserDatabase();
        System.out.println("[Server] UserDatabase: admin/123, alice/alice123, bob/bob456, guest/guest");

        // ----------------------------------------------------------------
        // PHASE 5: Tạo StorageManager MỘT LẦN khi khởi động.
        // Tạt cả ClientHandler dùng CHUNG — an toàn vì StorageManager
        // chỉ có read + write vào các file khác nhau (mỗi client một file riêng).
        // ----------------------------------------------------------------
        StorageManager storageManager;
        try {
            storageManager = new StorageManager();
        } catch (java.io.IOException e) {
            System.err.println("[Server] Không thể tạo storage directory: " + e.getMessage());
            return;
        }

        ExecutorService threadPool = Executors.newCachedThreadPool();

        // ----------------------------------------------------------------
        // try-with-resources cho ServerSocket VÀ threadPool
        // Khi chương trình kết thúc (hoặc exception), cả hai tự đóng.
        // ----------------------------------------------------------------
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("[Server] Đang lắng nghe trên port " + PORT + "...");
            System.out.println("[Server] Thread Pool sẵn sàng.");
            System.out.println("[Server] Chờ Client kết nối... (Ctrl+C để dừng)\n");
            System.out.println("=".repeat(50));

            // ----------------------------------------------------------------
            // VÒNG LẶP VÔ HẠN — đây là thay đổi cốt lõi của Phase 3
            //
            // Phase 2: accept() gọi 1 lần → phục vụ 1 client → dừng
            // Phase 3: accept() gọi liên tục → mỗi client có thread riêng
            //
            // Vòng lặp này chạy trên "main Thread" và CHỈ làm một việc:
            //   nhận kết nối mới → giao cho pool → nhận kết nối mới → ...
            // ----------------------------------------------------------------
            while (true) {

                // accept() BLOCKING: main Thread dừng tại đây cho đến khi
                // có Client mới kết nối. Trong lúc đó, các Thread trong pool
                // vẫn tiếp tục phục vụ các Client đang kết nối.
                Socket clientSocket = serverSocket.accept();

                // Tăng bộ đếm và tạo ID cho Client này
                // incrementAndGet() tăng 1 và trả về giá trị mới (thread-safe)
                int id = clientCount.incrementAndGet();
                String clientId = "Client#" + id;

                System.out.println("\n[Server] ✓ " + clientId + " kết nối!"
                        + " | Tổng đang kết nối: " + id);

                // ----------------------------------------------------------------
                // Tạo ClientHandler cho Client vừa kết nối
                // Truyền vào: socket (đường dây riêng) và clientId (để log)
                // ----------------------------------------------------------------
                // PHASE 5: Truyền cả userDatabase và storageManager.
                ClientHandler handler = new ClientHandler(
                        clientSocket, clientId, userDatabase, storageManager);

                // ----------------------------------------------------------------
                // submit() giao ClientHandler cho Thread Pool.
                // Pool lấy một Thread sẵn có (hoặc tạo mới nếu không có)
                // và gọi handler.run() trong Thread đó.
                //
                // Main Thread KHÔNG CHỜ handler hoàn thành.
                // Main Thread quay lại accept() NGAY LẬP TỨC.
                // ----------------------------------------------------------------
                threadPool.submit(handler);

                System.out.println("[Server] → Giao " + clientId
                        + " cho Thread Pool. Main Thread quay lại chờ...");
            }

        } catch (IOException e) {
            System.err.println("[Server] LỖI: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Khi vòng lặp kết thúc (do Exception hoặc tắt server),
            // yêu cầu Thread Pool dừng: không nhận task mới,
            // đợi các task đang chạy hoàn thành.
            threadPool.shutdown();
            System.out.println("[Server] Thread Pool đã đóng. Server dừng.");
        }
    }
}
