# PHASE 6 — FTP Download + File Management + Error Handling + Logging
## FTP Project · Lập Trình Mạng

> **Mục tiêu Phase 6:**
> 1. **Download File (`RETR`):** Truyền file từ Server về máy Client bằng cơ chế Stream và Buffer 4096 bytes, loại bỏ triệt để lỗi Buffer-Ahead ở cả 2 đầu.
> 2. **File Management (`DELE`, `RNFR`, `RNTO`):** Hoàn thiện các thao tác quản trị file cơ bản: xóa file và đổi tên file an toàn 2 bước.
> 3. **Error Handling (Xử lý lỗi toàn diện):** Phân loại và bao quát 5 nhóm lỗi (Client, Server, Network, Authentication, Security) với mã trạng thái FTP chuẩn (RFC 959).
> 4. **Structured Logging (Ghi nhật ký hệ thống):** Thiết kế format log chuẩn mực giúp quản trị viên giám sát mọi hành vi và chẩn đoán sự cố Server.
> 5. **Test Matrix & Câu hỏi bảo vệ đồ án:** Bảng ma trận 15 ca kiểm thử và câu hỏi vấn đáp chuyên sâu.

---

## 🗺️ BẢN ĐỒ PHÂN BỐ CODE TRONG PHASE 6

Bảng tra cứu nhiệm vụ của từng file trong dự án khi tích hợp các tính năng Phase 6:

```
FTPProject/
├── src/
│   ├── server/
│   │   ├── TCPServer.java           ← Bootstrap Server, quản lý Thread Pool, khởi tạo Storage & Logger
│   │   ├── ClientHandler.java       ← Nhận diện và bóc tách luồng Text vs Stream (STOR/RETR), ghi Log
│   │   ├── CommandHandler.java      ← Parser & Router: điều hướng DELE, RNFR, RNTO, handleRetr, handleStor
│   │   ├── StorageManager.java      ← Thao tác I/O đĩa: sendFile (RETR), deleteFile, renameFile, chống Path Traversal
│   │   ├── Session.java             ← Lưu trạng thái login, currentDirectory và trường renameFrom cho RNFR/RNTO
│   │   ├── ProtocolInputStream.java ← Đọc text byte-by-byte trên Server, không nuốt byte nhị phân
│   │   └── UserDatabase.java        ← Danh sách tài khoản xác thực
│   └── client/
│       └── TCPClient.java           ← Client Console: handleDownload (RETR), handleUpload (STOR), đọc text an toàn
├── storage/                         ← Thư mục Server quản lý file
├── downloads/                       ← Thư mục Client lưu trữ các file tải về qua lệnh RETR
└── phase6.md                        ← File tổng hợp kiến thức toàn diện này
```

| Chức năng / Xử lý | File đảm nhiệm | Method cốt lõi |
|---|---|---|
| **Download file (`RETR`) phía Server** | [`StorageManager.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/StorageManager.java) | `sendFile(currentDir, filename, rawOut, writer)` |
| **Download file (`RETR`) phía Client** | [`TCPClient.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/client/TCPClient.java) | `handleDownload(remoteFilename, ...)` |
| **Xóa file (`DELE`)** | [`StorageManager.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/StorageManager.java) | `deleteFile(currentDir, filename)` |
| **Đổi tên file bước 1 (`RNFR`)** | [`Session.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/Session.java) | `setRenameFrom(filename)` |
| **Đổi tên file bước 2 (`RNTO`)** | [`StorageManager.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/StorageManager.java) | `renameFile(currentDir, oldName, newName)` |
| **Loại bỏ Buffer-Ahead trên Client** | [`TCPClient.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/client/TCPClient.java) | `readLine(InputStream in)` đọc từng byte đến `\n` |
| **Xử lý đóng phiên (`QUIT`)** | [`CommandHandler.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/CommandHandler.java) | `handleQuit()`, `isQuitResponse()` |
| **Bảo vệ an toàn chống Path Traversal** | [`StorageManager.java`](file:///d:/Lap_Trinh_Mang/FTPProject/src/server/StorageManager.java) | `resolveSafe(currentDir, clientPath)` |

---

## 📚 1. LÝ THUYẾT BƯỚC 1 — DOWNLOAD / RETR

### 1.1 Upload và Download khác nhau thế nào?
- **Upload (`STOR`):** Chiều truyền từ **Client $\to$ Server**. Client là nguồn cung cấp dữ liệu, Server là đích lưu trữ.
- **Download (`RETR`):** Chiều truyền từ **Server $\to$ Client**. Server là nguồn đọc dữ liệu, Client là đích ghi nhận dữ liệu.

### 1.2 Trong Upload, bên nào đọc file? Bên nào ghi file?
- **Client đọc file:** Dùng `FileInputStream` đọc từ ổ cứng của máy Client.
- **Server ghi file:** Nhận các byte từ Socket và dùng `FileOutputStream` ghi xuống ổ cứng của Server.

### 1.3 Trong Download, bên nào đọc file? Bên nào ghi file?
- **Server đọc file:** Dùng `FileInputStream` đọc file từ thư mục `storage/`.
- **Client ghi file:** Nhận các byte từ Socket và dùng `FileOutputStream` ghi vào thư mục local `downloads/`.

### 1.4 InputStream và OutputStream bố trí ở bên nào trong Download?
- **Tại Server:**
  - `FileInputStream`: Đọc các byte từ file trên đĩa cứng.
  - Socket `OutputStream`: Đẩy các byte dữ liệu đó lên kết nối mạng gửi cho Client.
- **Tại Client:**
  - Socket `InputStream`: Hút các byte nhị phân từ mạng về.
  - `FileOutputStream`: Ghi các byte đó vào file đĩa cứng local.

### 1.5 Buffer 4096 bytes hoạt động như thế nào?
- Buffer là mảng byte tạm thời: `byte[] buffer = new byte[4096]` (kích thước 4KB).
- **Cơ chế:** Phía gửi đọc 1 chunk tối đa 4096 bytes từ đĩa $\to$ ghi ra Socket. Phía nhận đọc tối đa 4096 bytes từ Socket $\to$ ghi xuống đĩa.
- Vòng lặp tuần hoàn tái sử dụng mảng 4KB này cho đến khi hết file. Dù file có kích thước 1GB hay 5GB, bộ nhớ RAM tiêu thụ luôn là hằng số **4KB**, ngăn ngừa hoàn toàn lỗi sập bộ nhớ `OutOfMemoryError`.

### 1.6 File Size dùng để làm gì?
Vì hệ thống chạy trên **1 kết nối TCP duy nhất (Single Connection)** cho cả Text Command và Binary Stream:
- Sau khi download xong, kết nối TCP vẫn phải duy trì để Client gõ các lệnh kế tiếp.
- Bên nhận **không thể dựa vào tín hiệu đóng kết nối (`-1` / EOF)** để kết thúc nhận file.
- Do đó, Server phải gửi trước kích thước file: `150 Opening binary mode... (SIZE <bytes>)`. Bên nhận sẽ đọc đúng ngần ấy byte rồi lập tức dừng đọc binary để quay lại chế độ đọc text.

### 1.7 Làm thế nào biết đã nhận đủ file?
Bên nhận thiết lập biến đếm ngược: `long remaining = fileSize`.
- Mỗi lần gọi `read(buffer)`, nhận được `bytesRead` byte $\to$ cập nhật: `remaining -= bytesRead`.
- Khi `remaining == 0`, bên nhận biết chắc chắn 100% dữ liệu file đã được thu thập trọn vẹn.

### 1.8 Điều gì xảy ra nếu mất kết nối giữa chừng?
- Nếu mạng bị đứt hoặc Client/Server tắt ngang khi `remaining > 0`:
  - Socket sẽ trả về `-1` hoặc ném `SocketException`.
  - Bên nhận ngay lập tức đóng luồng ghi đĩa và kích hoạt cơ chế dọn dẹp: gọi `file.delete()` để **xóa ngay file dở dang (partial file)**, không để lại file hỏng trên đĩa, đồng thời báo lỗi mã `426 Connection closed; transfer aborted`.

### 1.9 Làm thế nào kiểm tra file nhận được có đúng với file gốc?
1. **So sánh kích thước (Byte Length):** `originalFile.length() == downloadedFile.length()`.
2. **Kiểm tra Checksum (Mã băm MD5 / SHA-256):** Tính hàm băm mật mã của 2 file. Nếu hai chuỗi hash trùng khớp nhau, file được bảo đảm toàn vẹn từng bit dữ liệu.
3. **So sánh trực tiếp (File Compare):** Dùng công cụ so sánh byte có sẵn của hệ điều hành:
   ```powershell
   fc.exe storage\test_upload.txt downloads\test_upload.txt
   ```

---

## 🗂️ 2. LÝ THUYẾT BƯỚC 2 — FILE MANAGEMENT (`DELE`, `RNFR`, `RNTO`, `QUIT`)

### 2.1 Lệnh Xóa File (`DELE`)
- **Cú pháp:** `DELE <filename>`
- **Nghiệp vụ Server:**
  1. Kiểm tra đăng nhập (chưa login $\to$ `530 Not logged in`).
  2. Kiểm tra tên file hợp lệ (cấm chứa `/`, `\`, `:`, `..`).
  3. Dùng `resolveSafe()` kiểm tra quyền trong `storage/`.
  4. Kiểm tra file có tồn tại và không phải là thư mục không.
  5. Gọi `Files.deleteIfExists(targetPath)`.
  6. Thành công $\to$ `250 File deleted successfully`. Thất bại $\to$ `550 Delete failed`.

### 2.2 Lệnh Đổi Tên File 2 Bước (`RNFR` và `RNTO`)
Tại sao FTP chuẩn lại tách việc đổi tên thành 2 command riêng biệt thay vì dùng một lệnh đơn kiểu `RENAME old new`?

```
Client                              Server
  │                                    │
  ├─── (1) RNFR old.txt ──────────────►│ Kiểm tra: old.txt có tồn tại không?
  │                                    │ Có -> Lưu "old.txt" vào Session!
  │◄── 350 Ready for RNTO ─────────────┤ Trả mã 350 (Intermediate)
  │                                    │
  ├─── (2) RNTO new.txt ──────────────►│ Kiểm tra: Session có file chờ đổi không?
  │                                    │ Kiểm tra: new.txt đã bị trùng chưa?
  │                                    │ Tiến hành Files.move() và xóa Session chờ
  │◄── 250 Rename successful ──────────┤
```

#### Lý do thiết kế 2 bước trong chuẩn RFC 959:
1. **Tính nguyên tử và kiểm tra trung gian:** Bước 1 (`RNFR` - Rename From) xác thực file gốc có tồn tại và có quyền sửa đổi hay không. Nếu file gốc không tồn tại, Server báo lỗi `550 File not found` ngay, không mất công chờ đợi tên mới.
2. **Tránh lỗi phân tách tham số phức tạp:** Nếu gộp chung `RENAME "file cu co dau cach.txt" "file moi.txt"`, việc parser chuỗi chứa khoảng trắng hoặc dấu ngoặc kép trên đường truyền socket rất dễ phát sinh lỗi cú pháp. Tách thành 2 lệnh độc lập giúp truyền tên file dài, chứa khoảng trắng một cách an toàn và chuẩn xác tuyệt đối.
3. **Mã trạng thái chuẩn:**
   - `RNFR` thành công trả về mã **`350`** (Mã 3xx nghĩa là *cần gửi lệnh tiếp theo để hoàn thành thao tác*).
   - Nếu Client gõ `RNTO` mà trước đó chưa gõ `RNFR` thành công, Server trả về mã **`503 Bad sequence of commands`**.

### 2.3 Lệnh Đóng Kết Nối (`QUIT`)
- **Cú pháp:** `QUIT`
- **Nghiệp vụ:** Server phản hồi mã `221 Goodbye`, sau đó thoát khỏi vòng lặp `while`, đóng Socket và thu hồi tài nguyên luồng trong Thread Pool.

---

## 🛡️ 3. LÝ THUYẾT BƯỚC 3 — HỆ THỐNG XỬ LÝ LỖI (ERROR HANDLING)

### 3.1 Bảng Phân Loại 5 Nhóm Lỗi

```
HỆ THỐNG LỖI FTP
├── A. Client Error         (Lỗi do người dùng nhập sai cú pháp, thiếu tham số)
├── B. Server Error         (Lỗi từ tài nguyên đĩa, file không tìm thấy, trùng tên)
├── C. Network Error        (Lỗi đường truyền socket, rớt mạng, connection reset)
├── D. Authentication Error (Lỗi xác thực danh tính, sai pass, chưa login)
└── E. Security Error       (Lỗi tấn công vượt quyền, Path Traversal)
```

| Nhóm lỗi | Tình huống thực tế | Mã FTP chuẩn | Thông điệp phản hồi |
|---|---|:---:|---|
| **A. Client Error** | Gõ lệnh không hỗ trợ | `500` | `500 Unknown command` |
| | Thiếu tham số (gõ `USER`, `STOR`) | `501` | `501 Syntax error in parameters` |
| | Tên file chứa ký tự cấm (`:`, `/`) | `553` | `553 Invalid filename: contains illegal characters` |
| **B. Server Error** | File cần tải/xóa không có | `550` | `550 File not found: <filename>` |
| | Đổi tên trùng file đã có | `550` | `550 File already exists: <filename>` |
| | Ổ đĩa bị khóa/hết dung lượng | `451` | `451 Requested action aborted: local error in processing` |
| **C. Network Error** | Mất kết nối khi đang truyền byte | `426` | `426 Connection closed; transfer aborted` |
| | Client ngắt đột ngột | Server Log | Đóng socket, thu hồi Thread, không crash Server |
| **D. Auth Error** | Chưa login mà dùng lệnh | `530` | `530 Not logged in` |
| | Sai mật khẩu | `530` | `530 Login incorrect` |
| | Gõ `PASS` trước `USER` hoặc `RNTO` trước `RNFR` | `503` | `503 Bad sequence of commands` |
| **E. Security Error** | Dùng `../../` để ra ngoài storage | `550` | `550 Access denied` |

---

### 3.2 Các Khái Niệm Ngoại Lệ Cốt Lõi

#### 1. `IOException` là gì?
Là lớp ngoại lệ cha của mọi lỗi I/O (Input/Output) trong Java, xuất hiện khi có sự cố khi đọc/ghi file từ đĩa cứng hoặc truyền/nhận dữ liệu qua Socket mạng.

#### 2. `SocketException` là gì?
Là lớp con của `IOException`, phát sinh chuyên biệt khi có sự cố xảy ra ở tầng giao vận Socket (ví dụ: cổng mạng bị đóng, card mạng mất kết nối, socket timeout).

#### 3. "Connection reset" là gì?
Là hiện tượng một bên gửi gói tin TCP `RST` (Reset) để đơn phương hủy kết nối ngay lập tức mà không qua quy trình bắt tay 4 bước đóng kết nối chuẩn (`FIN-ACK`). Xảy ra khi:
- Tiến trình Client bị tắt đột ngột (Tắt cửa sổ Terminal, nhấn `Ctrl + C`, hoặc bị Task Manager End Task).
- Client gửi dữ liệu đến một cổng đã bị Server đóng.

#### 4. Vì sao Client ngắt kết nối lại khiến Server phát sinh Exception?
Khi Client đóng cửa sổ, hệ điều hành Client đóng Socket. Server đang ở trạng thái chờ đọc dữ liệu (`read()`). Khi không nhận được dữ liệu mà kết nối đã mất, lời gọi hàm `read()` trên Server sẽ ném ra ngoại lệ `SocketException: Connection reset` hoặc trả về `-1`. Đây là **hành vi hoàn toàn bình thường** trong lập trình mạng, Server cần bắt ngoại lệ này ở khối `catch` để đóng Socket và kết thúc Thread một cách êm đẹp, tránh làm sập toàn bộ ứng dụng.

#### 5. Tại sao tuyệt đối KHÔNG ĐƯỢC dùng try-catch để che giấu lỗi rỗng?
```java
// ❌ CỰC KỲ NGUY HIỂM: Bắt lỗi nhưng bỏ qua, không ghi log
try {
    fos.write(buffer, 0, bytesRead);
} catch (Exception e) {
    // Để trống -> File ghi thiếu byte nhưng lập trình viên không hề hay biết!
}
```
Che giấu lỗi (Empty catch block) khiến:
- Hệ thống rơi vào trạng thái lỗi ngầm (Silent Failure).
- File bị hỏng nhưng Server vẫn báo "Thành công".
- Khi xảy ra sự cố trên môi trường thực tế, quản trị viên hoàn toàn bất lực vì không có bất kỳ dấu vết lỗi nào được ghi lại.

#### 6. Lỗi nào nên gửi về Client? Lỗi nào chỉ nên ghi vào Server Log?
- **Gửi về cho Client:** Các thông báo lỗi nghiệp vụ mà Client có thể hiểu và sửa đổi hành vi (ví dụ: `530 Sai password`, `550 File không tồn tại`, `501 Thiếu tham số`).
- **Chỉ ghi vào Server Log:** Chi tiết kỹ thuật nội bộ nhạy cảm (Đường dẫn tuyệt đối trên ổ cứng Server `D:\FTPProject\storage`, Stack trace chi tiết của Exception, thông tin cấu hình máy chủ). Nếu gửi chi tiết này cho Client, kẻ tấn công có thể lợi dụng để thăm dò cấu trúc hệ thống.

---

## 📊 4. LÝ THUYẾT BƯỚC 4 — HỆ THỐNG GHI NHẬT KÝ (LOGGING)

### 4.1 Tại sao Server cần Logging?
1. **Giám sát thời gian thực:** Quản trị viên biết ai đang kết nối, từ địa chỉ IP nào, làm hành động gì.
2. **Truy vết sự cố (Auditing & Debugging):** Khi khách hàng báo "Tôi không upload được file", xem lại log sẽ thấy chính xác nguyên nhân (do ổ đĩa đầy, do sai tên file hay do mất mạng lúc 10:15).
3. **Bảo mật:** Phát hiện các đợt quét brute-force mật khẩu hoặc các nỗ lực tấn công Path Traversal lặp đi lặp lại từ một địa chỉ IP đáng ngờ.

### 4.2 Logging khác gì so với `System.out.println()` thông thường?
| Đặc điểm | `System.out.println()` | Hệ thống Logging chuẩn |
|---|---|---|
| **Đích đến** | Chỉ in tạm thời ra màn hình Console | Ghi ra file lưu trữ lâu dài (`server.log`), dễ sao lưu |
| **Cấu trúc** | Chuỗi tự do, lộn xộn, khó phân tích | Có format cố định, dễ dùng regex/Elasticsearch để lọc |
| **Thông tin ngữ cảnh** | Thường thiếu thời gian, IP, User | Luôn kèm Timestamp, IP, Client ID, Username, Command |
| **Mức độ (Level)** | Không có | Phân cấp rõ ràng: `INFO`, `WARN`, `ERROR` |

### 4.3 Định dạng Log Chuẩn Được Áp Dụng
Mỗi dòng log của Server được chuẩn hóa theo cấu trúc sau:
```text
[HH:mm:ss] IP=<client_ip> USER=<username> CMD=<command> FILE=<filename> RESULT=<SUCCESS/ERROR> [MSG=<details>]
```

**Ví dụ thực tế:**
```text
[17:20:15] IP=127.0.0.1 USER=admin CMD=STOR FILE=data.pdf RESULT=SUCCESS MSG=Saved 102400 bytes
[17:22:04] IP=127.0.0.1 USER=admin CMD=RETR FILE=secret.doc RESULT=ERROR MSG=File not found
[17:25:30] IP=127.0.0.1 USER=guest CMD=DELE FILE=../../config.sys RESULT=ERROR MSG=Path traversal detected
```

---

## 📋 5. MA TRẬN KIỂM THỬ TOÀN DIỆN (TEST MATRIX 15 TEST CASES)

Bảng ma trận kiểm thử chuẩn hóa phục vụ nghiệm thu và bảo vệ đồ án:

| Test # | Tên ca kiểm thử | Lệnh nhập tại Client | Phản hồi mong đợi (Expected) | Ý nghĩa nghiệp vụ |
|:---:|---|---|---|---|
| **TC1** | Đăng nhập đúng | `USER admin`<br>`PASS 123` | `331 Username accepted`<br>`230 Login successful` | Xác thực danh tính người dùng thành công |
| **TC2** | Đăng nhập sai pass | `USER admin`<br>`PASS sai_pass` | `331 Username accepted`<br>`530 Login incorrect` | Từ chối truy cập và reset session |
| **TC3** | Chặn khi chưa login | `LIST` hoặc `PWD` | `530 Not logged in` | Kiểm soát bảo vệ tài nguyên |
| **TC4** | Liệt kê thư mục | `LIST` | `150 Directory listing...`<br>`226 Directory listing complete` | Trả về danh sách file dạng nhiều dòng |
| **TC5** | Upload file | `STOR test_upload.txt` | `125 Ready...`<br>`SIZE 159`<br>`226 Transfer complete` | Đẩy file nhị phân từ Client lên Server |
| **TC6** | Download file | `RETR test_upload.txt` | `150 Opening binary... (SIZE 159)`<br>`226 Transfer complete` | Tải file nhị phân từ Server về `downloads/` |
| **TC7** | Tải file không có | `RETR khong_ton_tai.txt` | `550 File not found: khong_ton_tai.txt` | Báo lỗi tài nguyên không tồn tại |
| **TC8** | Xóa file | `DELE test_upload.txt` | `250 File deleted successfully` | Xóa file an toàn trên Server |
| **TC9** | Xóa file không có | `DELE file_ao.txt` | `550 File not found: file_ao.txt` | Chặn xóa tài nguyên không tồn tại |
| **TC10** | Đổi tên bước 1 (RNFR) | `RNFR README.txt` | `350 File exists, ready for destination name` | Ghi nhận file nguồn vào Session |
| **TC11** | Đổi tên bước 2 (RNTO) | `RNTO HUONG_DAN.txt` | `250 Rename successful` | Đổi tên thành công trên đĩa |
| **TC12** | Đổi tên sai trình tự | Gõ `RNTO moi.txt` (chưa gõ `RNFR`) | `503 Bad sequence of commands` | Bắt buộc tuân thủ thứ tự lệnh |
| **TC13** | Tấn công Path Traversal| `RETR ../secret.txt` hoặc `DELE ../` | `553 Invalid filename` hoặc `550 Access denied` | Chặn kẻ tấn công thoát khỏi `storage/` |
| **TC14** | Client ngắt đột ngột | Đóng Terminal Client (Alt+F4) | Server ghi log disconnect, xóa file rác, Server không sập | Khả năng chống chịu lỗi kết nối mạng |
| **TC15** | Đóng phiên làm việc | `QUIT` | `221 Goodbye` | Đóng kết nối lịch sự, thu hồi Thread |

---

## ❓ 6. BỘ CÂU HỎI BẢO VỆ ĐỒ ÁN (VIVA EXAM QUESTIONS & ANSWERS)

### 🟢 Câu 1: Tại sao trong lệnh `RETR`, Client cũng phải đọc dòng text bằng cơ chế byte-by-byte thay vì dùng `BufferedReader`?
> **Trả lời:** Khi Server phản hồi dòng `150 Opening binary mode... (SIZE 159)\n` rồi bơm ngay các byte nhị phân của file vào Socket, nếu Client dùng `BufferedReader.readLine()`, bộ đệm 8KB của nó sẽ đọc trước và vô tình "nuốt" luôn các byte đầu tiên của file. Khi Client gọi `socket.getInputStream().read()`, các byte đó đã biến mất khỏi stream mạng, dẫn đến file tải về bị hỏng. Do đó, hàm `readLine(socketIn)` đọc từng byte đến `\n` là giải pháp bảo đảm 100% các byte nhị phân tiếp theo vẫn còn nguyên vẹn trong Socket.

### 🟢 Câu 2: Trình bày sự khác biệt giữa `QUIT` và `LOGOUT`?
> **Trả lời:** 
> - `LOGOUT`: Hủy bỏ trạng thái xác thực (`session.reset()`), đưa trạng thái về `UNAUTHENTICATED`. Kết nối TCP Socket **vẫn được giữ mở**, người dùng có thể gõ `USER` khác để đăng nhập lại mà không cần kết nối lại từ đầu.
> - `QUIT`: Server gửi lời chào `221 Goodbye`, sau đó Server và Client **chủ động đóng hoàn toàn kết nối Socket**, giải phóng Thread trong Thread Pool.

### 🟡 Câu 3: Tại sao FTP lại chia thao tác đổi tên thành 2 lệnh `RNFR` và `RNTO` thay vì 1 lệnh đơn?
> **Trả lời:** Có 2 lý do chính: (1) Tính tin cậy: `RNFR` giúp kiểm tra sự tồn tại và tính hợp lệ của file nguồn trước khi yêu cầu tên đích. (2) Tránh lỗi bóc tách chuỗi khi tên file chứa khoảng trắng hoặc ký tự đặc biệt nếu truyền chung trên một dòng lệnh. Việc tách đôi giúp xử lý tên file dài và phức tạp một cách an toàn và nhất quán theo chuẩn RFC 959.

### 🟡 Câu 4: Khi đang truyền file dung lượng lớn mà kết nối mạng bị đứt, hệ thống làm thế nào để không để lại "file rác" trên ổ cứng?
> **Trả lời:** Bên nhận duy trì một biến đếm số bytes cần nhận `remaining`. Trong khối `while (remaining > 0)`, nếu hàm `read()` trả về `-1` (tín hiệu ngắt kết nối bất thường), bên nhận sẽ lập tức đóng `FileOutputStream` và gọi `Files.deleteIfExists(targetPath)` để xóa ngay file dở dang khỏi đĩa, đồng thời phản hồi mã lỗi `426 Connection closed; transfer aborted`.

### 🔴 Câu 5: Hệ thống của bạn xử lý thế nào để phân biệt giữa "Lệnh đọc Text bình thường" và "Lệnh truyền tải Dữ liệu nhị phân (Binary Stream)" trên cùng 1 kết nối Socket?
> **Trả lời:** Trong `ClientHandler`, ta sử dụng `ProtocolInputStream` để đọc từng dòng lệnh. Khi phát hiện các lệnh cần truyền stream như `STOR` hay `RETR`, `ClientHandler` tạm thời ngưng cơ chế xử lý text đơn giản và chuyển giao `InputStream`/`OutputStream` gốc của Socket cho các hàm chuyên biệt (`handleStor`, `handleRetr`). Các hàm này đọc/ghi đúng số lượng `fileSize` bytes đã thỏa thuận trước, sau đó trả lại luồng để quay lại vòng lặp đọc text command bình thường.

### 🔴 Câu 6: Nếu người dùng gửi lệnh `DELE ../../Windows/win.ini`, cơ chế nào ngăn chặn hành vi phá hoại này?
> **Trả lời:** Lớp phòng thủ 2 tầng: (1) Tầng tiền kiểm tra chuỗi (Pre-validation): Nếu chuỗi chứa `..`, `:`, `/`, `\` thì lập tức bị từ chối với mã `553 Invalid filename`. (2) Tầng chuẩn hóa đường dẫn: Hàm `resolveSafe()` sử dụng `storageRoot.resolve(clientPath).normalize()`. Nếu kết quả sau chuẩn hóa không bắt đầu bằng tiền tố `storageRoot` (`!resolved.startsWith(storageRoot)`), một ngoại lệ `SecurityException` sẽ được kích hoạt, trả về mã lỗi `550 Access denied`.

---

## 🗺️ 7. TIẾN ĐỘ TOÀN BỘ DỰ ÁN (ROADMAP)

| Phase | Nội dung thực hiện | Trạng thái |
|---|---|:---:|
| **Phase 1** | TCP Socket cơ bản, Single-Client | ✅ Hoàn thành |
| **Phase 2** | Giao thức Command-Response (`PING`, `ECHO`, `QUIT`) | ✅ Hoàn thành |
| **Phase 3** | Multi-Client đồng thời với `ExecutorService` (Thread Pool) | ✅ Hoàn thành |
| **Phase 4** | Quản lý Session & Xác thực người dùng (`USER`, `PASS`, `LOGOUT`) | ✅ Hoàn thành |
| **Phase 5** | Thao tác Thư mục (`PWD`, `LIST`) & Upload File (`STOR`) | ✅ Hoàn thành |
| **Phase 6** | **Download File (`RETR`), Quản lý File (`DELE`, `RNFR`/`RNTO`), Error Handling, Logging** | 🚀 **Đang triển khai** |
| **Phase 7** | Giao diện đồ họa người dùng (GUI Client JavaFX / Swing) | ⏳ Sắp tới |

---
*Tài liệu tổng hợp kiến thức & hướng dẫn kỹ thuật Phase 6 · FTP Project · Lập Trình Mạng*
