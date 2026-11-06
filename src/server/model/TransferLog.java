package server.model;

import java.sql.Timestamp;

/**
 * THỰC THỂ NHẬT KÝ KIỂM TOÁN (TRANSFER LOG ENTITY)
 *
 * Ánh xạ tới bảng 'transfer_logs' trong MySQL.
 * Lưu vết mọi hoạt động truyền nhận file, đăng nhập, xóa, đổi tên, chia sẻ.
 */
public class TransferLog {

    private int       logId;
    private Integer   userId;
    private String    username; // Thông tin hiển thị
    private String    clientIp;
    private String    actionType; // LOGIN, LOGOUT, UPLOAD, DOWNLOAD, DELETE, RENAME, MKDIR, RMDIR, SHARE, REVOKE, ACCESS_DENIED, DOWNLOAD_SHARED
    private String    filename;
    private long      bytesTransferred;
    private String    status;     // SUCCESS, FAILED
    private String    message;
    private Timestamp createdAt;

    public TransferLog() {}

    public TransferLog(int logId, Integer userId, String clientIp, String actionType,
                       String filename, long bytesTransferred, String status,
                       String message, Timestamp createdAt) {
        this.logId            = logId;
        this.userId           = userId;
        this.clientIp         = clientIp;
        this.actionType       = actionType;
        this.filename         = filename;
        this.bytesTransferred = bytesTransferred;
        this.status           = status;
        this.message          = message;
        this.createdAt        = createdAt;
    }

    // Getters and Setters
    public int       getLogId()                        { return logId; }
    public void      setLogId(int logId)               { this.logId = logId; }

    public Integer   getUserId()                       { return userId; }
    public void      setUserId(Integer userId)         { this.userId = userId; }

    public String    getUsername()                     { return username; }
    public void      setUsername(String username)      { this.username = username; }

    public String    getClientIp()                     { return clientIp; }
    public void      setClientIp(String clientIp)      { this.clientIp = clientIp; }

    public String    getActionType()                   { return actionType; }
    public void      setActionType(String actionType)  { this.actionType = actionType; }

    public String    getFilename()                     { return filename; }
    public void      setFilename(String filename)      { this.filename = filename; }

    public long      getBytesTransferred()             { return bytesTransferred; }
    public void      setBytesTransferred(long bytes)   { this.bytesTransferred = bytes; }

    public String    getStatus()                       { return status; }
    public void      setStatus(String status)          { this.status = status; }

    public String    getMessage()                      { return message; }
    public void      setMessage(String message)        { this.message = message; }

    public Timestamp getCreatedAt()                    { return createdAt; }
    public void      setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
