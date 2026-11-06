package server;

import server.model.User;

/**
 * PHASE 2 - Session (Máy trạng thái phiên làm việc FSM & Phân quyền)
 *
 * Lưu trữ trạng thái và danh tính của Client đang kết nối:
 *   - FSM States: UNAUTHENTICATED -> WAITING_PASSWORD -> AUTHENTICATED
 *   - Lưu trữ đối tượng User đầy đủ (ID, Role, HomeDir, Quota)
 *   - Cung cấp phương thức kiểm tra quyền Server-side (isAdmin)
 *   - Quản lý thư mục hiện tại trong Sandbox của người dùng
 */
public class Session {

    public enum State {
        UNAUTHENTICATED,   // Chưa xác thực
        WAITING_PASSWORD,  // Đã gửi USER, đang chờ PASS
        AUTHENTICATED      // Đã xác thực thành công
    }

    private State state = State.UNAUTHENTICATED;
    private String pendingUsername = null;
    private String loggedInUser = null;
    private User currentUser = null; // Đối tượng User đầy đủ sau khi đăng nhập thành công
    private String currentDirectory = ""; // Đường dẫn tương đối bên trong Sandbox của User

    // renameFrom — lưu tên file đang chờ đổi tên (RNFR/RNTO)
    private String renameFrom = null;

    // Getters
    public State getState()             { return state; }
    public String getPendingUsername()  { return pendingUsername; }
    public String getLoggedInUser()     { return loggedInUser; }
    public User getCurrentUser()        { return currentUser; }

    public boolean isAuthenticated() {
        return state == State.AUTHENTICATED;
    }

    /**
     * Kiểm tra phiên hiện tại có quyền Quản trị viên (ROLE_ADMIN) hay không.
     * Kiểm tra nghiêm ngặt tại Server, không tin role client gửi lên.
     */
    public boolean isAdmin() {
        return isAuthenticated() && currentUser != null && currentUser.isAdmin();
    }

    /**
     * Lấy tên thư mục Sandbox của User hiện tại trên máy chủ
     */
    public String getHomeDir() {
        if (currentUser != null && currentUser.getHomeDir() != null) {
            return currentUser.getHomeDir();
        }
        return (loggedInUser != null) ? loggedInUser : "";
    }

    // State Transitions
    public void setPendingUsername(String username) {
        this.pendingUsername = username;
        this.state = State.WAITING_PASSWORD;
    }

    public void loginSuccess(User user) {
        this.currentUser = user;
        this.loggedInUser = (user != null) ? user.getUsername() : this.pendingUsername;
        this.pendingUsername = null;
        this.state = State.AUTHENTICATED;
    }

    public void loginSuccess() {
        this.loggedInUser = this.pendingUsername;
        this.pendingUsername = null;
        this.state = State.AUTHENTICATED;
    }

    public void reset() {
        this.state = State.UNAUTHENTICATED;
        this.pendingUsername = null;
        this.loggedInUser = null;
        this.currentUser = null;
        this.currentDirectory = "";
        this.renameFrom = null;
    }

    public String getCurrentDirectory()         { return currentDirectory; }
    public void setCurrentDirectory(String dir) { this.currentDirectory = (dir != null) ? dir.trim() : ""; }

    /**
     * Di chuyển lên thư mục cha (CDUP) mà không bao giờ vượt ra ngoài Sandbox.
     */
    public void cdup() {
        if (currentDirectory == null || currentDirectory.isEmpty()) {
            currentDirectory = "";
            return;
        }
        int lastSlash = currentDirectory.lastIndexOf('/');
        if (lastSlash > 0) {
            currentDirectory = currentDirectory.substring(0, lastSlash);
        } else {
            currentDirectory = "";
        }
    }

    /**
     * Chuyển thư mục làm việc (CWD).
     */
    public void cd(String dir) {
        if (dir == null || dir.isEmpty() || dir.equals("/")) {
            currentDirectory = "";
        } else if (dir.equals("..")) {
            cdup();
        } else {
            String clean = dir.startsWith("/") ? dir.substring(1) : dir;
            if (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
            if (currentDirectory.isEmpty()) {
                currentDirectory = clean;
            } else {
                currentDirectory = currentDirectory + "/" + clean;
            }
        }
    }

    public String getRenameFrom()          { return renameFrom; }
    public void setRenameFrom(String n)    { this.renameFrom = n; }
    public void clearRenameFrom()          { this.renameFrom = null; }
    public boolean hasRenameFrom()         { return renameFrom != null; }

    @Override
    public String toString() {
        return String.format("Session[State=%s, User=%s, Role=%s, Sandbox=%s]",
                state, loggedInUser, (currentUser != null ? currentUser.getRoleName() : "N/A"), getHomeDir());
    }
}
