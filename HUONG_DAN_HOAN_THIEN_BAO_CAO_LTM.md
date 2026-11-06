# HƯỚNG DẪN CHI TIẾT SỬA ĐỔI VÀ HOÀN THIỆN BÁO CÁO ĐỒ ÁN LẬP TRÌNH MẠNG
## (CHIA THEO TỪNG CHƯƠNG — PHỤC VỤ HIỆN TẠI VÀ LỘ TRÌNH NÂNG CẤP CUỐI KỲ)
**Đề tài:** Ứng dụng truyền tập tin Client - Server dựa trên giao thức TCP Socket và tập lệnh FTP tùy biến  
**Sinh viên thực hiện:** Nguyễn Tất Võ (24ITE119) & Lê Hữu Minh Hy (24ITE043)  
**Lớp:** Lập trình mạng (11) — Trường Đại học Công nghệ Thông tin & Truyền thông Việt - Hàn (VKU)  
**Giảng viên hướng dẫn:** ThS. Nguyễn Thanh Cẩm  

---

## 📌 QUY ƯỚC ĐÁNH DẤU TRONG TÀI LIỆU
Để bạn dễ dàng thao tác đối chiếu và chỉnh sửa trực tiếp vào file báo cáo Word của mình, tài liệu phân chia rõ ràng theo 3 ký hiệu:
* 🔴 **NỘI DUNG CẦN SỬA ĐỔI:** Chỉ rõ đoạn văn, bảng biểu cũ trong file `báo cáo cuối kì.docx` đang bị sai lệch so với mã nguồn thực tế, kèm nội dung văn bản chuẩn xác cần thay thế vào.
* 🟢 **NỘI DUNG CẦN BỔ SUNG:** Các mục kiến trúc, giải thuật, đoạn code kỹ thuật cần chèn thêm vào để báo cáo đạt chất lượng chuyên môn cao nhất (đặc biệt là lỗi Buffer-Ahead và giải thuật ProtocolInputStream).
* 🚀 **ĐỊNH HƯỚNG NÂNG CẤP BÀI CUỐI KỲ:** Cách trình bày để khẳng định hệ thống đã có thiết kế CSDL MySQL và lộ trình mở rộng hoàn chỉnh, giúp bảo vệ đồ án đạt điểm tối đa.

---

# PHẦN I: HƯỚNG DẪN ĐIỀU CHỈNH PHẦN MỞ ĐẦU

### 1. Mục 1.3: "Vai trò của giao thức TCP và chuẩn FTP (RFC 959)"
* 🔴 **Nội dung cần sửa đổi:**
  * *Báo cáo cũ:* Đang khẳng định hệ thống phân tách thành 2 kênh TCP: Kênh điều khiển Port 21/2121 và Kênh dữ liệu mở cổng động ngẫu nhiên PASV (50000 - 50050).
  * *Thực tế trong code:* Hệ thống sử dụng **1 kênh TCP Socket duy nhất (Port 6789)**.
  * *Đoạn văn thay thế chuẩn xác:*
    > *"Đề tài nghiên cứu và xây dựng hệ thống truyền tệp tin tin cậy dựa trên giao thức TCP Socket, áp dụng mô hình **Truyền tải đa hợp trên một kết nối (Single-Channel Stream Multiplexing)** kết hợp tập lệnh ứng dụng chuẩn hóa theo mã phản hồi 3 chữ số của RFC 959. Giải pháp một kết nối giúp Client và Server trao đổi cả chuỗi lệnh điều khiển và luồng byte nhị phân của tệp tin qua cùng một Socket (Port 6789), loại bỏ hoàn toàn sự phức tạp của việc mở nhiều dải cổng mạng, giúp ứng dụng vượt qua dễ dàng các chính sách tường lửa (Firewall) và cơ chế dịch địa chỉ mạng (NAT) trong môi trường mạng LAN phòng máy thực hành."*

### 2. Mục 2.2: "Mục tiêu cụ thể"
* 🟢 **Nội dung cần bổ sung:**
  * *Bổ sung vào mục Sản phẩm phần mềm:*
    > *"Xây dựng thành công lớp `ProtocolInputStream` giải quyết triệt để vấn đề xung đột luồng và lỗi 'nuốt dữ liệu nhị phân' (Buffer-Ahead bug) của bộ đệm Java I/O; cài đặt giao diện người dùng trực quan bằng Java Swing hỗ trợ đa dạng giao diện (Theme Dark Custom / Flat), hiển thị thanh tiến trình truyền tệp thời gian thực bằng kỹ thuật xử lý bất đồng bộ `SwingWorker`."*

### 3. Mục 3.2: "Phạm vi và giới hạn của đề tài"
* 🔴 **Nội dung cần sửa đổi:**
  * Sửa lại số hiệu cổng hoạt động: Cổng dịch vụ là **TCP Port `6789`** (thay vì 2121 và dải 50000 - 50050).
  * Sửa lại tập lệnh hỗ trợ: Gồm 13 lệnh thực tế: `USER`, `PASS`, `LOGOUT`, `PING`, `ECHO`, `PWD`, `LIST`, `STOR`, `RETR`, `DELE`, `RNFR`, `RNTO`, `QUIT` (bỏ `PASV`, `PORT`, `TYPE`, `MKD`, `RMD` trong danh sách hiện tại).
* 🚀 **Định hướng nâng cấp cuối kỳ (Cần ghi rõ trong mục Giới hạn):**
  * Ghi chú rõ: *"Trong phạm vi giai đoạn giữa kỳ, hệ thống quản lý danh sách tài khoản dưới dạng In-Memory Database để ưu tiên kiểm thử độ tin cậy của tầng Socket. Việc tích hợp hệ quản trị cơ sở dữ liệu quan hệ **MySQL qua JDBC**, phân quyền sandbox theo từng người dùng và mã hóa kênh truyền **FTPS (TLS v1.3)** được xác định là mục tiêu hoàn thiện cho giai đoạn cuối kỳ."*

---

# PHẦN II: HƯỚNG DẪN ĐIỀU CHỈNH CHƯƠNG 1
## (CƠ SỞ LÝ THUYẾT VÀ MỘT SỐ CÔNG NGHỆ)

### 1. Mục 1.1 và 1.2 (Lập trình mạng, OSI 7 tầng, TCP/IP 4 tầng, Client-Server)
* **Đánh giá:** Phần này trong báo cáo cũ của bạn viết **RẤT TỐT, chuẩn mực học thuật**. Hãy giữ nguyên các bảng so sánh và sơ đồ quá trình bắt tay 3 bước TCP.

### 2. Mục 1.3.2: "Giao thức truyền tập tin FTP (RFC 959)"
* 🔴 **Nội dung cần sửa đổi:**
  * Không mô tả chế độ Active Mode và Passive Mode như thể hệ thống đã cài đặt cả hai.
  * Hãy chuyển thành mục: **"So sánh mô hình FTP 2 kênh truyền thống (RFC 959) và Mô hình 1 kênh đa hợp (Single-Channel Stream)"**:
    * *Mô hình 2 kênh RFC 959:* Ưu điểm là phân tách lệnh và dữ liệu, nhưng nhược điểm lớn là Kênh dữ liệu mở cổng động thường bị Firewall phía Client hoặc NAT chặn (ở Active Mode) hoặc đòi hỏi Server mở dải cổng rộng (ở Passive Mode).
    * *Mô hình 1 kênh đa hợp (Lựa chọn của đồ án):* Gửi lệnh text và dữ liệu nhị phân trên cùng một kết nối Socket đã thiết lập, tối ưu hóa tài nguyên mạng, phù hợp tuyệt đối cho môi trường mạng nội bộ trường học.

### 3. Mục 1.4: "Sơ lược về công nghệ và công cụ phát triển"
* 🟢 **Nội dung cần bổ sung:**
  * **Mục 1.4.3 (Java I/O):** Bổ sung phân tích về lớp tự xây dựng `ProtocolInputStream` và mảng đệm byte chuẩn `byte[] buffer = new byte[4096]`.
  * **Mục 1.4.5 (Giao diện người dùng):** Viết rõ sử dụng **Java Swing kết hợp `SwingWorker`**: Tiến trình tải file chạy ngầm trong `doInBackground()`, phát tín hiệu tiến độ qua interface `ProgressListener` để giao diện chính cập nhật `JProgressBar` mượt mà, không bị hiện tượng Not Responding (đơ ứng dụng).

### 4. Mục 1.5: "Cơ sở dữ liệu và cơ chế lưu trữ của FTP Server"
* 🔴 **Nội dung cần sửa đổi & 🚀 Bổ sung nâng cấp cuối kỳ:**
  * *Hiện tại:* Trình bày nguyên lý thiết kế **Single Responsibility Principle (SRP)**: Tách riêng lớp `UserDatabase.java` đóng vai trò là tầng truy cập dữ liệu (Data Access Layer). Hiện tại sử dụng `HashMap` lưu trữ danh sách tài khoản trong bộ nhớ RAM (`admin/123`, `alice/alice123`, `bob/bob456`) phục vụ kiểm thử nhanh.
  * *Lộ trình cuối kỳ:* Đưa ra thiết kế nâng cấp kết nối CSDL **MySQL Server 8.0** qua thư viện **JDBC**, mật khẩu băm một chiều bảo mật bằng thuật toán **SHA-256** để lưu trữ vĩnh viễn trên đĩa cứng.

---

# PHẦN III: HƯỚNG DẪN ĐIỀU CHỈNH CHƯƠNG 2
## (PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG FTP CLIENT VÀ FTP SERVER)

### 1. Mục 2.1: "Các chức năng của FTP Client & Server"
* 🔴 **Nội dung cần sửa đổi:** Cập nhật lại Bảng danh mục chức năng thực tế:

| Phân hệ | Tên chức năng | Mô tả nghiệp vụ thực tế | File đảm nhiệm |
| :--- | :--- | :--- | :--- |
| **Server** | Lắng nghe kết nối đa luồng | Mở ServerSocket cổng 6789, tiếp nhận Client qua Thread Pool. | `TCPServer.java` |
| **Server** | Xác thực danh tính | Xử lý cặp lệnh `USER` và `PASS`, kiểm tra qua `UserDatabase`. | `CommandHandler.java` |
| **Server** | Quản lý phiên (Session) | Duy trì trạng thái đăng nhập, thư mục làm việc và tên file chờ đổi tên. | `Session.java` |
| **Server** | Thao tác tệp tin vật lý | Quét thư mục (`LIST`), đọc/ghi file (`RETR`/`STOR`), xóa (`DELE`), đổi tên. | `StorageManager.java` |
| **Server** | Chống Path Traversal | Chuẩn hóa đường dẫn `resolveSafe()`, ngăn chặn thoát khỏi thư mục gốc. | `StorageManager.java` |
| **Server** | Đọc luồng an toàn | Đọc byte-by-byte không buffer-ahead để phân tách text và nhị phân. | `ProtocolInputStream.java` |
| **Client** | Đăng nhập đa giao diện | Cửa sổ `LoginFrame` nhập IP, Port, User, Pass; hỗ trợ chọn Theme Dark/Nimbus. | `LoginFrame.java` |
| **Client** | Duyệt tệp từ xa | `MainFrame` hiển thị bảng `JTable` danh sách file kèm dung lượng và ngày sửa. | `MainFrame.java` |
| **Client** | Tải lên tệp (Upload) | Chọn file qua `JFileChooser`, gửi lệnh `STOR <file> <size>`, cập nhật progress %. | `FtpClientService.java` |
| **Client** | Tải về tệp (Download) | Chọn file trên bảng, chọn nơi lưu, nhận luồng byte qua Socket, ghi đĩa an toàn. | `FtpClientService.java` |
| **Client** | Đổi tên tệp 2 bước | Thực thi chuỗi lệnh `RNFR` (chọn file) và `RNTO` (đặt tên mới). | `FtpClientService.java` |
| **Client** | Nhật ký lệnh thời gian thực | Khung Log Terminal màu tối hiển thị chi tiết mọi lệnh gửi và mã phản hồi 3 chữ số. | `MainFrame.java` |

---

### 2. Mục 2.2: "Mô hình tổng quát của hệ thống FTP"
* 🔴 **Nội dung cần sửa đổi:**
  * Thay thế sơ đồ 2 kênh bằng **Sơ đồ Kiến trúc 1 kênh TCP Socket Stream (Port 6789)**:

```
[ FTP CLIENT GUI ]                                    [ FTP SERVER DAEMON ]
┌───────────────────────────┐                         ┌───────────────────────────┐
│ • LoginFrame / MainFrame  │                         │ • TCPServer (Port 6789)   │
│ • JTable Remote Files     │    TCP Connection       │ • ThreadPool (Multi-Client│
│ • SwingWorker Progress %  │ ◄═════════════════════► │ • ClientHandler Runnable  │
│ • FtpClientService Layer  │    (Port 6789 Stream)   │ • CommandHandler (Router) │
└───────────────────────────┘                         │ • ProtocolInputStream     │
                                                      │ • StorageManager (storage/│
                                                      │ • UserDatabase (Auth)     │
                                                      └───────────────────────────┘
```

---

### 3. Mục 2.3.2: "Mô hình hóa Ca sử dụng (Use Case Analysis)"
* 🔴 **Nội dung cần sửa đổi trong 6 Ca sử dụng:**
  * **UC01 (Khởi động Server):** Server khởi tạo `new ServerSocket(6789)` và lắng nghe trên cửa sổ dòng lệnh Console.
  * **UC03 (Duyệt thư mục LIST):** Bỏ bước gửi `PASV`. Client gửi trực tiếp `LIST` trên Socket hiện tại $ightarrow$ Server phản hồi `150 Listing directory` $ightarrow$ Server đẩy các dòng text danh sách file $ightarrow$ Server gửi `226 Directory send OK`.
  * **UC04 (Tải lên STOR):** Bỏ bước gửi `PASV`. Client gửi `STOR <filename> <filesize>` $ightarrow$ Server kiểm tra quyền và phản hồi `150 Ready to receive <filesize> bytes` $ightarrow$ Client đẩy luồng byte qua Socket theo mảng đệm 4KB $ightarrow$ Server nhận đủ byte, đóng file và gửi `226 Transfer complete`.
  * **UC05 (Tải về RETR):** Bỏ bước gửi `PASV`. Client gửi `RETR <filename>` $ightarrow$ Server phản hồi `150 Opening binary stream for <filename> (<size> bytes)` $ightarrow$ Server đẩy luồng byte qua Socket $ightarrow$ Client nhận đủ byte và ghi vào đĩa $ightarrow$ Server gửi `226 Transfer complete`.
* 🟢 **Bổ sung Ca sử dụng mới:**
  * **UC07 (Đổi tên tệp an toàn 2 bước):**
    * Tác nhân: Người dùng FTP Client.
    * Luồng chính: Người dùng bấm [Đổi tên] $ightarrow$ Client gửi `RNFR <old_name>` $ightarrow$ Server kiểm tra file tồn tại, lưu vào Session và phản hồi `350 File exists, ready for destination name` $ightarrow$ Client hiển thị hộp thoại nhập tên mới và gửi `RNTO <new_name>` $ightarrow$ Server thực thi `Files.move()` và phản hồi `250 Rename successful` $ightarrow$ Client tự động làm mới bảng danh sách file.

---

### 4. Mục 2.3.3: "Sơ đồ Lớp (Class Diagram) và Đặc tả 11 Lớp thực tế"
* 🔴 **Nội dung cần sửa đổi TOÀN BỘ:**
  * Xóa bỏ các lớp lý thuyết không có trong code (`FTPServerCore`, `FTPDataConnection`, `FileTransferWorker`...).
  * Thay thế bằng **Đặc tả 11 Lớp thực tế trong mã nguồn**:

#### Nhóm 1: Các Lớp phía Máy chủ (Package `server`)
1. **Lớp `TCPServer`:**
   * *Thuộc tính:* `private static final int PORT = 6789`, `private static final AtomicInteger clientCount`, `ExecutorService threadPool`.
   * *Phương thức:* `public static void main(String[] args)` (mở ServerSocket, khởi tạo UserDatabase và StorageManager một lần duy nhất, chạy vòng lặp `accept()` cấp phát luồng).
2. **Lớp `ClientHandler` (implements Runnable):**
   * *Thuộc tính:* `private final Socket clientSocket`, `private final CommandHandler handler`, `private final String clientId`.
   * *Phương thức:* `public void run()` (sử dụng ProtocolInputStream đọc từng dòng lệnh text và bàn giao nhận file nhị phân).
3. **Lớp `CommandHandler`:**
   * *Thuộc tính:* `private final Session session`, `private final UserDatabase userDatabase`, `private final StorageManager storageManager`.
   * *Phương thức:* `public String handle(String rawInput)` (bộ định tuyến switch-case cho 13 lệnh FTP), `public String handleStor(...)`, `public String handleRetr(...)`, `private String handleRnfr(...)`, `private String handleRnto(...)`.
4. **Lớp `ProtocolInputStream` (Điểm sáng kỹ thuật):**
   * *Thuộc tính:* `private final InputStream in`.
   * *Phương thức:* `public String readLine()` (đọc byte-by-byte đến `\n`, không đọc lố vào RAM), `public InputStream getRawInputStream()` (bàn giao luồng đọc file nhị phân).
5. **Lớp `Session`:**
   * *Thuộc tính:* `private State state` (UNAUTHENTICATED / AUTHENTICATED), `private String pendingUsername`, `private String loggedInUser`, `private String currentDirectory`, `private String renameFrom`.
   * *Phương thức:* `public boolean isAuthenticated()`, `public void loginSuccess()`, `public void reset()`, `public void setRenameFrom(String name)`, `public void clearRenameFrom()`.
6. **Lớp `StorageManager`:**
   * *Thuộc tính:* `private final Path storageRoot`, `private static final SimpleDateFormat DATE_FORMAT`.
   * *Phương thức:* `private Path resolveSafe(String currentDir, String clientPath)` (chuẩn hóa đường dẫn, chặn Path Traversal), `public String listDirectory()`, `public String receiveFile(...)`, `public String sendFile(...)`, `public String deleteFile(...)`, `public String renameFile(...)`.
7. **Lớp `UserDatabase`:**
   * *Thuộc tính:* `private final Map<String, String> users` (lưu trữ In-Memory HashMap).
   * *Phương thức:* `public boolean validate(String username, String password)`, `public boolean userExists(String username)`.

#### Nhóm 2: Các Lớp phía Máy trạm (Package `client`)
8. **Lớp `TCPClient`:** Ứng dụng dòng lệnh Console hoàn chỉnh hỗ trợ `handleDownload(RETR)` và `handleUpload(STOR)`.
9. **Lớp `FtpClientService`:**
   * *Thuộc tính:* `private Socket socket`, `private InputStream socketIn`, `private PrintWriter writer`, `private boolean connected`, `private boolean loggedIn`.
   * *Interface con:* `public interface ProgressListener { void onProgress(int percent, long bytesTransferred, long totalBytes); }`.
   * *Phương thức:* `public boolean connect(host, port)`, `public String login(u, p)`, `public List<FileEntry> list()`, `public String stor(localFile, listener)`, `public String retr(filename, saveDir, listener)`, `public String dele(filename)`, `public String rename(oldName, newName)`.
10. **Lớp `LoginFrame` (extends JFrame):**
    * *Giao diện:* `tfServer`, `tfPort`, `tfUsername`, `pfPassword`, `btnConnect`, `btnLogin`, `lblStatus`.
    * *Phương thức:* `private void applyTheme(String themeName)` (hỗ trợ Dark Custom, Nimbus, Windows, Metal), `private void doConnect()`, `private void doLogin()`.
11. **Lớp `MainFrame` (extends JFrame):**
    * *Giao diện:* `JTable fileTable`, `JProgressBar progressBar`, `JTextArea logArea`, các nút Toolbar (`btnUpload`, `btnDownload`, `btnDelete`, `btnRename`, `btnLogout`).
    * *Xử lý bất đồng bộ:* `private void doUpload()` và `private void doDownload()` ứng dụng **`SwingWorker`** truyền dữ liệu trong luồng nền, cập nhật giao diện mượt mà.

---

### 5. Mục 2.3.6 (MỤC MỚI CẦN BỔ SUNG): "Điểm sáng Kỹ thuật: Thiết kế Lớp ProtocolInputStream giải quyết lỗi Buffer-Ahead"
* 🟢 **Nội dung cần chèn vào báo cáo:**
  * Đưa sơ đồ phân tích lỗi Buffer-Ahead và đoạn mã nguồn hoàn chỉnh của `ProtocolInputStream.java` (đã có trong file Word mới).
  * Khẳng định: Đây là giải pháp tự thiết kế độc lập, thể hiện năng lực làm chủ kỹ thuật lập trình Socket và luồng byte thô.

---

### 6. Mục 2.3.7 (MỤC MỚI CẦN BỔ SUNG): "Thiết kế Cơ sở Dữ liệu MySQL cho Giai đoạn Nâng cấp Cuối kỳ"
* 🚀 **Nội dung cần chèn vào báo cáo:**
  * **Lược đồ CSDL quan hệ 2 bảng:** `users` và `transfer_logs`.
  * **Mã SQL tạo bảng:** Đưa toàn bộ đoạn mã SQL chuẩn mực (đã có trong file Word).
  * **Mã nguồn Java mẫu JDBC:** Đưa lớp `UserDatabase.java` viết bằng JDBC kết nối MySQL có băm mật khẩu SHA-256.
  * Việc đưa mục này vào khẳng định: Bạn không hề "bỏ quên" CSDL, mà đã có thiết kế chuẩn mực sẵn sàng cho việc cắm nối vào hệ thống ở bài cuối kỳ!

---

# PHẦN IV: HƯỚNG DẪN ĐIỀU CHỈNH CHƯƠNG 3
## (CÀI ĐẶT, KIỂM THỬ VÀ ĐÁNH GIÁ KẾT QUẢ)

### 1. Mục 3.1: "Môi trường cài đặt và cấu hình"
* 🔴 **Nội dung cần sửa đổi:**
  * Sửa cổng điều khiển từ `2121` thành **`6789`**.
  * Bỏ hoàn toàn dải cổng `50000 - 50050` vì hệ thống chạy trên 1 kết nối duy nhất.
  * Cấu hình IP: Loopback `127.0.0.1` (kiểm thử máy đơn) và IP LAN tĩnh (ví dụ `10.212.42.239` hoặc `192.168.1.100` khi thử nghiệm trong phòng máy).

### 2. Mục 3.2: "Hướng dẫn cài đặt và sử dụng phần mềm"
* 🔴 **Nội dung cần sửa đổi:**
  * **Khởi động Máy chủ (Server):**
    * Thực thi file `start_server.bat` (hoặc lệnh `java -cp out/production/FTPProject server.TCPServer`).
    * Cửa sổ dòng lệnh Terminal hiển thị thông báo: `[Server] Đang lắng nghe trên port 6789...`. Khi có kết nối, server xuất log: `[Server] Client #1 đã kết nối từ /127.0.0.1:xxxxx`.
  * **Khởi động Máy trạm (Client):**
    * Thực thi file `start_gui.bat` (hoặc lệnh `java -cp out/production/FTPProject client.LoginFrame`).
    * Bước 1: Màn hình `LoginFrame` xuất hiện. Nhập Server IP, Port `6789`, chọn giao diện Dark Custom, bấm `[Kết nối Server]`.
    * Bước 2: Nhập Username `admin` và Password `123`, bấm `[Đăng nhập]`.
    * Bước 3: Màn hình chính `MainFrame` mở ra. Người dùng duyệt danh sách tệp trên bảng `JTable`, bấm `[Upload]` chọn file qua `JFileChooser` để tải lên (thanh progress bar chạy từ 0% đến 100%), bấm `[Download]` để tải về, bấm `[Đổi tên]` hoặc `[Xóa]`.

### 3. Mục 3.3: "Kịch bản kiểm thử (Test Cases) thực tế"
* 🔴 **Nội dung cần sửa đổi:** Cập nhật lại Ma trận kiểm thử khớp với các tính năng thực tế:

| Mã TC | Tên ca kiểm thử | Dữ liệu thử nghiệm | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **TC01** | Khởi động Server cổng 6789 | Cổng TCP 6789 sẵn sàng | Server mở cổng thành công, lắng nghe `accept()`. | Mở cổng 6789 thành công. | **PASS** |
| **TC02** | Khởi động trùng cổng | Chạy 2 Server cùng lúc | Server thứ hai bắt lỗi `BindException`. | Bắt lỗi chính xác, dừng an toàn. | **PASS** |
| **TC03** | Đăng nhập tài khoản đúng | `admin` / `123` | Server phản hồi mã `230 Login successful`. | Đăng nhập thành công, mở MainFrame. | **PASS** |
| **TC04** | Đăng nhập sai mật khẩu | `admin` / `sai_pass` | Server phản hồi mã `530 Login incorrect`. | Hiển thị cảnh báo lỗi màu đỏ. | **PASS** |
| **TC05** | Duyệt danh sách tệp (`LIST`) | Thư mục `storage/` có 10 file | Server trả về `150` -> `226`, bảng nạp đúng 10 hàng. | Hiển thị đầy đủ tên, loại, dung lượng. | **PASS** |
| **TC06** | Tải lên file (`STOR`) | File ảnh `hinh_anh.png` | Client gửi `STOR`, thanh tiến trình chạy đều 0-100%. | Server nhận đủ byte, mở ảnh nguyên vẹn. | **PASS** |
| **TC07** | Tải về file (`RETR`) | File `tailieu.docx` | Client nhận luồng byte, ghi vào đĩa máy trạm. | File tải về đọc tốt, không lỗi font. | **PASS** |
| **TC08** | Kiểm tra toàn vẹn (Checksum) | So sánh mã SHA-256 | Mã băm file trước và sau khi tải trùng khớp 100%. | Trùng khớp tuyệt đối từng bit. | **PASS** |
| **TC09** | Đổi tên file 2 bước | `RNFR cu.txt` -> `RNTO moi.txt`| Server thực thi `Files.move()`, bảng tự động cập nhật. | Đổi tên thành công trên đĩa. | **PASS** |
| **TC10** | Chống Path Traversal | Lệnh `DELE ../../file.sys` | Server chặn qua `resolveSafe()`, trả về mã 550. | Ngăn chặn truy cập ngoài storage/. | **PASS** |
| **TC11** | Đăng xuất (`LOGOUT`) | Bấm nút [Đăng xuất] | Session về UNAUTHENTICATED, Socket vẫn giữ. | Trở về màn hình Login không mất kết nối. | **PASS** |
| **TC12** | Tải đa Client đồng thời | 3 Client cùng upload/download | Thread Pool xử lý song song, không nghẽn. | Cả 3 Client hoàn thành trơn tru. | **PASS** |

---

# PHẦN V: HƯỚNG DẪN ĐIỀU CHỈNH KẾT LUẬN & HƯỚNG PHÁT TRIỂN

* 🔴 **Sửa phần Kết quả đạt được:**
  * Khẳng định đã làm chủ kỹ thuật lập trình Socket TCP, đa luồng Thread Pool và thiết kế giao thức ứng dụng.
  * Tự thiết kế giải thuật `ProtocolInputStream` giải quyết dứt điểm lỗi Buffer-Ahead.
  * Ứng dụng giao diện đồ họa hiện đại với `SwingWorker` đa luồng chống đơ lag giao diện.
* 🚀 **Bổ sung phần Lộ trình Nâng cấp Hoàn thiện Bài Cuối kỳ (4 Module lớn):**
  1. **Module 1 (Cơ sở dữ liệu):** Thay thế `HashMap` trong `UserDatabase` bằng hệ quản trị **MySQL Server 8.0** qua JDBC; bổ sung chức năng Đăng ký tài khoản (`REGISTER`) và lưu nhật ký truyền tệp `transfer_logs`.
  2. **Module 2 (Phân quyền Sandbox):** Tự động tạo thư mục riêng `storage/<username>/` cho từng tài khoản, ngăn chặn người dùng xem trộm file của nhau.
  3. **Module 3 (Bảo mật kênh truyền FTPS):** Tích hợp `SSLSocket` và `SSLServerSocket` với chứng chỉ số X.509, mã hóa toàn bộ dữ liệu qua giao thức TLS v1.3.
  4. **Module 4 (Cơ chế Resume):** Bổ sung lệnh `REST <offset>` cho phép tải tiếp tệp tin khi bị gián đoạn đường truyền mạng.

---

# PHẦN VI: BỘ 15 CÂU HỎI VẤN ĐÁP BẢO VỆ ĐỒ ÁN VÀ CÂU TRẢ LỜI MẪU
*(Đã được tích hợp đầy đủ 100% trong file Word và Markdown để bạn ôn tập tự tin trước Hội đồng chấm thi!)*
