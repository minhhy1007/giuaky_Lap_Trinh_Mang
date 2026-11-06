package server;

import server.dao.UserDAO;
import server.db.DBConnection;
import server.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TẦNG QUẢN LÝ TÀI KHOẢN NGƯỜI DÙNG (USER DATABASE)
 *
 * Đã nâng cấp trong Phase 2:
 *   - Kết nối trực tiếp CSDL MySQL thông qua UserDAO
 *   - Hỗ trợ lưu trữ tài khoản bền vững và mật khẩu băm SHA-256 kèm Salt
 *   - Cơ chế Fallback dự phòng: Nếu MySQL chưa bật, tự động chuyển về HashMap
 *     trong RAM để đảm bảo ứng dụng không bao giờ bị sập.
 */
public class UserDatabase {

    private final UserDAO userDAO;
    private final Map<String, String> inMemoryUsers = new HashMap<>();

    public UserDatabase() {
        this.userDAO = new UserDAO();

        // Tài khoản dự phòng In-Memory khi chạy offline không có MySQL
        inMemoryUsers.put("admin", "123");
        inMemoryUsers.put("alice", "alice123");
        inMemoryUsers.put("bob",   "bob456");
        inMemoryUsers.put("guest", "guest");
    }

    /**
     * Xác thực tài khoản đăng nhập và trả về đối tượng User đầy đủ
     *
     * @param username Username gõ vào lệnh USER
     * @param password Password gõ vào lệnh PASS
     * @return Đối tượng User nếu hợp lệ, null nếu sai
     */
    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;

        // 1. Thử xác thực qua MySQL DAO trước
        if (DBConnection.getInstance().isAvailable()) {
            User user = userDAO.validateUser(username, password);
            if (user != null) {
                return user;
            }
        }

        // 2. Fallback sang In-Memory nếu MySQL lỗi hoặc tài khoản offline
        String storedPass = inMemoryUsers.get(username);
        if (storedPass != null && storedPass.equals(password)) {
            User fallbackUser = new User();
            fallbackUser.setUserId(username.equals("admin") ? 1 : 2);
            fallbackUser.setUsername(username);
            fallbackUser.setHomeDir(username);
            fallbackUser.setRoleId(username.equals("admin") ? 1 : 2);
            fallbackUser.setRoleName(username.equals("admin") ? "ROLE_ADMIN" : "ROLE_USER");
            fallbackUser.setQuotaBytes(104857600); // 100MB
            fallbackUser.setActive(true);
            return fallbackUser;
        }

        return null;
    }

    /**
     * Phương thức tương thích ngược giữ nguyên chữ ký (validate trả về boolean)
     */
    public boolean validate(String username, String password) {
        return authenticate(username, password) != null;
    }

    /**
     * Kiểm tra username có tồn tại không
     */
    public boolean userExists(String username) {
        if (username == null) return false;
        if (DBConnection.getInstance().isAvailable()) {
            if (userDAO.findByUsername(username) != null) return true;
        }
        return inMemoryUsers.containsKey(username);
    }

    /**
     * Đăng ký tài khoản người dùng mới
     */
    public boolean register(String username, String password, String email) {
        if (DBConnection.getInstance().isAvailable()) {
            boolean success = userDAO.registerUser(username, password, email);
            if (success) {
                inMemoryUsers.put(username, password); // Đồng bộ vào bộ nhớ
                return true;
            }
            return false;
        }

        // Nếu offline, chỉ lưu tạm vào RAM
        if (inMemoryUsers.containsKey(username)) return false;
        inMemoryUsers.put(username, password);
        return true;
    }

    /**
     * Đổi mật khẩu tài khoản
     */
    public boolean changePassword(int userId, String username, String oldPass, String newPass) {
        if (DBConnection.getInstance().isAvailable() && userId > 0) {
            boolean success = userDAO.changePassword(userId, oldPass, newPass);
            if (success) {
                inMemoryUsers.put(username, newPass);
                return true;
            }
            return false;
        }

        // Fallback offline
        String stored = inMemoryUsers.get(username);
        if (stored != null && stored.equals(oldPass)) {
            inMemoryUsers.put(username, newPass);
            return true;
        }
        return false;
    }

    /**
     * Lấy danh sách toàn bộ người dùng (Dành cho Admin)
     */
    public List<User> getAllUsers() {
        if (DBConnection.getInstance().isAvailable()) {
            return userDAO.getAllUsers();
        }
        return List.of();
    }
}
