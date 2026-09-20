package server;

import java.util.HashMap;
import java.util.Map;

/**
 * PHASE 4 - User Database
 *
 * Lưu trữ danh sách username/password hợp lệ.
 * Phase 4 dùng HashMap đơn giản trong bộ nhớ (in-memory).
 * Phase sau có thể thay bằng file, database thật.
 *
 * Tại sao tách ra class riêng?
 *   - Single Responsibility: CommandHandler lo xử lý command,
 *     UserDatabase lo quản lý tài khoản.
 *   - Sau này đổi nguồn dữ liệu (file/DB) chỉ cần sửa class này,
 *     không đụng vào CommandHandler.
 */
public class UserDatabase {

    // ----------------------------------------------------------------
    // Map<username, password> lưu các tài khoản hợp lệ.
    //
    // Trong thực tế: password phải được HASH (bcrypt, SHA-256...)
    // không bao giờ lưu plaintext.
    // Phase 4 dùng plaintext để dễ hiểu, đây là MÔI TRƯỜNG HỌC TẬP.
    // ----------------------------------------------------------------
    private final Map<String, String> users = new HashMap<>();

    /**
     * Constructor: nạp sẵn danh sách tài khoản test.
     */
    public UserDatabase() {
        // Tài khoản test cho Phase 4
        // Format: users.put("username", "password")
        users.put("admin", "123");
        users.put("alice", "alice123");
        users.put("bob",   "bob456");
        users.put("guest", "guest");
    }

    /**
     * Kiểm tra username/password có hợp lệ không.
     *
     * @param username  Username Client gõ vào lệnh USER
     * @param password  Password Client gõ vào lệnh PASS
     * @return true nếu đúng, false nếu sai hoặc không tồn tại
     */
    public boolean validate(String username, String password) {
        if (username == null || password == null) return false;

        // Lấy password thật từ DB theo username
        String storedPassword = users.get(username);

        // Nếu username không tồn tại → storedPassword = null → false
        // Nếu tồn tại → so sánh password
        return password.equals(storedPassword);
    }

    /**
     * Kiểm tra username có tồn tại không (không cần password).
     * Dùng khi xử lý lệnh USER — chỉ kiểm tra username trước.
     *
     * @param username  Username cần kiểm tra
     * @return true nếu username tồn tại trong hệ thống
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }
}
