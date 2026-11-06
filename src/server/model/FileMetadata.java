package server.model;

import java.sql.Timestamp;

/**
 * THỰC THỂ TỆP TIN (FILE METADATA ENTITY)
 *
 * Ánh xạ tới bảng 'files' trong MySQL.
 * Quản lý thông tin metadata, kích thước và mã băm SHA-256 kiểm tra toàn vẹn.
 */
public class FileMetadata {

    private int       fileId;
    private int       userId;
    private String    filename;
    private String    relativePath; // Ví dụ: "/tailieu.docx" hoặc "/docs/report.pdf"
    private long      fileSize;
    private String    sha256Hash;
    private boolean   isDeleted;
    private Timestamp uploadedAt;
    private Timestamp updatedAt;

    public FileMetadata() {}

    public FileMetadata(int fileId, int userId, String filename, String relativePath,
                        long fileSize, String sha256Hash, boolean isDeleted,
                        Timestamp uploadedAt, Timestamp updatedAt) {
        this.fileId       = fileId;
        this.userId       = userId;
        this.filename     = filename;
        this.relativePath = relativePath;
        this.fileSize     = fileSize;
        this.sha256Hash   = sha256Hash;
        this.isDeleted    = isDeleted;
        this.uploadedAt   = uploadedAt;
        this.updatedAt    = updatedAt;
    }

    // Getters and Setters
    public int       getFileId()                    { return fileId; }
    public void      setFileId(int fileId)          { this.fileId = fileId; }

    public int       getUserId()                    { return userId; }
    public void      setUserId(int userId)          { this.userId = userId; }

    public String    getFilename()                  { return filename; }
    public void      setFilename(String filename)   { this.filename = filename; }

    public String    getRelativePath()              { return relativePath; }
    public void      setRelativePath(String r)      { this.relativePath = r; }

    public long      getFileSize()                  { return fileSize; }
    public void      setFileSize(long fileSize)     { this.fileSize = fileSize; }

    public String    getSha256Hash()                { return sha256Hash; }
    public void      setSha256Hash(String hash)     { this.sha256Hash = hash; }

    public boolean   isDeleted()                    { return isDeleted; }
    public void      setDeleted(boolean deleted)    { isDeleted = deleted; }

    public Timestamp getUploadedAt()                { return uploadedAt; }
    public void      setUploadedAt(Timestamp u)     { this.uploadedAt = u; }

    public Timestamp getUpdatedAt()                 { return updatedAt; }
    public void      setUpdatedAt(Timestamp u)      { this.updatedAt = u; }

    @Override
    public String toString() {
        return String.format("FileMetadata[ID=%d, UserID=%d, Path='%s', Size=%d bytes, Hash=%s]",
                fileId, userId, relativePath, fileSize, (sha256Hash != null ? sha256Hash.substring(0, 8) + "..." : "null"));
    }
}

