# PHASE 3 — Multi-Client / Thread Pool
## FTP Project · Lập Trình Mạng

> **Mục tiêu Phase 3:**
> Server phục vụ **nhiều Client đồng thời** — mỗi Client có Thread riêng,
> không Client nào phải chờ Client khác.

---

## 📚 1. Lý Thuyết Bổ Sung

### 1.1 Tại sao Server single-client có vấn đề?

```
[Phase 2 — Single-Client]

main Thread:
  accept()  ← chờ Client A
      ↓ Client A kết nối
  readLine() ← BLOCKING, đang chờ Client A gõ lệnh...
      ↓
      ↓  ← Client B gõ cửa vào lúc này!
      ↓     Server đang bận readLine() → KHÔNG THỂ accept()
      ↓     Client B BỊ TỪ CHỐI hoặc phải đợi vô thời hạn
      ↓
  Client A gõ QUIT
  accept()  ← BÂY GIỜ MỚI nhận Client B (quá muộn!)
```

**Kết luận:** Phase 2 chỉ phục vụ **1 client tại một thời điểm**.

---

### 1.2 Thread là gì?

**Thread** = luồng thực thi độc lập bên trong chương trình.

```
JVM Process (một chương trình)
├── main Thread       ← vòng lặp accept()
├── pool-1-thread-1   ← phục vụ Client A (readLine blocking ở đây)
├── pool-1-thread-2   ← phục vụ Client B (readLine blocking ở đây)
└── pool-1-thread-3   ← phục vụ Client C (readLine blocking ở đây)
```

- Mỗi Thread có **stack riêng** (biến cục bộ, call stack).
- Các Thread dùng **chung heap** (object được tạo bằng `new`).
- Khi Thread-1 bị block, **Thread-2 và Thread-3 vẫn chạy bình thường**.

---

### 1.3 ClientHandler là gì?

Class **đại diện cho phiên làm việc với 1 Client**, implement `Runnable`:

```java
public class ClientHandler implements Runnable {
    private final Socket clientSocket;  // đường dây riêng với Client đó
    private final CommandHandler handler;

    @Override
    public void run() {
        // toàn bộ vòng lặp readLine() → handle() → println()
        // chạy trong Thread riêng của ClientHandler này
    }
}
```

| Tạo từ | Phục vụ |
|--------|--------|
| `new ClientHandler(socketA, "Client#1")` | Client A, trong Thread-1 |
| `new ClientHandler(socketB, "Client#2")` | Client B, trong Thread-2 |
| `new ClientHandler(socketC, "Client#3")` | Client C, trong Thread-3 |

---

### 1.4 ExecutorService là gì?

**Bộ quản lý Thread** — thay ta tạo, hủy, tái sử dụng Thread.

```java
// Thủ công (nguy hiểm):
new Thread(handler).start();  // ai dọn dẹp? bao nhiêu thread là đủ?

// Dùng ExecutorService (an toàn):
executor.submit(handler);     // nó tự điều phối
```

---

### 1.5 Thread Pool là gì?

**Tập hợp Thread tạo sẵn, tái sử dụng** thay vì tạo mới mỗi lần:

```
Không có Pool (lãng phí):
  Client A → tạo Thread mới (tốn ~1ms overhead)
  Client A ngắt → Thread bị hủy (phí)
  Client B → tạo Thread mới (tốn ~1ms overhead lại)

Có CachedThreadPool (hiệu quả):
  Client A → lấy Thread từ pool (gần như ngay lập tức)
  Client A ngắt → trả Thread về pool (không hủy, chờ tái dùng)
  Client B → lấy Thread từ pool (ngay lập tức)
```

`Executors.newCachedThreadPool()`:
- Tự tạo Thread mới khi cần (không giới hạn số Thread)
- Tái sử dụng Thread nhàn rỗi
- Thu hồi Thread nhàn rỗi sau 60 giây

---

### 1.6 Tại sao mỗi Socket cần Thread riêng?

Vì `readLine()` là **blocking**:

```
Nếu 1 Thread phục vụ 2 Client:
  Thread đang readLine() chờ Client A gõ lệnh...
  Client B cần response → Thread đang bận → Client B PHẢI ĐỢI!

Nếu mỗi Client có Thread riêng:
  Thread-1: readLine() chờ Client A → block chỉ ảnh hưởng Thread-1
  Thread-2: readLine() chờ Client B → chạy độc lập, không bị ảnh hưởng
  main:     accept() chờ Client C   → vẫn hoạt động bình thường
```

---

## 🏗️ 2. Cấu Trúc Project Phase 3

```
FTPProject/
├── src/
│   ├── server/
│   │   ├── TCPServer.java        ← Viết lại: vòng lặp accept() + ExecutorService
│   │   ├── ClientHandler.java    ← MỚI: Runnable xử lý 1 client
│   │   └── CommandHandler.java   ← Giữ nguyên từ Phase 2
│   └── client/
│       └── TCPClient.java        ← Giữ nguyên từ Phase 2
├── phase1.md
├── phase2.md
└── phase3.md                     ← File này
```

---

## 💻 3. Thay Đổi So Với Phase 2

### 3.1 TCPServer.java — cốt lõi thay đổi

```diff
- // Phase 2: accept() 1 lần, xử lý 1 client, dừng
- Socket clientSocket = serverSocket.accept();
- CommandHandler handler = new CommandHandler();
- while ((messageFromClient = reader.readLine()) != null) {
-     String response = handler.handle(messageFromClient);
-     writer.println(response);
-     if (handler.isQuitResponse(response)) break;
- }

+ // Phase 3: vòng lặp vô hạn, mỗi client có thread riêng
+ ExecutorService threadPool = Executors.newCachedThreadPool();
+ while (true) {
+     Socket clientSocket = serverSocket.accept();   // chờ client mới
+     String clientId = "Client#" + clientCount.incrementAndGet();
+     ClientHandler handler = new ClientHandler(clientSocket, clientId);
+     threadPool.submit(handler);                   // giao cho pool, quay lại accept() ngay
+ }
```

### 3.2 ClientHandler.java — toàn bộ logic giao tiếp từ Phase 2

```java
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final CommandHandler handler;

    @Override
    public void run() {
        // Vòng lặp từ Phase 2, nhưng chạy trong Thread riêng:
        BufferedReader reader = ...;
        PrintWriter writer = ...;
        while ((commandLine = reader.readLine()) != null) {
            String response = handler.handle(commandLine);
            writer.println(response);
            if (handler.isQuitResponse(response)) break;
        }
    }
}
```

---

## 📊 4. Sơ Đồ Thread — 3 Client Đồng Thời

```
[main Thread]                [pool-thread-1]          [pool-thread-2]          [pool-thread-3]
──────────────                ───────────────          ───────────────          ───────────────
accept()...
    │ Client A kết nối
submit(handlerA) ──────────► handlerA.run()
accept()...                  readLine() blocking...
    │ Client B kết nối
submit(handlerB) ───────────────────────────────────► handlerB.run()
accept()...                  readLine() blocking...    readLine() blocking...
    │ Client C kết nối
submit(handlerC) ────────────────────────────────────────────────────────────► handlerC.run()
accept()...                  readLine() blocking...    readLine() blocking...    readLine() blocking...
    │
    │  ← 3 Client phục vụ song song, main Thread chờ Client D
    │
    │  Client A gõ: PING
    │                 <<< "PING"
    │                 handle() → "200 PONG"
    │                 >>> "200 PONG"
    │
    │  Client B gõ: ECHO Hi         ← xảy ra CÙNG LÚC với Client A
    │                                  <<< "ECHO Hi"
    │                                  handle() → "200 Hi"
    │                                  >>> "200 Hi"
    │
    │  Client A gõ: QUIT
    │                 <<< "QUIT"
    │                 handle() → "221 Goodbye"
    │                 >>> "221 Goodbye"
    │                 break → socket đóng
    │                 Thread-1 kết thúc → trả về pool
    │
accept()...                  [Thread-1 về pool]        readLine() blocking...    readLine() blocking...
```

---

## 🚀 5. Hướng Dẫn Chạy và Test

### Compile

```powershell
javac -d out src\server\TCPServer.java src\server\ClientHandler.java src\server\CommandHandler.java src\client\TCPClient.java
```

### Chạy Server

```powershell
java -cp out server.TCPServer
```

### Test 2 Client đồng thời

Mở **Terminal 2** và **Terminal 3** cùng lúc:
```powershell
java -cp out client.TCPClient  # Terminal 2
java -cp out client.TCPClient  # Terminal 3
```

**Console Server sẽ thấy:**
```
=== FTP Project - Phase 3: Multi-Client Server ===
[Server] Chờ Client kết nối...

[Server] ✓ Client#1 kết nối! | Tổng đang kết nối: 1
[Client#1] Đã kết nối. | IP: 127.0.0.1:54231 | Thread: pool-1-thread-1
[Server] → Giao Client#1 cho Thread Pool. Main Thread quay lại chờ...

[Server] ✓ Client#2 kết nối! | Tổng đang kết nối: 2
[Client#2] Đã kết nối. | IP: 127.0.0.1:54232 | Thread: pool-1-thread-2
[Server] → Giao Client#2 cho Thread Pool. Main Thread quay lại chờ...
```

**Khi Client#1 gõ PING:**
```
[Client#1] <<< "PING"
[Client#1] >>> "200 PONG"
```

**Khi Client#2 gõ ECHO Hello (cùng lúc):**
```
[Client#2] <<< "ECHO Hello"
[Client#2] >>> "200 Hello"
```

**Hai Client chat độc lập, không ảnh hưởng nhau.**

---

## 🔑 6. AtomicInteger — Tại Sao Cần?

```java
// Dùng int thường — KHÔNG AN TOÀN với đa luồng:
private static int count = 0;

// Thread-1 đọc: count = 5
// Thread-2 đọc: count = 5  ← cùng lúc!
// Thread-1 ghi: count = 6
// Thread-2 ghi: count = 6  ← sai! phải là 7

// Dùng AtomicInteger — AN TOÀN:
private static AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet(); // một thao tác NGUYÊN TỬ, không thể bị chen ngang
```

---

## 🔑 7. Điểm Khác Biệt Phase 2 vs Phase 3

| | Phase 2 | Phase 3 |
|---|---------|---------|
| **Số client cùng lúc** | 1 | Không giới hạn |
| **accept() gọi** | 1 lần | Vô hạn (while true) |
| **Xử lý client** | Trong main Thread | Trong Thread riêng (pool) |
| **Server dừng khi** | Client gõ QUIT | Ctrl+C (hoặc Exception) |
| **ExecutorService** | Không có | `newCachedThreadPool()` |
| **AtomicInteger** | Không cần | Đếm client thread-safe |
| **ClientHandler** | Không có | `implements Runnable` |

---

## 📌 8. Tóm Tắt Nhanh

```
Thread               = luồng thực thi độc lập
Runnable             = interface có method run(), Thread gọi khi bắt đầu
ClientHandler        = Runnable xử lý đúng 1 client, trong 1 thread
ExecutorService      = bộ quản lý thread
newCachedThreadPool()= pool tự mở rộng, tái dụng thread nhàn rỗi
submit(runnable)     = giao task cho pool, không chờ task xong
while (true) accept()= main thread chỉ làm 1 việc: nhận client mới
AtomicInteger        = int an toàn trong môi trường đa luồng
incrementAndGet()    = tăng 1 nguyên tử, trả về giá trị mới
finally              = block chạy kể cả khi có exception
```

---

## ❓ 9. Câu Hỏi Bảo Vệ

### 🟢 Câu 1
> Trong Phase 3, main Thread làm gì trong vòng lặp `while (true)`?
> Nó có tham gia xử lý command của Client không?

### 🟢 Câu 2
> Tại sao `ClientHandler` implement `Runnable` thay vì `extends Thread`?

### 🟡 Câu 3
> `threadPool.submit(handler)` có chờ `handler.run()` hoàn thành không?
> Main Thread làm gì ngay sau lệnh `submit()`?

### 🟡 Câu 4
> Nếu Client A đang gõ lệnh rất chậm, Client B có bị ảnh hưởng không?
> Giải thích tại sao.

### 🔴 Câu 5
> `CommandHandler` không có từ khóa `synchronized` hay `volatile`.
> Mỗi `ClientHandler` tạo `CommandHandler` riêng.
> Đây có phải thiết kế đúng không? Điều gì xảy ra nếu ta chỉ tạo
> **một `CommandHandler` duy nhất** và chia sẻ giữa tất cả Thread?

### 🔴 Câu 6
> `newCachedThreadPool()` không giới hạn số Thread.
> Nếu có **10.000 Client** kết nối cùng lúc → tạo 10.000 Thread → crash.
> Dùng loại pool nào để giới hạn tối đa 50 Thread đồng thời?

---

## 🗺️ 10. Roadmap

| Phase | Nội dung | Trạng thái |
|-------|----------|-----------|
| Phase 1 | TCP Socket cơ bản | ✅ |
| Phase 2 | Application Protocol | ✅ |
| **Phase 3** | **Multi-Client / Thread Pool** | ✅ **Hoàn thành** |
| Phase 4 | FTP Commands — USER, PASS, LIST | ⏳ |
| Phase 5 | File Transfer — RETR, STOR | ⏳ |
| Phase 6 | GUI | ⏳ |

---

*Tài liệu được tạo ngày 20/09/2026 · FTP Project · Phase 3*
