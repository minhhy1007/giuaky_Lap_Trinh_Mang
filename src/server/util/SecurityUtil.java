package server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * TIỆN ÍCH BẢO MẬT & MÃ HÓA (SECURITY & HASHING UTILITY)
 *
 * Trách nhiệm:
 *   - Băm mật khẩu một chiều an toàn bằng chuẩn SHA-256 kèm Salt
 *   - Đối chiếu mật khẩu (Verify) với cơ chế tương thích ngược (Backward Compatibility)
 *   - Tính mã băm SHA-256 của tệp tin để kiểm tra tính toàn vẹn (Integrity Check)
 *   - Tạo mã Token ngẫu nhiên bảo mật cao (SecureRandom) cho tính năng chia sẻ file
 */
public class SecurityUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    /**
     * Băm mật khẩu dạng chuỗi kết hợp với Salt ngẫu nhiên bằng giải thuật SHA-256.
     *
     * @param password Mật khẩu gốc dạng text
     * @param salt     Chuỗi muối (Salt) chống tấn công Rainbow Table
     * @return Chuỗi Hexadecimal 64 ký tự của SHA-256
     */
    public static String hashPassword(String password, String salt) {
        if (password == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (salt != null && !salt.isEmpty()) {
                md.update(salt.getBytes(StandardCharsets.UTF_8));
            }
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi hệ thống: Không tìm thấy giải thuật SHA-256", e);
        }
    }

    /**
     * Xác thực mật khẩu đăng nhập.
     * Hỗ trợ tương thích ngược: Nếu DB chứa mật khẩu băm SHA-256 thì đối chiếu hash,
     * nếu là bản nâng cấp ban đầu vẫn nhận diện được chính xác.
     */
    public static boolean verifyPassword(String rawPassword, String salt, String storedHash) {
        if (rawPassword == null || storedHash == null) return false;

        // 1. Kiểm tra đối chiếu mật khẩu băm với salt
        String computedHashWithSalt = hashPassword(rawPassword, salt);
        if (storedHash.equalsIgnoreCase(computedHashWithSalt)) {
            return true;
        }

        // 2. Kiểm tra đối chiếu mật khẩu băm không salt (chuẩn SHA-256 thuần của Seed Data)
        String computedHashNoSalt = hashPassword(rawPassword, "");
        if (storedHash.equalsIgnoreCase(computedHashNoSalt)) {
            return true;
        }

        // 3. Dự phòng tương thích tạm thời cho tài khoản demo plaintext cũ
        return storedHash.equals(rawPassword);
    }

    /**
     * Tạo chuỗi Salt ngẫu nhiên 16 bytes (32 ký tự Hex)
     */
    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        return bytesToHex(saltBytes);
    }

    /**
     * Tạo mã Token ngẫu nhiên 32 bytes (64 ký tự Hex) dùng cho liên kết chia sẻ file
     */
    public static String generateToken() {
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        return bytesToHex(tokenBytes);
    }

    /**
     * Tính toán mã băm SHA-256 của một file trên đĩa cứng để kiểm tra tính toàn vẹn.
     * Đọc tệp theo khối 8KB để tiết kiệm RAM với các tệp tin lớn hàng GB.
     *
     * @param file Tệp tin cần tính mã băm
     * @return Chuỗi Hex SHA-256 của tệp
     * @throws IOException nếu không đọc được tệp
     */
    public static String calculateSHA256(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return calculateSHA256(is);
        }
    }

    /**
     * Tính toán mã băm SHA-256 từ một InputStream
     */
    public static String calculateSHA256(InputStream is) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }

    /**
     * Chuyển mảng byte sang chuỗi Hexadecimal chữ thường
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}

