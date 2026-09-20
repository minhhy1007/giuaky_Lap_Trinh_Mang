package server;

/**
 * PHASE 5 - Session (cập nhật)
 *
 * Lưu trữ TRẠNG THÁI của một phiên kết nối Client cụ thể.
 * Mỗi ClientHandler → mỗi CommandHandler → mỗi Session.
 * Các Session hoàn toàn độc lập nhau.
 *
 * Vòng đời (lifecycle) của một Session:
 *
 *   [UNAUTHENTICATED]
 *       │  USER <name>
 *       ▼
 *   [WAITING_PASSWORD]      ← biết username, chưa biết password
 *       │  PASS đúng            │  PASS sai
 *       ▼                       ▼
 *   [AUTHENTICATED]         [UNAUTHENTICATED]
 *       │  LOGOUT / QUIT
 *       ▼
 *   [UNAUTHENTICATED]
 */
public class Session {

    // ----------------------------------------------------------------
    // Enum định nghĩa các trạng thái rõ ràng.
    // Dùng enum thay vì boolean để dễ mở rộng sau này.
    // ----------------------------------------------------------------
    public enum State {
        UNAUTHENTICATED,   // chưa gõ USER
        WAITING_PASSWORD,  // đã gõ USER, đang chờ PASS
        AUTHENTICATED      // đã login thành công
    }

    // Trạng thái hiện tại — bắt đầu là UNAUTHENTICATED
    private State state = State.UNAUTHENTICATED;

    // Username vừa gõ bằng lệnh USER, chờ xác nhận bằng PASS
    // null nếu chưa gõ USER
    private String pendingUsername = null;

    // Username đã xác thực thành công — chỉ có giá trị khi AUTHENTICATED
    private String loggedInUser = null;

    // ----------------------------------------------------------------
    // PHASE 5: Thư mục hiện tại của session (relative path từ storageRoot)
    // ""       = root (storage/)
    // "folder" = storage/folder/
    // Reset về "" khi logout
    // ----------------------------------------------------------------
    private String currentDirectory = "";

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public State getState() {
        return state;
    }

    public String getPendingUsername() {
        return pendingUsername;
    }

    public String getLoggedInUser() {
        return loggedInUser;
    }

    /** Kiểm tra nhanh xem Client đã login thành công chưa. */
    public boolean isAuthenticated() {
        return state == State.AUTHENTICATED;
    }

    // ----------------------------------------------------------------
    // State transitions — thay đổi trạng thái theo từng bước auth
    // ----------------------------------------------------------------

    /**
     * Gọi khi Client gõ USER <username>.
     * Chuyển sang trạng thái chờ password.
     *
     * @param username  Username Client gõ vào
     */
    public void setPendingUsername(String username) {
        this.pendingUsername = username;
        this.state = State.WAITING_PASSWORD;
    }

    /**
     * Gọi khi PASS đúng.
     * Chuyển sang AUTHENTICATED, lưu tên user.
     */
    public void loginSuccess() {
        this.loggedInUser = this.pendingUsername;
        this.pendingUsername = null;
        this.state = State.AUTHENTICATED;
    }

    /**
     * Gọi khi PASS sai hoặc khi LOGOUT.
     * Reset về UNAUTHENTICATED, xóa mọi thông tin.
     */
    public void reset() {
        this.state           = State.UNAUTHENTICATED;
        this.pendingUsername = null;
        this.loggedInUser    = null;
        this.currentDirectory = "";  // reset về root khi logout
    }

    // PHASE 5: Getters/setters cho currentDirectory
    public String getCurrentDirectory()          { return currentDirectory; }
    public void setCurrentDirectory(String dir)  { this.currentDirectory = dir; }

    // ----------------------------------------------------------------
    // PHASE 7: renameFrom — lưu tên file đang chờ đổi tên (RNFR/RNTO)
    // null khi chưa có lệnh RNFR nào
    // ----------------------------------------------------------------
    private String renameFrom = null;

    public String getRenameFrom()          { return renameFrom; }
    public void   setRenameFrom(String n)  { this.renameFrom = n; }
    public void   clearRenameFrom()        { this.renameFrom = null; }
    public boolean hasRenameFrom()         { return renameFrom != null; }

    @Override
    public String toString() {
        return "Session{state=" + state
                + ", pending=" + pendingUsername
                + ", loggedIn=" + loggedInUser + "}";
    }
}
