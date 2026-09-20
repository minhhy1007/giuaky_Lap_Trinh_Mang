# PHASE 2 — Application Protocol / Command-Response
## FTP Project · Lập Trình Mạng

> **Mục tiêu Phase 2:**
> Client gửi **command có cấu trúc** → Server **parse** → Server trả **response có mã số**.

---

## 📚 1. Lý Thuyết Bổ Sung

### 1.1 TCP có hiểu command không?

**Không.** TCP hoàn toàn **mù** với nội dung bạn gửi.

- TCP chỉ đảm bảo byte đến đích đúng thứ tự, không mất mát.
- Với TCP, `"PING"` chỉ là 4 byte: `[50][49][4E][47]`
- **Application Protocol** (code của ta) mới quyết định ý nghĩa của byte đó.

---

### 1.2 Byte stream là gì?

Dòng chảy liên tục của byte thô qua kết nối TCP.

```
Client gửi 3 lần:
  "PING\n"       →  [50 49 4E 47 0A]
  "ECHO Hi\n"    →  [45 43 48 4F 20 48 69 0A]
  "QUIT\n"       →  [51 55 49 54 0A]

TCP stream thấy một chuỗi liên tục:
  [50 49 4E 47 0A 45 43 48 4F 20 48 69 0A 51 55 49 54 0A]
```

Ta dùng **`\n` làm delimiter** → `readLine()` tách từng command.

---

### 1.3 Tại sao cần Application Protocol?

| Tầng | Ai xử lý | Ví dụ |
|------|---------|-------|
| Tầng Vật lý | Dây cáp, wifi | Bit 0/1 |
| TCP | Hệ điều hành | Đảm bảo byte đến nơi |
| **Application Protocol** | **Code của ta** | `PING → 200 PONG` |

> Không có protocol → Server không phân biệt được `PING` và `HELLO`.

---

### 1.4 Command là gì?

Chỉ lệnh Client gửi, **theo quy ước đã thống nhất**:

| Command | Cú pháp | Ý nghĩa |
|---------|---------|---------|
| `PING` | `PING` | Kiểm tra Server còn sống không |
| `ECHO` | `ECHO <text>` | Server phản lại đoạn text |
| `QUIT` | `QUIT` | Ngắt kết nối |

---

### 1.5 Response là gì?

Câu trả lời của Server: **mã số + thông điệp** (kiểu FTP/HTTP).

| Mã | Ý nghĩa |
|----|---------|
| `200` | OK — thành công |
| `221` | Goodbye — đóng kết nối |
| `500` | Error — command không hợp lệ |

```
PING        →  200 PONG
ECHO Hello  →  200 Hello
QUIT        →  221 Goodbye
XYZ         →  500 Unknown command: XYZ
```

---

### 1.6 Server parse command thế nào?

```java
String input = "ECHO Hello World";

// Bước 1: trim() — xóa khoảng trắng thừa
input = input.trim();             // "ECHO Hello World"

// Bước 2: split(" ", 2) — tách tại dấu cách đầu tiên, tối đa 2 phần
String[] parts = input.split(" ", 2);
// parts[0] = "ECHO"
// parts[1] = "Hello World"

// Bước 3: toUpperCase() — normalize command
String command = parts[0].toUpperCase();  // "ECHO"

// Bước 4: lấy argument (nếu có)
String argument = (parts.length > 1) ? parts[1] : "";  // "Hello World"

// Bước 5: switch xử lý
switch (command) {
    case "PING": ...
    case "ECHO": ...
    case "QUIT": ...
    default:     return "500 Unknown command";
}
```

---

### 1.7 String command được truyền qua Socket thế nào?

```
[Client] writer.println("PING")
              ↓
   String → encode UTF-8 → bytes → thêm \n
   [50][49][4E][47][0A]
              ↓ TCP stream ↓
   [50][49][4E][47][0A]
              ↓
[Server] reader.readLine()
   Đọc bytes đến khi gặp [0A] (\n)
   [50][49][4E][47] → decode UTF-8 → String "PING"
              ↓
   "PING" sẵn sàng để parse
```

---

## 🏗️ 2. Cấu Trúc Project Phase 2

```
FTPProject/
├── src/
│   ├── server/
│   │   ├── TCPServer.java       ← Sửa tối thiểu từ Phase 1
│   │   └── CommandHandler.java  ← MỚI: bộ não xử lý command
│   └── client/
│       └── TCPClient.java       ← Sửa banner + prompt
├── out/
├── phase1.md
└── phase2.md                    ← File này
```

---

## 💻 3. Thay Đổi So Với Phase 1

### 3.1 Phân tích Phase 1 — vấn đề

```java
// PHASE 1: echo thẳng, không hiểu command
String response = "ECHO: " + messageFromClient;  // mọi input đều echo
if (messageFromClient.equalsIgnoreCase("QUIT")) { // check bằng nội dung thô
    break;
}
```

**Vấn đề:**
- Không phân biệt `PING` / `ECHO` / `QUIT`
- Logic "quit" nằm lẫn trong Server → khó mở rộng
- Không có mã số response

---

### 3.2 Phase 2 — giải pháp

```java
// PHASE 2: delegate cho CommandHandler
CommandHandler handler = new CommandHandler();
String response = handler.handle(messageFromClient); // parse + xử lý
writer.println(response);
if (handler.isQuitResponse(response)) { // check bằng MÃ RESPONSE
    break;
}
```

**Cải tiến:**
- Logic xử lý tách ra `CommandHandler` (Single Responsibility)
- Server không cần biết command là gì, chỉ gọi `handle()` và gửi response
- Kiểm tra quit bằng **mã 221**, không phải nội dung chuỗi

---

## 🔧 4. CommandHandler.java — Chi Tiết

```java
public class CommandHandler {

    public String handle(String rawInput) {
        // 1. Làm sạch input
        String input = rawInput.trim();

        // 2. Tách command và argument
        String[] parts = input.split(" ", 2);
        String command  = parts[0].toUpperCase();     // tên command
        String argument = (parts.length > 1) ? parts[1] : ""; // phần còn lại

        // 3. Phân nhánh xử lý
        switch (command) {
            case "PING": return "200 PONG";
            case "ECHO": return argument.isEmpty()
                            ? "500 ECHO requires a message"
                            : "200 " + argument;
            case "QUIT": return "221 Goodbye";
            default:     return "500 Unknown command: " + parts[0];
        }
    }

    public boolean isQuitResponse(String response) {
        return response != null && response.startsWith("221");
    }
}
```

---

## 📊 5. Luồng Client → Server → Client

### PING

```
Client                              Server
──────                              ──────
Nhập: PING
writer.println("PING")
         ──── "PING\n" ────────────►
                                    reader.readLine() = "PING"
                                    handler.handle("PING")
                                      → command = "PING"
                                      → return "200 PONG"
                                    writer.println("200 PONG")
         ◄─── "200 PONG\n" ─────────
reader.readLine() = "200 PONG"
In: "200 PONG"
```

---

### ECHO Hello World

```
Client                              Server
──────                              ──────
Nhập: ECHO Hello World
writer.println("ECHO Hello World")
         ──── "ECHO Hello World\n" ─►
                                    parts = ["ECHO", "Hello World"]
                                    command  = "ECHO"
                                    argument = "Hello World"
                                    return "200 Hello World"
                                    writer.println("200 Hello World")
         ◄─── "200 Hello World\n" ──
In: "200 Hello World"
```

---

### QUIT

```
Client                              Server
──────                              ──────
Nhập: QUIT
writer.println("QUIT")
         ──── "QUIT\n" ────────────►
                                    command = "QUIT"
                                    return "221 Goodbye"
                                    writer.println("221 Goodbye")
                                    isQuitResponse("221 Goodbye") = true
                                    break → clientSocket.close()
         ◄─── "221 Goodbye\n" ──────
reader.readLine() = "221 Goodbye"
In: "221 Goodbye"
break → socket đóng
```

---

### Command lạ (500)

```
Client                              Server
──────                              ──────
Nhập: HELLO
writer.println("HELLO")
         ──── "HELLO\n" ───────────►
                                    command = "HELLO"
                                    → default case
                                    return "500 Unknown command: HELLO"
                                    writer.println("500 ...")
         ◄─── "500 Unknown..." ──────
In: "500 Unknown command: HELLO"
// Kết nối VẪN mở, Client có thể gửi tiếp
```

---

## 🚀 6. Hướng Dẫn Chạy

### Compile

```powershell
javac -d out src\server\TCPServer.java src\server\CommandHandler.java src\client\TCPClient.java
```

### Chạy Server (Terminal 1)

```powershell
java -cp out server.TCPServer
```

```
=== FTP Project - Phase 2: Application Protocol Server ===
[Server] Đang khởi động...
[Server] Đang lắng nghe trên port 6789...
[Server] Chờ Client kết nối...
```

### Chạy Client (Terminal 2)

```powershell
java -cp out client.TCPClient
```

```
=== FTP Project - Phase 2: Application Protocol Client ===
[Client] ✓ Kết nối thành công!
[Client] Commands: PING | ECHO <text> | QUIT
-------------------------------------------
[Client] Nhập:
```

### Test từng command

```
[Client] Nhập: PING
[Client] → Đã gửi: "PING"
[Client] ← Nhận từ Server: "200 PONG"

[Client] Nhập: ECHO Lap trinh mang
[Client] → Đã gửi: "ECHO Lap trinh mang"
[Client] ← Nhận từ Server: "200 Lap trinh mang"

[Client] Nhập: echo hello    ← viết thường cũng được
[Client] ← Nhận từ Server: "200 hello"

[Client] Nhập: XYZ
[Client] ← Nhận từ Server: "500 Unknown command: XYZ"

[Client] Nhập: ECHO           ← thiếu argument
[Client] ← Nhận từ Server: "500 ECHO requires a message. Usage: ECHO <text>"

[Client] Nhập: QUIT
[Client] ← Nhận từ Server: "221 Goodbye"
[Client] ✓ Đã ngắt kết nối.
```

---

## 🔑 7. Điểm Khác Biệt Phase 1 vs Phase 2

| | Phase 1 | Phase 2 |
|---|---------|---------|
| **Server xử lý** | Echo thẳng mọi message | Parse command, trả response đúng |
| **Response format** | `"ECHO: <input>"` | `"<mã> <thông điệp>"` |
| **Kiểm tra QUIT** | So sánh nội dung chuỗi | Kiểm tra mã `221` |
| **Tách biệt logic** | Lẫn trong Server | Tách ra `CommandHandler` |
| **Command không hợp lệ** | Vẫn echo | Trả lỗi `500` |
| **Case-sensitive** | Có (QUIT ≠ quit) | Không (`toUpperCase()`) |

---

## 📌 8. Tóm Tắt Nhanh

```
Application Protocol  = quy ước ý nghĩa của byte gửi qua TCP
Command               = chỉ lệnh Client gửi (PING, ECHO, QUIT)
Response              = <mã> <thông điệp> (200 PONG, 221 Goodbye)
parse                 = đọc → trim → split → switch
split(" ", 2)         = tách tại dấu cách đầu, tối đa 2 phần
toUpperCase()         = normalize command, không phân biệt hoa/thường
200                   = OK
221                   = Goodbye / closing connection
500                   = Unknown / Error
CommandHandler        = class riêng xử lý command (tách khỏi Server)
isQuitResponse()      = check mã 221 để biết lúc nào đóng kết nối
```

---

## 🗺️ 9. Roadmap

| Phase | Nội dung | Trạng thái |
|-------|----------|-----------|
| Phase 1 | TCP Socket cơ bản | ✅ Hoàn thành |
| **Phase 2** | **Application Protocol** | ✅ **Hoàn thành** |
| Phase 3 | Đa luồng (Thread) — nhiều Client | ⏳ Tiếp theo |
| Phase 4 | FTP Commands — USER, PASS, LIST | ⏳ |
| Phase 5 | File Transfer — RETR, STOR | ⏳ |
| Phase 6 | GUI | ⏳ |

---

*Tài liệu được tạo ngày 20/09/2026 · FTP Project · Phase 2*
