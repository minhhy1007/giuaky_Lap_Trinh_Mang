package server;

import server.dao.FileDAO;
import server.dao.UserDAO;
import server.util.SecurityUtil;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * QUẢN TRỊ LƯU TRỮ, SANDBOX, DUNG LƯỢNG QUOTA & TÍNH TOÀN VẸN (STORAGE MANAGER)
 *
 * Nâng cấp Phase 3:
 *   - Quản trị Cây Thư mục Con: Tạo thư mục (MKD), Xóa thư mục (RMD)
 *   - Quản trị Quota Nguyên tử: Chặn upload vượt dung lượng khi nhiều client upload đồng thời
 *   - Cơ chế File Tạm (.upload.tmp): Không ghi đè file cũ khi upload dở dang, tự dọn dẹp khi mất mạng
 *   - Tính toán và Đối soát mã băm SHA-256 đối soát tính toàn vẹn 100%
 *   - Đồng bộ hóa Metadata tệp tin vào bảng 'files' và cập nhật 'used_bytes' của User
 */
public class StorageManager {

    private final Path storageRoot;
    private final UserDAO userDAO;
    private final FileDAO fileDAO;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public StorageManager() throws IOException {
        this.storageRoot = Paths.get("storage").toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
        this.userDAO = new UserDAO();
        this.fileDAO = new FileDAO();
        System.out.println("[StorageManager] Storage Root: " + storageRoot);
    }

    // =========================================================================
    // CORE: RESOLVE PATH VỚI BẢO MẬT USER SANDBOX & CHỐNG PATH TRAVERSAL
    // =========================================================================

    public Path resolveSafe(String userHome, String currentDir, String clientPath) {
        Path userRoot;
        if (userHome == null || userHome.trim().isEmpty()) {
            userRoot = storageRoot;
        } else {
            userRoot = storageRoot.resolve(userHome.trim()).normalize();
        }

        try {
            if (!Files.exists(userRoot)) {
                Files.createDirectories(userRoot);
            }
        } catch (IOException e) {
            System.err.println("[StorageManager] Không thể tạo thư mục Sandbox: " + userRoot);
        }

        if (clientPath != null) {
            clientPath = clientPath.trim();
            if (clientPath.startsWith("\"") && clientPath.endsWith("\"") && clientPath.length() >= 2) {
                clientPath = clientPath.substring(1, clientPath.length() - 1).trim();
            }
        }

        Path resolved;
        if (clientPath == null || clientPath.isEmpty() || clientPath.equals(".")) {
            resolved = userRoot.resolve(currentDir).normalize();
        } else if (clientPath.startsWith("/")) {
            String relative = clientPath.substring(1);
            resolved = userRoot.resolve(relative).normalize();
        } else {
            resolved = userRoot.resolve(currentDir).resolve(clientPath).normalize();
        }

        // BẢO MẬT: Đường dẫn phải thuộc userRoot
        if (!resolved.startsWith(userRoot)) {
            throw new SecurityException("CROSS-ACCOUNT HOẶC PATH TRAVERSAL BỊ CHẶN! Yêu cầu: " + clientPath);
        }

        return resolved;
    }

    public Path resolveSafe(String currentDir, String clientPath) {
        return resolveSafe("", currentDir, clientPath);
    }

    // =========================================================================
    // QUẢN TRỊ THƯ MỤC: LIST, MKD (MAKE DIR), RMD (REMOVE DIR)
    // =========================================================================

    public String listDirectory(String userHome, String currentDir) {
        try {
            Path dir = resolveSafe(userHome, currentDir, "");
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                return "550 No such directory: /" + currentDir;
            }

            File[] files = dir.toFile().listFiles();
            StringBuilder sb = new StringBuilder();
            String displayPath = "/" + (currentDir.isEmpty() ? "" : currentDir + "/");
            sb.append("150 Directory listing for ").append(displayPath).append(":\n");

            if (files == null || files.length == 0) {
                sb.append("  (empty directory)\n");
            } else {
                Arrays.sort(files, Comparator
                        .<File, Boolean>comparing(f -> !f.isDirectory())
                        .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

                for (File f : files) {
                    // Ẩn các file tạm upload dở dang .upload.tmp
                    if (f.getName().endsWith(".upload.tmp")) continue;

                    String type = f.isDirectory() ? "<DIR>" : "     ";
                    String size = f.isDirectory() ? "           -" : String.format("%12d", f.length());
                    String date = DATE_FORMAT.format(new Date(f.lastModified()));
                    sb.append(String.format("  %s  %s bytes  %s  %s%n", type, size, date, f.getName()));
                }
            }

            sb.append("226 Directory listing complete. ")
              .append(files == null ? 0 : files.length)
              .append(" item(s).");
            return sb.toString();

        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }
    }

    public String listDirectory(String currentDir) {
        return listDirectory("", currentDir);
    }

    /**
     * Tạo thư mục mới trong Sandbox (MKD)
     */
    public String makeDirectory(String userHome, String currentDir, String dirName) {
        if (dirName == null || dirName.trim().isEmpty()) {
            return "501 Directory name required";
        }
        dirName = dirName.trim();
        if (dirName.startsWith("\"") && dirName.endsWith("\"") && dirName.length() >= 2) {
            dirName = dirName.substring(1, dirName.length() - 1).trim();
        }
        if (dirName.contains("..") || dirName.contains("/") || dirName.contains("\\") || dirName.contains(":")) {
            return "553 Invalid directory name";
        }

        try {
            Path targetDir = resolveSafe(userHome, currentDir, dirName.trim());
            if (Files.exists(targetDir)) {
                return "550 Directory or file already exists: " + dirName;
            }
            Files.createDirectories(targetDir);
            String fullPath = formatDir(currentDir) + (currentDir.isEmpty() ? "" : "/") + dirName.trim();
            return "257 \"" + fullPath + "\" directory created";
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        } catch (IOException e) {
            return "451 Failed to create directory: " + e.getMessage();
        }
    }

    /**
     * Xóa thư mục rỗng trong Sandbox (RMD)
     */
    public String removeDirectory(String userHome, String currentDir, String dirName) {
        if (dirName == null || dirName.trim().isEmpty()) {
            return "501 Directory name required";
        }
        dirName = dirName.trim();
        if (dirName.startsWith("\"") && dirName.endsWith("\"") && dirName.length() >= 2) {
            dirName = dirName.substring(1, dirName.length() - 1).trim();
        }
        if (dirName.contains("..") || dirName.contains("/") || dirName.contains("\\") || dirName.contains(":")) {
            return "553 Invalid directory name";
        }

        try {
            Path targetDir = resolveSafe(userHome, currentDir, dirName.trim());
            if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
                return "550 Directory not found: " + dirName;
            }

            File[] contents = targetDir.toFile().listFiles();
            if (contents != null && contents.length > 0) {
                return "550 Directory not empty: " + dirName;
            }

            Files.delete(targetDir);
            return "250 Directory removed successfully: " + dirName;
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        } catch (IOException e) {
            return "451 Failed to remove directory: " + e.getMessage();
        }
    }

    // =========================================================================
    // UPLOAD: STOR VỚI QUOTA NGUYÊN TỬ, FILE TẠM & SHA-256 CHECKSUM
    // =========================================================================

    /**
     * Nhận file từ Client, kiểm tra Quota nguyên tử, ghi file tạm và tính SHA-256
     */
    public String receiveFile(int userId, String userHome, String currentDir, String filename,
                              long fileSize, InputStream rawIn) {
        return receiveFile(userId, userHome, currentDir, filename, fileSize, rawIn, false);
    }

    public String receiveFile(int userId, String userHome, String currentDir, String filename,
                              long fileSize, InputStream rawIn, boolean quotaPreReserved) {
        if (filename == null || filename.isEmpty()) return "501 Filename required";
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            return "553 Invalid filename: contains illegal characters";
        }

        Path targetPath;
        try {
            targetPath = resolveSafe(userHome, currentDir, filename);
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }

        // 1. KIỂM TRA VÀ ĐẶT TRƯỚC DUNG LƯỢNG QUOTA NGUYÊN TỬ (Atomic Check & Reserve)
        if (!quotaPreReserved && userId > 0 && fileSize > 0) {
            boolean quotaReserved = userDAO.checkAndReserveQuota(userId, fileSize);
            if (!quotaReserved) {
                long[] qInfo = userDAO.getQuotaInfo(userId);
                long remaining = Math.max(0, qInfo[1] - qInfo[0]);
                return String.format("552 Storage quota exceeded: File size %d bytes exceeds remaining quota (%d bytes).",
                        fileSize, remaining);
            }
        }

        // 2. TẠO FILE TẠM (.upload.tmp) — Tránh ghi đè file cũ khi mạng đứt giữa chừng
        Path tempPath = targetPath.resolveSibling(filename + "." + System.currentTimeMillis() + ".upload.tmp");
        byte[] buffer = new byte[4096];
        long remaining = fileSize;
        long totalReceived = 0;

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            if (userId > 0 && fileSize > 0) userDAO.releaseQuota(userId, fileSize);
            return "451 Internal server error: Missing SHA-256 algorithm";
        }

        try (FileOutputStream fos = new FileOutputStream(tempPath.toFile())) {
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int bytesRead = rawIn.read(buffer, 0, toRead);

                if (bytesRead == -1) {
                    // Mất kết nối mạng giữa chừng: Xóa file tạm và hoàn trả Quota đã trừ
                    fos.close();
                    Files.deleteIfExists(tempPath);
                    if (userId > 0 && fileSize > 0) userDAO.releaseQuota(userId, fileSize);
                    return "426 Connection closed; transfer aborted. Temporary file discarded.";
                }

                fos.write(buffer, 0, bytesRead);
                md.update(buffer, 0, bytesRead);
                totalReceived += bytesRead;
                remaining -= bytesRead;
            }
            fos.flush();
        } catch (IOException e) {
            try { Files.deleteIfExists(tempPath); } catch (IOException ignored) {}
            if (userId > 0 && fileSize > 0) userDAO.releaseQuota(userId, fileSize);
            return "451 Requested action aborted: " + e.getMessage();
        }

        // 3. ATOMIC MOVE: Chuyển từ file tạm sang file chính khi đã nhận đủ 100% bytes
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try { Files.deleteIfExists(tempPath); } catch (IOException ignored) {}
            if (userId > 0 && fileSize > 0) userDAO.releaseQuota(userId, fileSize);
            return "451 Failed to finalize file: " + e.getMessage();
        }

        // 4. LẤY MÃ BĂM SHA-256 VÀ LƯU METADATA VÀO MYSQL
        String sha256 = SecurityUtil.bytesToHex(md.digest());
        String relPath = "/" + (currentDir.isEmpty() ? "" : currentDir + "/") + filename;

        if (userId > 0) {
            fileDAO.upsertFileRecord(userId, filename, relPath, totalReceived, sha256);
        }

        System.out.println("[StorageManager] Saved [" + userHome + "]: " + targetPath + " (" + totalReceived + " bytes) SHA256: " + sha256);
        return "226 Transfer complete. File '" + filename + "' saved (" + totalReceived + " bytes). SHA256: " + sha256;
    }

    public String receiveFile(String userHome, String currentDir, String filename, long fileSize, InputStream rawIn) {
        return receiveFile(0, userHome, currentDir, filename, fileSize, rawIn);
    }

    public String receiveFile(String currentDir, String filename, long fileSize, InputStream rawIn) {
        return receiveFile(0, "", currentDir, filename, fileSize, rawIn);
    }

    // =========================================================================
    // DOWNLOAD: RETR VỚI STREAM NHỊ PHÂN VÀ CHECKSUM
    // =========================================================================

    public String sendFile(String userHome, String currentDir, String filename, OutputStream rawOut, PrintWriter writer) {
        if (filename == null || filename.isEmpty()) return "501 Filename required";
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            return "553 Invalid filename: contains illegal characters";
        }

        Path targetPath;
        try {
            targetPath = resolveSafe(userHome, currentDir, filename);
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }

        File file = targetPath.toFile();
        if (!file.exists())     return "550 File not found: " + filename;
        if (file.isDirectory()) return "550 Cannot retrieve a directory: " + filename;
        if (!file.canRead())    return "550 Cannot read file: " + filename;

        long fileSize = file.length();
        writer.println("150 Opening binary mode data connection for '" + filename + "' (SIZE " + fileSize + ")");

        byte[] buffer = new byte[4096];
        long totalSent = 0;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return "451 Internal server error: Missing SHA-256 algorithm";
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                rawOut.write(buffer, 0, bytesRead);
                md.update(buffer, 0, bytesRead);
                totalSent += bytesRead;
            }
            rawOut.flush();
        } catch (IOException e) {
            return "426 Connection closed; transfer aborted: " + e.getMessage();
        }

        String sha256 = SecurityUtil.bytesToHex(md.digest());
        return "226 Transfer complete. File '" + filename + "' sent (" + totalSent + " bytes). SHA256: " + sha256;
    }

    public String sendFile(String currentDir, String filename, OutputStream rawOut, PrintWriter writer) {
        return sendFile("", currentDir, filename, rawOut, writer);
    }

    // =========================================================================
    // XÓA VÀ ĐỔI TÊN TỆP TIN: ĐỒNG BỘ CSDL VÀ HOÀN TRẢ QUOTA
    // =========================================================================

    public String deleteFile(int userId, String userHome, String currentDir, String filename) {
        if (filename == null || filename.isEmpty()) return "501 Filename required";
        filename = filename.trim();
        if (filename.startsWith("\"") && filename.endsWith("\"") && filename.length() >= 2) {
            filename = filename.substring(1, filename.length() - 1).trim();
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            return "553 Invalid filename: contains illegal characters";
        }

        Path targetPath;
        try {
            targetPath = resolveSafe(userHome, currentDir, filename);
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }

        File file = targetPath.toFile();
        if (!file.exists())     return "550 File not found: " + filename;
        if (file.isDirectory()) return "550 Cannot delete a directory: " + filename;

        long fileSize = file.length();
        try {
            Files.deleteIfExists(targetPath);

            // Cập nhật CSDL và hoàn trả dung lượng Quota
            if (userId > 0) {
                String relPath = "/" + (currentDir.isEmpty() ? "" : currentDir + "/") + filename;
                fileDAO.deleteFileRecord(userId, relPath);
                userDAO.releaseQuota(userId, fileSize);
            }

            return "250 File deleted successfully: " + filename;
        } catch (IOException e) {
            return "451 Delete failed: " + e.getMessage();
        }
    }

    public String deleteFile(String userHome, String currentDir, String filename) {
        return deleteFile(0, userHome, currentDir, filename);
    }

    public String deleteFile(String currentDir, String filename) {
        return deleteFile(0, "", currentDir, filename);
    }

    public String renameFile(int userId, String userHome, String currentDir, String oldName, String newName) {
        if (oldName == null || oldName.isEmpty()) return "501 Old filename required";
        if (newName == null || newName.isEmpty()) return "501 New filename required";
        oldName = oldName.trim();
        newName = newName.trim();
        if (oldName.startsWith("\"") && oldName.endsWith("\"") && oldName.length() >= 2) {
            oldName = oldName.substring(1, oldName.length() - 1).trim();
        }
        if (newName.startsWith("\"") && newName.endsWith("\"") && newName.length() >= 2) {
            newName = newName.substring(1, newName.length() - 1).trim();
        }

        for (String name : new String[]{oldName, newName}) {
            if (name.contains("..") || name.contains("/") || name.contains("\\") || name.contains(":")) {
                return "553 Invalid filename: contains illegal characters";
            }
        }

        Path srcPath, dstPath;
        try {
            srcPath = resolveSafe(userHome, currentDir, oldName);
            dstPath = resolveSafe(userHome, currentDir, newName);
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }

        if (!srcPath.toFile().exists()) return "550 File not found: " + oldName;
        if (dstPath.toFile().exists())  return "550 File already exists: " + newName;

        try {
            Files.move(srcPath, dstPath);

            // Cập nhật CSDL
            if (userId > 0) {
                String oldRel = "/" + (currentDir.isEmpty() ? "" : currentDir + "/") + oldName;
                String newRel = "/" + (currentDir.isEmpty() ? "" : currentDir + "/") + newName;
                fileDAO.renameFileRecord(userId, oldRel, newName, newRel);
                if (dstPath.toFile().isDirectory()) {
                    fileDAO.renameDirectoryRecords(userId, oldRel, newRel);
                }
            }

            return "250 Rename successful: " + oldName + " -> " + newName;
        } catch (IOException e) {
            return "451 Rename failed: " + e.getMessage();
        }
    }

    public String renameFile(String userHome, String currentDir, String oldName, String newName) {
        return renameFile(0, userHome, currentDir, oldName, newName);
    }

    public String renameFile(String currentDir, String oldName, String newName) {
        return renameFile(0, "", currentDir, oldName, newName);
    }

    public Path getStorageRoot() {
        return storageRoot;
    }

    public String formatDir(String currentDir) {
        return "/" + (currentDir.isEmpty() ? "" : currentDir);
    }
}
