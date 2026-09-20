# PHASE 1 — TCP Socket Cơ Bản
## FTP Project · Lập Trình Mạng

> **Mục tiêu Phase 1:**
> Xây dựng nền tảng TCP Socket: Client kết nối → gửi message → nhận response → disconnect.

---

## 📚 1. Lý Thuyết Nền Tảng

### 1.1 TCP (Transmission Control Protocol)

- Giao thức truyền thông **có kết nối** (connection-oriented) và **đáng tin cậy** (reliable).
- Trước khi gửi dữ liệu phải thực hiện **3-way handshake**:

```
Client ──SYN──────────────► Server
Client ◄──SYN-ACK────────── Server
Client ──ACK──────────────► Server
         [Kết nối đã thiết lập]
```

- Dữ liệu **đảm bảo đến nơi**, đúng thứ tự, không mất gói.
- FTP dùng TCP vì cần truyền file **chính xác 100%**.
- Ngược lại: **UDP** — nhanh hơn nhưng không đảm bảo (dùng cho video stream, game online).

---

### 1.2 IP (Internet Protocol)

- **Địa chỉ định danh** của một máy trong mạng, giống như địa chỉ nhà.
- **IPv4:** `192.168.1.100` — 4 nhóm số, mỗi nhóm 0–255.
- **IPv6:** `fe80::1` — dài hơn, dùng cho internet hiện đại.
- Hai máy muốn liên lạc phải biết **IP của nhau**.

---

### 1.3 Port

- **Số từ 0 → 65535**, giúp phân biệt các ứng dụng khác nhau **trên cùng một máy**.
- Địa chỉ đầy đủ = **IP + Port**, ví dụ: `127.0.0.1:6789`

| Port | Dịch vụ |
|------|---------|
| 21   | FTP Control |
| 22   | SSH |
| 80   | HTTP |
| 443  | HTTPS |
| < 1024 | Dành riêng cho hệ điều hành |
| **6789** | **Port ta dùng trong Phase 1** |

---

### 1.4 localhost

- Địa chỉ **loopback** của chính máy đang chạy.
- IP tương đương: **`127.0.0.1`**
- Gói tin gửi đến `localhost` **không ra mạng vật lý**, quay ngay về máy đó.
- Dùng để test Client–Server **trên cùng một máy**.

---

### 1.5 Socket

- **Điểm đầu cuối** (endpoint) của một kết nối TCP.
- Giống như "ổ cắm điện" — hai chương trình cắm vào và truyền dữ liệu.

```
[Client Socket] ←──── TCP Connection ────► [Server Socket]
   IP:PORT                                    IP:PORT
```

- Qua Socket ta **đọc/ghi dữ liệu** như đọc/ghi file.
- Java class: `java.net.Socket`

---

### 1.6 ServerSocket

- Socket đặc biệt **chỉ dùng ở Server**, để **lắng nghe** kết nối đến.
- **Bind** vào một port cố định và **chờ** client gọi vào.
- Khi có client → tạo ra một `Socket` mới để giao tiếp.
- **Bản thân `ServerSocket` không truyền dữ liệu**, chỉ chấp nhận kết nối.

```
ServerSocket(6789)  →  lắng nghe port 6789
        ↓ accept()
    Socket          →  kết nối thực sự với client đó
```

- Java class: `java.net.ServerSocket`

---

### 1.7 accept()

- Lệnh **blocking** — Server **dừng lại và đợi** cho đến khi có client kết nối.
- Khi client kết nối → `accept()` trả về một `Socket` mới.
- Socket mới này là "đường dây riêng" giữa Server và client đó.

```java
Socket clientSocket = serverSocket.accept();
//  ↑ Dừng tại đây cho đến khi có client
```

---

### 1.8 InputStream

- Luồng dữ liệu **đến** (nhận vào).
- Dùng để **đọc** dữ liệu từ socket (data người kia gửi sang).
- Giống như "tai" của socket.
- Wrap bằng `BufferedReader` để đọc text từng dòng:

```java
BufferedReader reader = new BufferedReader(
    new InputStreamReader(socket.getInputStream()));
String line = reader.readLine(); // đọc một dòng
```

---

### 1.9 OutputStream

- Luồng dữ liệu **đi ra** (gửi đi).
- Dùng để **ghi** dữ liệu vào socket (gửi cho người kia).
- Giống như "miệng" của socket.
- Wrap bằng `PrintWriter` để ghi text:

```java
PrintWriter writer = new PrintWriter(
    socket.getOutputStream(), true); // true = auto-flush
writer.println("Hello!"); // gửi một dòng
```

---

### 1.10 Client vs Server

| Tiêu chí | Server | Client |
|----------|--------|--------|
| **Khởi tạo** | `new ServerSocket(port)` | `new Socket(host, port)` |
| **Vai trò** | Chờ và phục vụ | Chủ động kết nối |
| **Port** | Bind cố định vào một port | Dùng port ngẫu nhiên |
| **Thứ tự chạy** | Chạy **trước** | Chạy **sau** |
| **accept()** | Cần gọi để nhận client | Không dùng |
| **Số lượng** | 1 server | Nhiều client |

---

## 🏗️ 2. Cấu Trúc Project

```
FTPProject/
├── src/
│   ├── server/
│   │   └── TCPServer.java    ← Server lắng nghe port 6789
│   └── client/
│       └── TCPClient.java    ← Client kết nối localhost:6789
├── out/                      ← Thư mục chứa .class sau khi compile
│   ├── server/
│   └── client/
└── phase1.md                 ← File này
```

---

## 💻 3. Giải Thích Code

### 3.1 TCPServer.java — Từng bước

```java
// BƯỚC 1: Tạo ServerSocket bind vào port 6789
// Dùng try-with-resources → tự đóng khi xong
try (ServerSocket serverSocket = new ServerSocket(PORT)) {

    // BƯỚC 2: Blocking — dừng đợi đến khi có Client kết nối
    // accept() trả về Socket mới = "đường dây riêng" với Client đó
    Socket clientSocket = serverSocket.accept();

    // BƯỚC 3: Lấy stream để đọc/ghi
    BufferedReader reader = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));  // đọc TỪ client
    PrintWriter writer = new PrintWriter(
            clientSocket.getOutputStream(), true);                  // ghi ĐẾN client

    // BƯỚC 4: Vòng lặp nhận–phản hồi
    String messageFromClient;
    while ((messageFromClient = reader.readLine()) != null) {
        // readLine() = blocking, đợi Client gửi một dòng
        // null = Client đóng kết nối → thoát vòng lặp

        String response = "ECHO: " + messageFromClient; // tạo response
        writer.println(response);                        // gửi về Client

        if (messageFromClient.equalsIgnoreCase("QUIT")) break;
    }

    // BƯỚC 5: Đóng socket của Client
    clientSocket.close();
}
```

---

### 3.2 TCPClient.java — Từng bước

```java
// BƯỚC 1: Kết nối đến Server — TCP handshake xảy ra ngay tại đây
// Nếu Server chưa chạy → ném ConnectException
try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {

    // BƯỚC 2: Lấy stream đọc/ghi + bàn phím
    BufferedReader reader   = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));   // đọc TỪ server
    PrintWriter   writer   = new PrintWriter(
            socket.getOutputStream(), true);                   // ghi ĐẾN server
    BufferedReader keyboard = new BufferedReader(
            new InputStreamReader(System.in));                 // đọc từ bàn phím

    // BƯỚC 3: Vòng lặp nhập–gửi–nhận
    while (true) {
        String userInput = keyboard.readLine(); // blocking, chờ Enter
        if (userInput == null || userInput.trim().isEmpty()) continue;

        writer.println(userInput);              // gửi lên Server
        String response = reader.readLine();    // đợi response từ Server
        if (response == null) break;            // Server đóng kết nối

        System.out.println("Nhận: " + response);
        if (userInput.equalsIgnoreCase("QUIT")) break;
    }
}
// BƯỚC 4: Socket tự đóng nhờ try-with-resources
```

---

### 3.3 Bảng các dòng code quan trọng

| Dòng code | Ý nghĩa |
|-----------|---------|
| `new ServerSocket(PORT)` | Mở cổng 6789, sẵn sàng nhận kết nối |
| `serverSocket.accept()` | **BLOCKING** — dừng đợi client kết nối |
| `new Socket(HOST, PORT)` | Client thực hiện TCP 3-way handshake |
| `new InputStreamReader(...)` | Chuyển byte stream → character stream |
| `new BufferedReader(...)` | Thêm buffer, hỗ trợ `readLine()` |
| `new PrintWriter(..., true)` | `true` = auto-flush, gửi ngay |
| `reader.readLine()` | **BLOCKING** — đợi đến khi nhận `\n` |
| `writer.println(msg)` | Gửi text + `\n` để phía kia đọc được |
| `clientSocket.getInetAddress()` | Lấy IP của client đang kết nối |
| `equalsIgnoreCase("QUIT")` | So sánh không phân biệt hoa/thường |

---

## 📊 4. Sơ Đồ Luồng Dữ Liệu

```
TERMINAL 1 (Server)                    TERMINAL 2 (Client)
══════════════════════                 ══════════════════════
new ServerSocket(6789)
        │
      accept()   ◄──── TCP Handshake ──── new Socket("localhost", 6789)
        │                                           │
        ▼                                           ▼
   clientSocket ◄──────── "Hello!" ─────────────── writer.println()
        │
   reader.readLine()
        │
   writer.println("ECHO: Hello!") ─────────────────► reader.readLine()
                                                          │
                                                    in ra màn hình
        │
        ◄────────────────── "QUIT" ─────────────── writer.println()
        │
      break → clientSocket.close()               break → socket đóng
        │
   serverSocket.close() (auto)
```

---

## 🚀 5. Hướng Dẫn Chạy

### Bước 1 — Compile

```powershell
# Trong thư mục FTPProject
mkdir -Force out
javac -d out src\server\TCPServer.java src\client\TCPClient.java
```

### Bước 2 — Chạy Server (Terminal 1)

```powershell
java -cp out server.TCPServer
```

**Kết quả mong đợi:**
```
=== FTP Project - Phase 1: TCP Server ===
[Server] Đang khởi động...
[Server] Đang lắng nghe trên port 6789...
[Server] Chờ Client kết nối...
```

### Bước 3 — Chạy Client (Terminal 2)

```powershell
java -cp out client.TCPClient
```

### Bước 4 — Giao tiếp

```
[Client] Nhập: Hello Server!
[Client] → Đã gửi: "Hello Server!"
[Client] ← Nhận từ Server: "ECHO: Hello Server!"

[Client] Nhập: QUIT
[Client] → Đã gửi: "QUIT"
[Client] ← Nhận từ Server: "ECHO: QUIT"
[Client] ✓ Đã ngắt kết nối.
```

---

## 🔑 6. Các Khái Niệm Blocking Quan Trọng

> **Blocking** = chương trình **dừng lại tại chỗ đó** và chờ, không làm gì khác.

| Lệnh blocking | Dừng đến khi nào? |
|---------------|-------------------|
| `serverSocket.accept()` | Có client kết nối vào |
| `reader.readLine()` | Nhận được `\n` hoặc kết nối đóng |
| `keyboard.readLine()` | Người dùng nhấn Enter |

---

## ❓ 7. Câu Hỏi Bảo Vệ

### 🟢 Câu 1 (Cơ bản)
> Tại sao phải chạy **Server trước** rồi mới chạy **Client**?
> Điều gì xảy ra nếu chạy Client trước?

### 🟢 Câu 2 (Cơ bản)
> `accept()` trả về kiểu dữ liệu gì?
> Nó khác `ServerSocket` ở chỗ nào?

### 🟡 Câu 3 (Trung bình)
> Tại sao ta dùng `println()` để gửi và `readLine()` để nhận?
> Nếu ta dùng `print()` (không có `\n`) thì chuyện gì xảy ra?

### 🟡 Câu 4 (Trung bình)
> `auto-flush = true` trong `PrintWriter` nghĩa là gì?
> Điều gì xảy ra nếu `auto-flush = false` và ta quên gọi `flush()`?

### 🔴 Câu 5 (Nâng cao)
> Sau khi Client gõ QUIT và vòng lặp kết thúc,
> `ServerSocket` có còn hoạt động không?
> Nếu có Client thứ 2 muốn kết nối, điều gì sẽ xảy ra?

### 🔴 Câu 6 (Nâng cao)
> `localhost` và `127.0.0.1` có khác nhau không?
> Ta có thể đổi `SERVER_HOST` thành IP thực (`192.168.x.x`)
> và vẫn kết nối được không? Điều kiện là gì?

---

## 📌 8. Tóm Tắt Nhanh (Cheatsheet)

```
TCP          = giao thức đáng tin cậy, có kết nối
IP           = địa chỉ máy trong mạng
Port         = cổng logic (0–65535), phân biệt ứng dụng
localhost    = 127.0.0.1, chính máy này
Socket       = điểm đầu cuối của kết nối TCP
ServerSocket = chỉ dùng ở Server, lắng nghe + accept()
accept()     = BLOCKING, chờ Client kết nối, trả về Socket
InputStream  = đọc dữ liệu ĐẾN  → wrap bằng BufferedReader
OutputStream = ghi dữ liệu ĐI   → wrap bằng PrintWriter
readLine()   = BLOCKING, đọc đến khi gặp '\n'
println()    = gửi text + '\n'
auto-flush   = gửi ngay, không cần gọi flush() thủ công
```

---

## 🗺️ 9. Roadmap — Các Phase Tiếp Theo

| Phase | Nội dung |
|-------|----------|
| **Phase 1** ✅ | TCP Socket cơ bản — kết nối, gửi/nhận, disconnect |
| Phase 2 | Đa luồng (Thread) — Server phục vụ nhiều Client |
| Phase 3 | FTP Commands — USER, PASS, LIST, RETR, STOR |
| Phase 4 | File Transfer — gửi/nhận file thực sự |
| Phase 5 | GUI — giao diện đồ họa |

---

*Tài liệu được tạo ngày 20/09/2026 · FTP Project · Phase 1*
