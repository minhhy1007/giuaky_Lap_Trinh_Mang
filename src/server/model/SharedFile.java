package server.model;

import java.sql.Timestamp;

/**
 * THỰC THỂ TỆP CHIA SẺ (SHARED FILE ENTITY)
 *
 * Đại diện cho một phiên chia sẻ tệp tin:
 *   - Chia sẻ đích danh theo tài khoản (shared_with_user_id)
 *   - Chia sẻ bằng liên kết/mã Token bảo mật (share_token)
 *   - Phân quyền: READ_ONLY (chỉ xem thông tin), DOWNLOAD (tải về), FULL (toàn quyền)
 *   - Thời hạn hết hiệu lực (expires_at) và trạng thái thu hồi (is_revoked)
 *   - Số lượt truy cập (access_count) và số lượt tải tối đa (max_downloads)
 */
public class SharedFile {

    private int shareId;
    private int fileId;
    private int ownerId;
    private Integer sharedWithUserId; // null nếu là chia sẻ Token công khai
    private String shareToken;
    private String permission;        // READ_ONLY, DOWNLOAD, FULL
    private Timestamp expiresAt;
    private boolean isRevoked;
    private int accessCount;
    private int maxDownloads;         // 0: không giới hạn
    private Timestamp createdAt;

    // Các trường mở rộng để hiển thị trên Client UI
    private String filename;
    private long fileSize;
    private String ownerUsername;
    private String recipientUsername;
    private String relativePath;

    public SharedFile() {}

    public int getShareId() { return shareId; }
    public void setShareId(int shareId) { this.shareId = shareId; }

    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public Integer getSharedWithUserId() { return sharedWithUserId; }
    public void setSharedWithUserId(Integer sharedWithUserId) { this.sharedWithUserId = sharedWithUserId; }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return isRevoked; }
    public void setRevoked(boolean revoked) { isRevoked = revoked; }

    public int getAccessCount() { return accessCount; }
    public void setAccessCount(int accessCount) { this.accessCount = accessCount; }

    public int getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(int maxDownloads) { this.maxDownloads = maxDownloads; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getRecipientUsername() { return recipientUsername; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    /**
     * Kiểm tra phiên chia sẻ có còn hiệu lực hay không
     */
    public boolean isValid() {
        if (isRevoked) return false;
        if (expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()))) return false;
        if (maxDownloads > 0 && accessCount >= maxDownloads) return false;
        return true;
    }
}

