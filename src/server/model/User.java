package server.model;

import java.sql.Timestamp;

/**
 * THỰC THỂ NGƯỜI DÙNG (USER ENTITY)
 *
 * Đại diện cho một tài khoản trong hệ thống Custom FTP.
 * Ánh xạ tương ứng với bảng 'users' trong MySQL.
 */
public class User {

    private int       userId;
    private String    username;
    private String    passwordHash;
    private String    salt;
    private String    email;
    private int       roleId;      // 1 = ROLE_ADMIN, 2 = ROLE_USER
    private String    roleName;    // "ROLE_ADMIN" hoặc "ROLE_USER"
    private long      quotaBytes;  // Dung lượng tối đa (bytes)
    private long      usedBytes;   // Dung lượng đã sử dụng (bytes)
    private String    homeDir;     // Tên thư mục Sandbox trên máy chủ
    private boolean   isActive;
    private Timestamp createdAt;

    public User() {}

    public User(int userId, String username, String passwordHash, String salt,
                String email, int roleId, String roleName, long quotaBytes,
                long usedBytes, String homeDir, boolean isActive) {
        this.userId       = userId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.salt         = salt;
        this.email        = email;
        this.roleId       = roleId;
        this.roleName     = roleName;
        this.quotaBytes   = quotaBytes;
        this.usedBytes    = usedBytes;
        this.homeDir      = homeDir;
        this.isActive     = isActive;
    }

    /**
     * Kiểm tra người dùng có phải Quản trị viên (ROLE_ADMIN) hay không.
     */
    public boolean isAdmin() {
        return this.roleId == 1 || "ROLE_ADMIN".equalsIgnoreCase(this.roleName);
    }

    // Getters and Setters
    public int       getUserId()                 { return userId; }
    public void      setUserId(int userId)       { this.userId = userId; }

    public String    getUsername()               { return username; }
    public void      setUsername(String u)       { this.username = u; }

    public String    getPasswordHash()           { return passwordHash; }
    public void      setPasswordHash(String p)   { this.passwordHash = p; }

    public String    getSalt()                   { return salt; }
    public void      setSalt(String salt)        { this.salt = salt; }

    public String    getEmail()                  { return email; }
    public void      setEmail(String email)      { this.email = email; }

    public int       getRoleId()                 { return roleId; }
    public void      setRoleId(int roleId)       { this.roleId = roleId; }

    public String    getRoleName()               { return roleName; }
    public void      setRoleName(String r)       { this.roleName = r; }

    public long      getQuotaBytes()             { return quotaBytes; }
    public void      setQuotaBytes(long q)       { this.quotaBytes = q; }

    public long      getUsedBytes()              { return usedBytes; }
    public void      setUsedBytes(long u)        { this.usedBytes = u; }

    public String    getHomeDir()                { return homeDir; }
    public void      setHomeDir(String homeDir)  { this.homeDir = homeDir; }

    public boolean   isActive()                  { return isActive; }
    public void      setActive(boolean active)   { isActive = active; }

    public Timestamp getCreatedAt()              { return createdAt; }
    public void      setCreatedAt(Timestamp c)   { this.createdAt = c; }

    @Override
    public String toString() {
        return String.format("User[ID=%d, User='%s', Role='%s', Home='%s', Quota=%dMB]",
                userId, username, roleName, homeDir, quotaBytes / (1024 * 1024));
    }
}

