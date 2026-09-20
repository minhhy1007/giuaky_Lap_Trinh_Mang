# PHASE 5 — FTP Command + Directory Management + File Upload
## FTP Project · Lập Trình Mạng

> **Mục tiêu Phase 5:**
> Xây dựng phần lõi quản lý file và thư mục trên Server:
> 1. **Command Processor mở rộng:** Xử lý chuẩn xác các lệnh FTP theo giao thức mã 3 chữ số (RFC 959).
> 2. **Directory Management + LIST:** Xem thư mục hiện tại (`PWD`), duyệt danh sách file/thư mục (`LIST`), kiểm soát an toàn trong thư mục `storage/`.
> 3. **Chống tấn công Path Traversal:** Tuyệt đối không cho Client thoát ra ngoài thư mục gốc của Server (`../../`).
> 4. **File Upload (STOR):** Truyền file nhị phân (binary) qua TCP Socket bằng cơ chế Stream & Buffer 4096 bytes, giải quyết dứt điểm vấn đề đệm trước (**Buffer-Ahead**) của `BufferedReader`.

---

## 🗺️ BẢN ĐỒ CODE: TỪNG CHỨC NĂNG NẰM Ở FILE NÀO?

Để nắm rõ toàn bộ hệ thống mà không bị nhầm lẫn, bảng dưới đây phân định chính xác nhiệm vụ của từng file trong project:

```
FTPProject/
├── src/
│   ├── server/
│   │   ├── TCPServer.java           ← Khởi động Server, mở Port 6789, tạo StorageManager dùng chung
│   │   ├── ClientHandler.java       ← Luồng riêng cho từng Client, nhận diện lệnh Text vs Stream STOR
│   │   ├── ProtocolInputStream.java ← Đọc byte-by-byte tránh nuốt nhầm byte dữ liệu nhị phân
│   │   ├── StorageManager.java      ← Toàn bộ thao tác File/Folder: LIST, STOR, chống Path Traversal
│   │   ├── CommandHandler.java      ← Phân tích cú pháp (Parser) & Điều hướng lệnh (Router)
│   │   ├── Session.java             ← Quản lý trạng thái đăng nhập & Current Directory của Client
│   │   └── UserDatabase.java        ← Danh sách tài khoản test (admin, alice, bob...)
│   └── client/
│       └── TCPClient.java           ← Client console: nhập lệnh, đọc LIST nhiều dòng, upload file STOR
├── storage/                         ← Thư mục lưu trữ an toàn duy nhất trên Server
│   └── README.txt
├── test_upload.txt                  ← File mẫu dùng để test upload
└── phase5.md                        ← File tài liệu kiến thức tổng hợp này
```

### Bảng tra cứu nhanh "Code nằm ở đâu?":

| Chức năng / Xử lý | File đảm nhiệm | Method / Dòng code chính |
|---|---|---|
| **Đọc byte-by-byte, không buffer-ahead** | [`ProtocolInputStream.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/ProtocolInputStream.java) | `readLine()`, `getRawInputStream()` |
| **Phòng chống Path Traversal (`..`)** | [`StorageManager.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/StorageManager.java) | `resolveSafe(currentDir, clientPath)` |
| **Liệt kê file/thư mục (`LIST`)** | [`StorageManager.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/StorageManager.java) | `listDirectory(currentDir)` |
| **Ghi file nhị phân qua Buffer 4096** | [`StorageManager.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/StorageManager.java) | `receiveFile(...)` |
| **Phân tích cú pháp & Router lệnh** | [`CommandHandler.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/CommandHandler.java) | `handle(rawInput)` |
| **Giao thức 3 bước của `STOR`** | [`CommandHandler.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/CommandHandler.java) | `handleStor(filename, lineReader, writer)` |
| **Bắt luồng chuyển tiếp Text / Stream** | [`ClientHandler.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/ClientHandler.java) | Vòng lặp `while` trong `run()` |
| **Lưu đường dẫn hiện tại của Session** | [`Session.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/Session.java) | `currentDirectory`, `getCurrentDirectory()` |
| **Khởi tạo root thư mục `storage/`** | [`TCPServer.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/TCPServer.java) | Khởi tạo `StorageManager` trong `main()` |
| **Đọc response LIST nhiều dòng** | [`TCPClient.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/client/TCPClient.java) | `readResponse(reader)` |
| **Đọc file local & bắn raw bytes lên Server** | [`TCPClient.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/client/TCPClient.java) | `handleUpload(...)` |

---

## 📚 1. LÝ THUYẾT NỀN TẢNG THEO TỪNG BƯỚC

---

### BƯỚC 1: FTP COMMAND PROCESSOR

#### 1.1 FTP Command là gì?
Là chuỗi ký tự ASCII được Client gửi đến Server kết thúc bằng `\r\n` hoặc `\n`. Theo chuẩn RFC 959:
- Bắt đầu bằng **Tên lệnh** (in hoa): `USER`, `PASS`, `LIST`, `STOR`, `PWD`, `QUIT`...
- Theo sau là **Tham số (Argument)** nếu có, cách nhau bởi khoảng trắng: `USER admin`, `STOR tailieu.pdf`.

#### 1.2 FTP Response là gì?
Là chuỗi phản hồi từ Server gửi về cho Client, bắt đầu bằng **mã trạng thái 3 chữ số**:
- **`1xx` - Đang tiến hành (Positive Preliminary):** `125 Ready for transfer`, `150 Directory listing started`.
- **`2xx` - Thành công (Positive Completion):** `200 Command OK`, `226 Transfer complete`, `230 Login OK`, `257 Current directory`.
- **`3xx` - Cần thông tin tiếp theo (Intermediate):** `331 Username accepted, need password`.
- **`4xx` - Lỗi tạm thời (Transient Negative):** `426 Connection closed; transfer aborted`, `451 Action aborted`.
- **`5xx` - Lỗi vĩnh viễn / Từ chối (Permanent Negative):** `500 Syntax error`, `501 Bad argument`, `530 Not logged in`, `550 Access denied`.

#### 1.3 Command Parser vs Command Handler vs Business Logic là gì?
- **Command Parser:** Nhiệm vụ bóc tách chuỗi thô. Ví dụ chuỗi `"STOR test.txt"` được cắt thành `command = "STOR"` và `argument = "test.txt"`.
- **Command Handler (Router):** Đóng vai trò là cảnh sát giao thông. Kiểm tra: Client đã login chưa? Nếu có quyền thì chuyển lệnh đó tới hàm nghiệp vụ tương ứng.
- **Business Logic:** Logic thực tế của chức năng: đọc đĩa, ghi file, tính toán, gọi DB.

#### 1.4 Tại sao không viết toàn bộ trong 1 method duy nhất?
Nếu nhồi nhét tất cả vào hàm `run()` của `ClientHandler`:
1. Vi phạm nguyên lý **Single Responsibility** (Trách nhiệm đơn nhất).
2. Mã nguồn phình to hàng nghìn dòng, cực kỳ khó đọc và debug.
3. Không thể tái sử dụng hoặc viết Unit Test độc lập cho nghiệp vụ File I/O.
4. Tách riêng `StorageManager` giúp việc đổi cách lưu trữ (sang Cloud, AWS S3, Database) sau này không ảnh hưởng đến giao thức mạng.

---

### BƯỚC 2: DIRECTORY MANAGEMENT & BẢO MẬT PATH TRAVERSAL

#### 2.1 Các khái niệm cơ bản về File & Path
- **File:** Tập hợp các byte dữ liệu được đặt tên lưu trữ trên bộ nhớ thứ cấp (HDD/SSD).
- **Directory (Thư mục):** File đặc biệt chứa danh sách các file và thư mục con khác.
- **Path (Đường dẫn):** Chuỗi ký tự định vị một tài nguyên trong hệ thống file.
- **Absolute Path (Đường dẫn tuyệt đối):** Đi từ gốc ổ đĩa (VD: `D:\Lap_Trinh_Mang\FTPProject\storage\file.txt`).
- **Relative Path (Đường dẫn tương đối):** Tính từ thư mục làm việc hiện tại (VD: `file.txt` hoặc `docs/readme.txt`).
- **`File.listFiles()`:** Hàm có sẵn của Java để đọc toàn bộ thư mục, trả về mảng các đối tượng `File[]`.

#### 2.2 Current Directory & Storage Directory là gì?
- **Storage Directory (`storageRoot`):** Thư mục duy nhất trên Server được cấp quyền làm "kho". Client chỉ được thao tác bên trong kho này.
- **Current Directory:** Vị trí hiện tại của Session của từng Client tính tương đối từ `storageRoot` (mặc định là `""` - tức ngay tại root của storage).

#### 2.3 Vì sao không cho Client truy cập tùy ý vào file hệ thống?
Nếu không giới hạn, Client ác ý có thể:
- Đọc trộm file cấu hình, mật khẩu hệ thống (`C:\Windows\System32\...` hoặc `/etc/passwd`).
- Ghi đè file thực thi, lây nhiễm mã độc để chiếm quyền điều khiển Server.

#### 2.4 LỖ HỔNG PATH TRAVERSAL LÀ GÌ? CƠ CHẾ VÀ CÁCH CHẶNG ĐỨNG
- **Kỹ thuật tấn công:** Kẻ tấn công dùng ký tự chuyển hướng thư mục cha (`..`) để "vượt ngục" khỏi thư mục được phép:
  ```
  Thư mục cho phép: D:\FTPProject\storage
  Kẻ tấn công gửi:   ../../Windows/System32/calc.exe
  Nếu nối chuỗi thô: D:\FTPProject\storage\..\..\Windows\System32\calc.exe
  Đường dẫn thực:    D:\Windows\System32\calc.exe  ==> BỊ THOÁT RA NGOÀI!
  ```

- **Giải pháp 3 bước triệt để trong `StorageManager.java`:**
  ```java
  // 1. Nối với thư mục gốc
  Path resolved = storageRoot.resolve(currentDir).resolve(clientPath);
  
  // 2. Chuẩn hóa để triệt tiêu toàn bộ ".." và "."
  resolved = resolved.normalize();
  
  // 3. Kiểm tra xem đường dẫn chuẩn hóa có còn nằm trong storageRoot không
  if (!resolved.startsWith(storageRoot)) {
      throw new SecurityException("PATH TRAVERSAL DETECTED!");
  }
  ```

---

### BƯỚC 3: FILE UPLOAD (STOR) & STREAM TRANSFER

#### 3.1 TCP Byte Stream & Chuyển đổi File thành Byte
TCP không biết file là gì. Nó chỉ biết truyền một dòng liên tục các byte nhị phân (`0` và `1`).
- **Phía gửi (Client):** `FileInputStream` đọc từng byte từ đĩa cứng local $\to$ đẩy vào Socket `OutputStream`.
- **Phía nhận (Server):** Socket `InputStream` nhận từng byte $\to$ `FileOutputStream` ghi trực tiếp xuống đĩa cứng Server.

#### 3.2 Buffer 4096 bytes là gì? Tại sao không load cả file vào RAM?
- **Buffer (Vùng đệm):** Một mảng byte tạm thời (ở đây chọn kích thước 4096 bytes = 4KB, kích thước tiêu chuẩn của 1 sector ổ đĩa).
- **Tại sao không dùng `byte[] allBytes = new byte[(int) fileSize]`?**
  Nếu file nặng 1GB hoặc 2GB, việc tạo một mảng byte như vậy sẽ chiếm sạch bộ nhớ RAM của JVM, gây lỗi sập Server ngay lập tức: `java.lang.OutOfMemoryError: Java heap space`.
- **Dùng Buffer 4KB:** Dù file 10MB hay 10GB, bộ nhớ RAM sử dụng luôn luôn cố định chỉ tốn **4KB**. Đọc 4KB $\to$ Ghi 4KB $\to$ Tái sử dụng mảng $\to$ Lặp lại cho đến hết.

#### 3.3 Làm sao Server biết khi nào nhận đủ bytes?
Trong kiến trúc chỉ dùng 1 kết nối TCP (Single Connection):
- Sau khi nhận file, kết nối vẫn phải giữ mở để nhận các lệnh tiếp theo (`LIST`, `QUIT`...).
- Server **KHÔNG THỂ** chờ đến khi gặp `-1` (EOF) được, vì `-1` chỉ xuất hiện khi Client đóng Socket!
- **Giải pháp:** Giao thức bắt buộc trao đổi trước kích thước:
  1. Client gửi `STOR filename`
  2. Server phản hồi `125 Ready`
  3. Client gửi `SIZE <số_bytes>` (ví dụ: `SIZE 159`)
  4. Server dùng vòng lặp đếm ngược: `remaining = fileSize`. Mỗi lần đọc được `n` byte thì `remaining -= n`. Khi `remaining == 0` thì dừng đọc!

#### 3.4 Xử lý thế nào nếu đứt kết nối giữa chừng?
Nếu Client bị ngắt mạng khi mới tải được 50%:
File trên đĩa Server sẽ bị lỗi (hỏng/dở dang). Trong `StorageManager.java`:
```java
if (bytesRead == -1) { // Kết nối bị đóng sớm trước khi nhận đủ bytes
    fos.close();
    Files.deleteIfExists(targetPath); // Xóa ngay lập tức file dở dang
    return "426 Connection closed; transfer aborted. Partial file deleted.";
}
```

---

## ⚡ 2. VẤN ĐỀ BUFFER-AHEAD & PHÁT MINH `ProtocolInputStream`

Đây là bài học kinh nghiệm lập trình socket cốt lõi nhất của Phase 5.

### Vấn đề "Buffer-Ahead" là gì?
Lớp `BufferedReader` thông thường đọc trước (buffer ahead) một khối 8192 bytes (8KB) từ Socket vào mảng nội bộ của nó nhằm tăng tốc độ đọc text.
Khi trộn lệnh Text (`SIZE 159\n`) và Binary Data (Nội dung file) trên cùng Socket:

```
Dữ liệu gửi từ Client qua Socket:
[S][I][Z][E][ ][1][5][9][\n][Byte_0][Byte_1]...[Byte_158]

Nếu dùng BufferedReader.readLine():
- Nó đọc chuỗi "SIZE 159"
- ĐỒNG THỜI nó tiện tay nuốt luôn [Byte_0] đến [Byte_158] vào bộ nhớ đệm ẩn của nó!

Khi Server gọi rawSocketStream.read():
- Con trỏ Socket đã cạn vì BufferedReader đã đọc mất rồi!
- Kết quả: File ghi xuống đĩa bị mất sạch các byte đầu hoặc bị treo (hang) vĩnh viễn!
```

### Giải pháp: `ProtocolInputStream.java`
Đọc từng byte một qua `in.read()` đến đúng ký tự `\n` thì dừng ngay lập tức, không lưu trữ đệm thừa một byte nào:
```java
public String readLine() throws IOException {
    StringBuilder sb = new StringBuilder();
    int byteRead;
    while ((byteRead = in.read()) != -1) {
        if (byteRead == '\n') return sb.toString();
        if (byteRead == '\r') continue; // Bỏ qua ký tự \r của Windows
        sb.append((char) byteRead);
    }
    return null;
}
```
Sau lệnh này, byte dữ liệu đầu tiên của file vẫn nằm nguyên vẹn 100% trong socket, sẵn sàng cho `getRawInputStream()` đọc.

---

## 🔍 3. CHI TIẾT CÁC FILE MÃ NGUỒN (CODE DETAILS)

---

### 3.1 [`ProtocolInputStream.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/ProtocolInputStream.java)
- **Nhiệm vụ:** Bộ đọc dữ liệu tầng protocol an toàn, không buffer-ahead.
- **Code cốt lõi:**
  - `readLine()`: Đọc text đến `\n` từng byte một.
  - `getRawInputStream()`: Cung cấp trực tiếp `InputStream` gốc để đọc file nhị phân.

---

### 3.2 [`StorageManager.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/StorageManager.java)
- **Nhiệm vụ:** Trung tâm thao tác File/Folder & Bảo mật.
- **Code cốt lõi:**
  1. `resolveSafe(String currentDir, String clientPath)`:
     - Nối đường dẫn, chạy `normalize()`, xác thực `startsWith(storageRoot)`. Chặn đứng mọi mưu toan hack `..`.
  2. `listDirectory(String currentDir)`:
     - Lấy danh sách file qua `dir.toFile().listFiles()`.
     - Sắp xếp (Thư mục lên trước, file theo sau, theo bảng chữ cái).
     - Định dạng đầu ra: `<DIR>` hoặc kích thước byte, thời gian sửa đổi, tên file.
     - Trả về mã bắt đầu `150` và kết thúc `226`.
  3. `receiveFile(String currentDir, String filename, long fileSize, InputStream rawIn)`:
     - Kiểm tra tính hợp lệ của filename (cấm `/`, `\`, `:`).
     - Mở `FileOutputStream`.
     - Vòng lặp `while (remaining > 0)` dùng buffer 4KB để nhận đủ số byte.
     - Tự động xóa file hỏng nếu mất kết nối giữa chừng (`Files.deleteIfExists(targetPath)`).

---

### 3.3 [`CommandHandler.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/CommandHandler.java)
- **Nhiệm vụ:** Parser & Router xử lý lệnh.
- **Code cốt lõi:**
  - `handle(String rawInput)`:
    - Bóc tách lệnh qua `rawInput.split("\\s+", 2)`.
    - Kiểm tra đăng nhập (nếu chưa login chỉ cho phép `USER`, `PASS`, `QUIT`).
    - Lệnh `PWD`: Trả về thư mục đang đứng kèm mã `257`.
    - Lệnh `LIST`: Gọi `storageManager.listDirectory()`.
    - Các lệnh dự trù Phase 6 (`RETR`, `DELE`, `RNFR`, `RNTO`): Trả về `550 Not implemented yet`.
  - `handleStor(String filename, ProtocolInputStream lineReader, PrintWriter writer)`:
    - Gửi thông báo sẵn sàng `125 Ready for '<filename>'. Send: SIZE <bytes>`.
    - Đọc dòng `SIZE <n>` qua `lineReader.readLine()`.
    - Lấy `lineReader.getRawInputStream()` chuyển giao cho `storageManager.receiveFile()`.

---

### 3.4 [`ClientHandler.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/ClientHandler.java)
- **Nhiệm vụ:** Quản lý vòng đời kết nối của 1 Client trong Thread Pool.
- **Code cốt lõi:**
  - Sử dụng `ProtocolInputStream lineReader = new ProtocolInputStream(socket.getInputStream())`.
  - Vòng lặp nhận diện lệnh:
    ```java
    if (commandLine.trim().toUpperCase().startsWith("STOR")) {
        // Tách filename và chuyển hướng sang luồng Stream riêng biệt
        response = handler.handleStor(filename, lineReader, writer);
    } else {
        // Lệnh chuỗi text thông thường
        response = handler.handle(commandLine);
    }
    ```

---

### 3.5 [`Session.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/Session.java)
- **Nhiệm vụ:** Lưu ngữ cảnh làm việc riêng cho từng Client.
- **Bổ sung trong Phase 5:**
  - Thêm thuộc tính `private String currentDirectory = "";`
  - Cung cấp `getCurrentDirectory()` và `setCurrentDirectory()`.
  - Tự động reset `currentDirectory = ""` khi người dùng `LOGOUT`.

---

### 3.6 [`TCPServer.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/TCPServer.java)
- **Nhiệm vụ:** Bootstrap máy chủ.
- **Thay đổi Phase 5:**
  - Khởi tạo thực thể `StorageManager storageManager = new StorageManager()` duy nhất khi Server start.
  - Truyền `storageManager` vào constructor của từng `ClientHandler`.

---

### 3.7 [`TCPClient.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/client/TCPClient.java)
- **Nhiệm vụ:** Chương trình Client hoàn chỉnh cho người dùng tương tác.
- **Code cốt lõi:**
  - `readResponse(BufferedReader reader)`: Tự động nhận diện response nhiều dòng (bắt đầu bằng `150` và dừng lại khi thấy dòng có mã `226`).
  - `handleUpload(...)`:
    1. Kiểm tra file có tồn tại trên máy client không.
    2. Gửi lệnh `STOR <filename>`.
    3. Đợi mã `125`.
    4. Gửi dòng `SIZE <filesize>`.
    5. Mở `FileInputStream` đọc đĩa và bơm từng chunk 4096 bytes vào socket `OutputStream`.
    6. Hiển thị thanh tiến độ: `Tiến độ: x/y bytes (z%)`.
    7. Đợi nhận phản hồi hoàn tất `226`.

---

## 🔄 4. SƠ ĐỒ LUỒNG CHUYỂN DỮ LIỆU (DATA FLOW DIAGRAMS)

### 4.1 Luồng Upload File (STOR) End-to-End

```
CLIENT (TCPClient)                                  SERVER (ClientHandler / StorageManager)
      │                                                                  │
      ├─────── (1) Gửi text: "STOR test.txt\n" ─────────────────────────►│
      │                                                                  │ Phân tích thấy "STOR"
      │                                                                  │ Gọi handler.handleStor()
      │◄────── (2) Trả text: "125 Ready for 'test.txt'. Send: SIZE..." ──┤
      │                                                                  │
      ├─────── (3) Gửi text: "SIZE 159\n" ──────────────────────────────►│ Đọc qua ProtocolInputStream
      │                                                                  │ Lấy kích thước = 159 bytes
      │                                                                  │ Mở FileOutputStream("storage/test.txt")
      │                                                                  │
      ├─────── (4) Bơm 159 raw bytes nhị phân từ file local ────────────►│ Vòng lặp đọc socket qua buffer 4KB
      │            (ghi trực tiếp ra socket.getOutputStream())          │ Ghi thẳng vào FileOutputStream
      │                                                                  │ Nhận đủ 159 bytes -> fos.close()
      │                                                                  │
      │◄────── (5) Trả text: "226 Transfer complete. File saved..." ─────┤
      ▼                                                                  ▼
```

### 4.2 Luồng Liệt Kê Thư Mục (LIST) Multi-line

```
CLIENT                                              SERVER
  │                                                    │
  ├─── Gửi: "LIST\n" ─────────────────────────────────►│ CommandHandler: gọi listDirectory()
  │                                                    │ StorageManager: đọc File[] từ storageRoot
  │◄── 150 Directory listing for /: ───────────────────┤ Đọc dòng 1 (phát hiện 150 -> chờ multi-line)
  │◄──   <DIR>              -  2026-09-20 14:00  docs ─┤ Đọc dòng tiếp theo
  │◄──          159 bytes  2026-09-20 15:30  a.txt ────┤ Đọc dòng tiếp theo
  │◄── 226 Directory listing complete. 2 item(s). ─────┤ Thấy mã 226 -> Kết thúc multi-line!
  ▼                                                    ▼
```

---

## 🧪 5. KỊCH BẢN KIỂM THỬ (TEST CASES)

### Biên dịch toàn bộ Project
```powershell
javac -d out src\server\ProtocolInputStream.java src\server\StorageManager.java src\server\Session.java src\server\UserDatabase.java src\server\CommandHandler.java src\server\ClientHandler.java src\server\TCPServer.java src\client\TCPClient.java
```

### Khởi động Server
```powershell
java -cp out server.TCPServer
```

### Khởi động Client
```powershell
java -cp out client.TCPClient
```

---

### Bảng 9 Kịch Bản Test Chi Tiết

| STT | Tình huống | Thao tác nhập tại Client | Phản hồi mong đợi từ Server |
|---|---|---|---|
| **TC1** | Dùng lệnh khi chưa Login | `PWD` hoặc `LIST` | `530 Not logged in` |
| **TC2** | Đăng nhập chuẩn FTP | `USER admin`<br>`PASS 123` | `331 Username accepted`<br>`230 Login successful` |
| **TC3** | Xem thư mục hiện tại | `PWD` | `257 "/" is current directory` |
| **TC4** | Xem danh sách file | `LIST` | `150 Directory listing for /:`<br>`  (danh sách files)`<br>`226 Directory listing complete.` |
| **TC5** | Upload file hợp lệ | `STOR test_upload.txt` | `125 Ready...`<br>`SIZE 159`<br>`[Client] Tiến độ: 100%`<br>`226 Transfer complete.` |
| **TC6** | Upload file không tồn tại | `STOR khong_co_file.abc` | `[Client] LỖI: File không tồn tại` (bị chặn tại Client) |
| **TC7** | Tấn công Path Traversal | Gửi lệnh thô: `STOR ../hacked.txt` | `553 Invalid filename: contains illegal characters` |
| **TC8** | Lệnh chưa hỗ trợ | `RETR test.txt` hoặc `DELE test.txt` | `550 RETR not implemented yet (Phase 6)` |
| **TC9** | Đăng xuất và Thoát | `LOGOUT`<br>`QUIT` | `230 Goodbye...`<br>`221 Goodbye` (Đóng socket) |

---

## ❓ 6. CÂU HỎI BẢO VỆ ĐỒ ÁN (VIVA QUESTIONS & ANSWERS)

### 🟢 Câu 1: Tại sao Server Phase 5 không dùng `BufferedReader` cho `ClientHandler` nữa?
> **Trả lời:** Vì `BufferedReader` có cơ chế đọc đón đầu (buffer-ahead khoảng 8KB). Khi Client gửi dòng chữ `SIZE 159\n` kèm theo các byte nhị phân của file, `BufferedReader` sẽ nuốt luôn một phần hoặc toàn bộ các byte của file vào bộ đệm của nó. Khi ta lấy `InputStream` gốc ra để đọc file thì dữ liệu đã bị mất. Do đó, ta thay bằng `ProtocolInputStream` để đọc text từng byte một, không đọc lấn sang byte của file.

### 🟢 Câu 2: Trình bày cơ chế chống tấn công Path Traversal trong code?
> **Trả lời:** Được thực hiện ở hàm `resolveSafe()` trong `StorageManager.java`. Ta lấy đường dẫn tương đối của Client ghép với thư mục gốc `storageRoot`, sau đó gọi hàm `.normalize()` để triệt tiêu toàn bộ các ký tự `..`. Cuối cùng kiểm tra bằng `.startsWith(storageRoot)`. Nếu đường dẫn sau khi chuẩn hóa không bắt đầu bằng thư mục `storageRoot`, chương trình sẽ ném ngoại lệ `SecurityException` và từ chối truy cập với mã lỗi `550 Access denied`.

### 🟡 Câu 3: Buffer 4096 bytes hoạt động như thế nào khi upload file 10MB?
> **Trả lời:** Buffer 4096 bytes là mảng `byte[4096]`. Khi Client truyền file 10MB, Server không cấp phát 10MB RAM mà chỉ dùng đúng 4KB này trong vòng lặp `while`. Mỗi lần lặp, nó đọc tối đa 4096 bytes từ socket và ghi ngay lập tức xuống đĩa qua `FileOutputStream`. Vòng lặp chạy khoảng 2500 lần là nhận trọn vẹn file mà chỉ tốn 4KB bộ nhớ heap.

### 🟡 Câu 4: Phản hồi của lệnh `LIST` khác gì so với các lệnh như `PWD` hay `PING`?
> **Trả lời:** `PWD` và `PING` là phản hồi đơn dòng (Single-line response, kết thúc bằng 1 ký tự xuống dòng). Ngược lại, `LIST` là phản hồi nhiều dòng (Multi-line response), mở đầu bằng mã `150`, in ra danh sách các file (mỗi file một dòng) và kết thúc bằng một dòng chứa mã `226`. Client phải có hàm `readResponse()` chuyên biệt để đọc cho đến khi gặp dòng có mã `226`.

### 🔴 Câu 5: Nếu hai Client cùng lúc upload file có tên trùng nhau (`data.txt`) thì điều gì sẽ xảy ra?
> **Trả lời:** Trong mã nguồn hiện tại, cả 2 luồng `ClientHandler` sẽ cùng mở `FileOutputStream` trỏ tới cùng một file `storage/data.txt`. Hai luồng sẽ ghi đè đan xen các byte dữ liệu lên nhau (Race Condition), dẫn đến file trên đĩa bị hỏng (corrupted). Để khắc phục ở các phase nâng cao, ta có thể dùng khóa đồng bộ file (`FileLock`), lưu theo định danh tạm (`UUID`), hoặc phân chia mỗi người dùng vào một thư mục con riêng biệt (`storage/admin/`, `storage/alice/`).

### 🔴 Câu 6: Làm thế nào Server biết khi nào file upload đã truyền xong mà không cần đóng Socket?
> **Trả lời:** Trước khi truyền dữ liệu thô, Client phải gửi dòng lệnh `SIZE <số_bytes>`. Server bóc tách số này và lưu vào biến đếm `remaining`. Mỗi lần đọc được một lượng `bytesRead`, Server trừ dần `remaining -= bytesRead`. Khi `remaining == 0`, Server biết đã nhận trọn vẹn file và dừng đọc, tiếp tục giữ Socket để đón nhận các lệnh tiếp theo.

---

## 🗺️ 7. TIẾN ĐỘ TOÀN BỘ DỰ ÁN (ROADMAP)

| Phase | Nội dung chính | Trạng thái |
|---|---|:---:|
| **Phase 1** | TCP Socket cơ bản, kết nối đơn luồng | ✅ Hoàn thành |
| **Phase 2** | Giao thức Command - Response ứng dụng (`PING`, `ECHO`, `QUIT`) | ✅ Hoàn thành |
| **Phase 3** | Multi-Client đồng thời với Thread Pool (`ExecutorService`) | ✅ Hoàn thành |
| **Phase 4** | Quản lý Session & Xác thực người dùng (`USER`, `PASS`, `LOGOUT`) | ✅ Hoàn thành |
| **Phase 5** | **FTP Commands lõi, Quản lý Thư mục, Upload File, Chống Path Traversal** | ✅ **Hoàn thành** |
| **Phase 6** | File Download (`RETR`), Xóa file (`DELE`), Đổi tên file (`RNFR`/`RNTO`) | ⏳ Sắp tới |
| **Phase 7** | Giao diện đồ họa người dùng (GUI Client JavaFX/Swing) | ⏳ Tương lai |

---
*Tài liệu hướng dẫn & giải thích kiến trúc mã nguồn Phase 5 · FTP Project · Lập Trình Mạng*
