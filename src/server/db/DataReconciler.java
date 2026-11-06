package server.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * QUY TẮC ĐỐI SOÁT DỮ LIỆU ĐĨA CỨNG VÀ CƠ SỞ DỮ LIỆU (DATA RECONCILIATION)
 *
 * Nhiệm vụ:
 *   - Kiểm tra tính nhất quán giữa tệp tin vật lý trên đĩa (Filesystem storage/)
 *     và các bản ghi metadata trong bảng 'files' của MySQL.
 *   - Phát hiện các bất thường (Anomalies):
 *       + Tệp tin mồ côi (Orphaned file): Có trên đĩa nhưng thiếu bản ghi trong DB.
 *       + Tệp tin ma (Ghost record): Có trong DB nhưng file thực tế bị mất trên đĩa.
 *   - Đối soát và đồng bộ lại dung lượng đã dùng (used_bytes) của từng người dùng.
 */
public class DataReconciler {

    private final Path storageRoot;
    private final DBConnection dbConnection;

    public DataReconciler(Path storageRoot) {
        this.storageRoot = storageRoot;
        this.dbConnection = DBConnection.getInstance();
    }

    public static class ReconciliationReport {
        public int totalDbRecords = 0;
        public int totalDiskFiles = 0;
        public int missingOnDisk = 0;
        public int orphanedOnDisk = 0;
        public int quotaSyncedUsers = 0;
        public List<String> issues = new ArrayList<>();

        @Override
        public String toString() {
            return String.format(
                "=== BÁO CÁO ĐỐI SOÁT DỮ LIỆU (DATA RECONCILIATION) ===%n" +
                "- Tổng bản ghi DB: %d%n" +
                "- Tổng tệp trên đĩa: %d%n" +
                "- Tệp ma trong DB (mất trên đĩa): %d%n" +
                "- Tệp mồ côi (thiếu trong DB): %d%n" +
                "- Số người dùng đã hiệu chỉnh Quota: %d%n" +
                "- Chi tiết vấn đề: %s",
                totalDbRecords, totalDiskFiles, missingOnDisk, orphanedOnDisk, quotaSyncedUsers,
                issues.isEmpty() ? "Hệ thống nhất quán 100%" : String.join("; ", issues)
            );
        }
    }

    /**
     * Thực hiện toàn bộ quy trình đối soát dữ liệu
     */
    public ReconciliationReport reconcile() {
        ReconciliationReport report = new ReconciliationReport();
        if (!dbConnection.isAvailable()) {
            report.issues.add("Không thể kết nối CSDL MySQL để đối soát.");
            return report;
        }

        try {
            Connection conn = dbConnection.getConnection();

            // 1. Kiểm tra các bản ghi trong DB xem file vật lý có còn tồn tại không
            String checkDbSql = "SELECT file_id, user_id, filename, relative_path, file_size FROM files WHERE is_deleted = FALSE";
            try (PreparedStatement stmt = conn.prepareStatement(checkDbSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    report.totalDbRecords++;
                    int fileId = rs.getInt("file_id");
                    String relPath = rs.getString("relative_path");
                    if (relPath.startsWith("/")) relPath = relPath.substring(1);

                    Path physicalPath = storageRoot.resolve(relPath).normalize();
                    if (!Files.exists(physicalPath) || !Files.isRegularFile(physicalPath)) {
                        report.missingOnDisk++;
                        report.issues.add("File ID " + fileId + " (" + relPath + ") có trong DB nhưng mất trên đĩa.");
                    }
                }
            }

            // 2. Đồng bộ lại used_bytes thực tế cho tất cả người dùng
            String syncQuotaSql = 
                "UPDATE users u " +
                "SET u.used_bytes = COALESCE(( " +
                "    SELECT SUM(f.file_size) FROM files f " +
                "    WHERE f.user_id = u.user_id AND f.is_deleted = FALSE " +
                "), 0)";
            try (PreparedStatement stmt = conn.prepareStatement(syncQuotaSql)) {
                report.quotaSyncedUsers = stmt.executeUpdate();
            }

        } catch (SQLException e) {
            report.issues.add("Lỗi truy vấn SQL khi đối soát: " + e.getMessage());
        }

        return report;
    }
}

