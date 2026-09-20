package server;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Comparator;

/**
 * PHASE 5 - Storage Manager
 *
 * Quản lý tất cả thao tác file/directory cho FTP Server.
 * Đảm bảo Client KHÔNG THỂ truy cập ra ngoài storage/.
 *
 * Nguyên tắc bảo mật cốt lõi — PATH TRAVERSAL PREVENTION:
 *   Mọi path do Client cung cấp đều phải được:
 *     1. Resolve thành absolute path
 *     2. Normalize (xử lý "..", ".", "//", v.v.)
 *     3. Kiểm tra: path đã normalize có bắt đầu bằng storageRoot không?
 *   Nếu không → từ chối, trả 550 Access denied.
 *
 * Ví dụ path traversal bị chặn:
 *   storageRoot = D:\FTPProject\storage
 *   clientPath  = "../../Windows/secret"
 *   resolve →  D:\FTPProject\storage\..\..\Windows\secret
 *   normalize → D:\Windows\secret
 *   startsWith(storageRoot)? NO → 550 Access denied!
 */
public class StorageManager {

    // Absolute path đến thư mục storage — KHÔNG BAO GIỜ thay đổi
    private final Path storageRoot;

    // Định dạng hiển thị thời gian trong LIST
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * Constructor: xác định storageRoot và tạo thư mục nếu chưa có.
     *
     * @throws IOException nếu không tạo được thư mục storage
     */
    public StorageManager() throws IOException {
        // "storage" là relative path → resolve thành absolute
        // Paths.get("storage") = thư mục "storage" trong thư mục chạy JVM
        this.storageRoot = Paths.get("storage").toAbsolutePath().normalize();

        // Tạo thư mục storage (và các parent nếu thiếu)
        Files.createDirectories(storageRoot);

        System.out.println("[StorageManager] Root: " + storageRoot);
    }

    // ================================================================
    // CORE: Path Resolution với bảo mật chống Path Traversal
    // ================================================================

    /**
     * Chuyển đổi path của Client thành Path an toàn trên server.
     *
     * Quy trình:
     *   1. Nối currentDir + clientPath
     *   2. Resolve thành absolute path trong storageRoot
     *   3. normalize() — xử lý "..", ".", "//" ...
     *   4. Kiểm tra: path có nằm trong storageRoot không?
     *
     * @param currentDir  Thư mục hiện tại của session (ví dụ: "", "folder")
     * @param clientPath  Path do client cung cấp (có thể chứa "..")
     * @return            Absolute Path đã được xác nhận an toàn
     * @throws SecurityException nếu path traversal bị phát hiện
     */
    private Path resolveSafe(String currentDir, String clientPath) {
        Path resolved;

        if (clientPath == null || clientPath.isEmpty() || clientPath.equals(".")) {
            // Không có path cụ thể → dùng currentDir
            resolved = storageRoot.resolve(currentDir).normalize();

        } else if (clientPath.startsWith("/")) {
            // Absolute path tính từ storageRoot (bỏ dấu "/" đầu)
            // Ví dụ: "/folder" → storageRoot/folder
            String relative = clientPath.substring(1);
            resolved = storageRoot.resolve(relative).normalize();

        } else {
            // Relative path từ currentDir
            // Ví dụ: currentDir="docs", clientPath="sub" → storageRoot/docs/sub
            resolved = storageRoot.resolve(currentDir).resolve(clientPath).normalize();
        }

        // ── BẢO MẬT: Kiểm tra path traversal ──────────────────────────
        // normalize() đã xử lý ".." → resolved là path thực sự
        // Nếu resolved không bắt đầu bằng storageRoot → client đang cố thoát ra
        if (!resolved.startsWith(storageRoot)) {
            throw new SecurityException(
                "PATH TRAVERSAL DETECTED! Attempted: " + clientPath);
        }

        return resolved;
    }

    // ================================================================
    // LIST — Liệt kê thư mục
    // ================================================================

    /**
     * Thực hiện LIST: liệt kê nội dung thư mục hiện tại.
     *
     * Format output:
     *   <TYPE>  <SIZE>  <DATE>  <NAME>
     *   <DIR>              -    2026-09-20 14:00  folder
     *        12345 bytes   .    2026-09-20 13:30  file.txt
     *
     * @param currentDir  Thư mục hiện tại của session
     * @return            Multi-line response bắt đầu 150, kết thúc 226
     */
    public String listDirectory(String currentDir) {
        try {
            Path dir = resolveSafe(currentDir, "");

            // Kiểm tra tồn tại và là directory
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                return "550 No such directory: /" + currentDir;
            }

            File[] files = dir.toFile().listFiles();

            // Build response
            StringBuilder sb = new StringBuilder();
            String displayPath = "/" + (currentDir.isEmpty() ? "" : currentDir + "/");
            sb.append("150 Directory listing for ").append(displayPath).append(":\n");

            if (files == null || files.length == 0) {
                sb.append("  (empty directory)\n");
            } else {
                // Sort: directories trước, rồi đến files, alphabetical
                Arrays.sort(files, Comparator
                        .<File, Boolean>comparing(f -> !f.isDirectory())
                        .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

                for (File f : files) {
                    String type = f.isDirectory() ? "<DIR>" : "     ";
                    String size = f.isDirectory()
                            ? "           -"
                            : String.format("%12d", f.length());
                    String date = DATE_FORMAT.format(new Date(f.lastModified()));
                    sb.append(String.format("  %s  %s bytes  %s  %s%n",
                            type, size, date, f.getName()));
                }
            }

            sb.append("226 Directory listing complete. ")
              .append(files == null ? 0 : files.length)
              .append(" item(s).");

            return sb.toString();

        } catch (SecurityException e) {
            System.err.println("[StorageManager] Security: " + e.getMessage());
            return "550 Access denied";
        }
    }

    // ================================================================
    // STOR — Nhận và lưu file
    // ================================================================

    /**
     * Nhận file từ Client qua InputStream và lưu vào storage.
     *
     * Sử dụng buffer 4096 bytes để đọc từng chunk thay vì đọc toàn bộ.
     *
     * Buffer 4096 bytes:
     *   Thay vì: new byte[fileSize] → tốn RAM nếu file 1GB
     *   Dùng:    new byte[4096]    → dùng 4KB, đọc nhiều lần
     *
     * @param currentDir  Thư mục hiện tại của session
     * @param filename    Tên file (do client cung cấp, cần validate)
     * @param fileSize    Số bytes sẽ nhận
     * @param rawIn       InputStream của Socket (để đọc raw bytes)
     * @return            Response code + message
     */
    public String receiveFile(String currentDir, String filename,
                              long fileSize, InputStream rawIn) {
        // ── Validate filename ────────────────────────────────────────
        // Filename không được chứa path separator hoặc ".."
        if (filename == null || filename.isEmpty()) {
            return "501 Filename required";
        }
        if (filename.contains("..") || filename.contains("/")
                || filename.contains("\\") || filename.contains(":")) {
            return "553 Invalid filename: contains illegal characters";
        }

        Path targetPath;
        try {
            // Resolve path an toàn
            targetPath = resolveSafe(currentDir, filename);
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }

        // ── Nhận bytes và ghi file ───────────────────────────────────
        // Buffer 4096 bytes: đọc tối đa 4096 bytes mỗi lần
        byte[] buffer = new byte[4096];
        long remaining = fileSize;   // số bytes còn cần đọc
        long totalReceived = 0;      // tổng bytes đã nhận

        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {

            while (remaining > 0) {
                // Đọc tối đa: min(buffer.length, remaining)
                // Không đọc quá số bytes của file (tránh chờ thêm data)
                int toRead = (int) Math.min(buffer.length, remaining);
                int bytesRead = rawIn.read(buffer, 0, toRead);

                if (bytesRead == -1) {
                    // Kết nối đứt giữa chừng — xóa file chưa hoàn chỉnh
                    fos.close();
                    Files.deleteIfExists(targetPath);
                    return "426 Connection closed; transfer aborted. Partial file deleted.";
                }

                // Ghi chunk này vào file
                fos.write(buffer, 0, bytesRead);
                totalReceived += bytesRead;
                remaining -= bytesRead;
            }

            // Flush để đảm bảo tất cả bytes được ghi xuống disk
            fos.flush();

        } catch (IOException e) {
            return "451 Requested action aborted: " + e.getMessage();
        }

        System.out.println("[StorageManager] File saved: " + targetPath
                + " (" + totalReceived + " bytes)");

        return "226 Transfer complete. File '" + filename + "' saved ("
                + totalReceived + " bytes).";
    }

    // ================================================================
    // RETR — Đọc và gửi file cho Client (Download)
    // ================================================================

    /**
     * Gửi file từ storage cho Client qua OutputStream theo buffer 4096 bytes.
     *
     * @param currentDir  Thư mục hiện tại của session
     * @param filename    Tên file cần lấy (do client chỉ định)
     * @param rawOut      OutputStream của Socket
     * @param writer      PrintWriter để gửi thông báo trung gian (150)
     * @return            Response cuối cùng (226 hoặc mã lỗi 5xx/4xx)
     */
    public String sendFile(String currentDir, String filename,
                           OutputStream rawOut, PrintWriter writer) {
        if (filename == null || filename.isEmpty()) {
            return "501 Filename required";
        }
        if (filename.contains("..") || filename.contains("/")
                || filename.contains("\\") || filename.contains(":")) {
            return "553 Invalid filename: contains illegal characters";
        }

        Path targetPath;
        try {
            targetPath = resolveSafe(currentDir, filename);
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }

        File file = targetPath.toFile();
        if (!file.exists()) {
            return "550 File not found: " + filename;
        }
        if (file.isDirectory()) {
            return "550 Cannot retrieve a directory: " + filename;
        }
        if (!file.canRead()) {
            return "550 Cannot read file: " + filename;
        }

        long fileSize = file.length();

        // ── Gửi mã 150 báo kích thước trước khi truyền raw bytes ───────
        writer.println("150 Opening binary mode data connection for '" + filename
                + "' (SIZE " + fileSize + ")");

        // ── Đọc đĩa và bơm từng chunk 4096 bytes ra socket ────────────
        byte[] buffer = new byte[4096];
        long totalSent = 0;

        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                rawOut.write(buffer, 0, bytesRead);
                totalSent += bytesRead;
            }
            rawOut.flush();
        } catch (IOException e) {
            return "426 Connection closed; transfer aborted: " + e.getMessage();
        }

        System.out.println("[StorageManager] File sent: " + targetPath
                + " (" + totalSent + " bytes)");

        return "226 Transfer complete. File '" + filename + "' sent ("
                + totalSent + " bytes).";
    }

    // ================================================================
    // Getters
    // ================================================================

    public Path getStorageRoot() {
        return storageRoot;
    }

    /**
     * Hiển thị current directory theo format FTP (bắt đầu bằng /)
     */
    public String formatDir(String currentDir) {
        return "/" + (currentDir.isEmpty() ? "" : currentDir);
    }

    // ================================================================
    // DELE — Xóa file (Phase 7)
    // ================================================================

    /**
     * Xóa file trong storage. Không xóa thư mục.
     *
     * @param currentDir Thư mục hiện tại của session
     * @param filename   Tên file cần xóa
     * @return           250 thành công | 550 lỗi
     */
    public String deleteFile(String currentDir, String filename) {
        if (filename == null || filename.isEmpty()) return "501 Filename required";
        if (filename.contains("..") || filename.contains("/")
                || filename.contains("\\") || filename.contains(":")) {
            return "553 Invalid filename: contains illegal characters";
        }

        Path targetPath;
        try {
            targetPath = resolveSafe(currentDir, filename);
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }

        File file = targetPath.toFile();
        if (!file.exists())      return "550 File not found: " + filename;
        if (file.isDirectory())  return "550 Cannot delete a directory: " + filename;

        try {
            Files.deleteIfExists(targetPath);
            System.out.println("[StorageManager] Deleted: " + targetPath);
            return "250 File deleted successfully: " + filename;
        } catch (IOException e) {
            return "451 Delete failed: " + e.getMessage();
        }
    }

    // ================================================================
    // RNFR / RNTO — Đổi tên file (Phase 7)
    // ================================================================

    /**
     * Đổi tên file từ oldName thành newName trong cùng thư mục.
     *
     * @param currentDir Thư mục hiện tại của session
     * @param oldName    Tên file hiện tại
     * @param newName    Tên file mới
     * @return           250 thành công | 550 lỗi
     */
    public String renameFile(String currentDir, String oldName, String newName) {
        if (oldName == null || oldName.isEmpty()) return "501 Old filename required";
        if (newName == null || newName.isEmpty()) return "501 New filename required";

        // Validate cả 2 tên file
        for (String name : new String[]{oldName, newName}) {
            if (name.contains("..") || name.contains("/")
                    || name.contains("\\") || name.contains(":")) {
                return "553 Invalid filename: contains illegal characters";
            }
        }

        Path srcPath, dstPath;
        try {
            srcPath = resolveSafe(currentDir, oldName);
            dstPath = resolveSafe(currentDir, newName);
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }

        if (!srcPath.toFile().exists()) return "550 File not found: " + oldName;
        if (dstPath.toFile().exists())  return "550 File already exists: " + newName;

        try {
            Files.move(srcPath, dstPath);
            System.out.println("[StorageManager] Renamed: " + srcPath + " -> " + dstPath);
            return "250 Rename successful: " + oldName + " -> " + newName;
        } catch (IOException e) {
            return "451 Rename failed: " + e.getMessage();
        }
    }
}
