import server.db.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class MigratePhase4 {
    public static void main(String[] args) {
        System.out.println("=== CHẠY MIGRATION PHASE 4 CSDL ===");
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Thêm shared_with_user_id vào shared_files nếu chưa có
            try {
                stmt.executeUpdate("ALTER TABLE shared_files ADD COLUMN shared_with_user_id INT NULL AFTER owner_id");
                stmt.executeUpdate("ALTER TABLE shared_files ADD CONSTRAINT fk_share_recipient FOREIGN KEY (shared_with_user_id) REFERENCES users(user_id) ON DELETE CASCADE");
                System.out.println("[OK] Đã thêm cột shared_with_user_id vào shared_files.");
            } catch (Exception e) {
                System.out.println("[INFO] Cột shared_with_user_id đã tồn tại hoặc bỏ qua: " + e.getMessage());
            }

            // 2. Thêm max_downloads vào shared_files nếu chưa có
            try {
                stmt.executeUpdate("ALTER TABLE shared_files ADD COLUMN max_downloads INT NOT NULL DEFAULT 0 AFTER access_count");
                System.out.println("[OK] Đã thêm cột max_downloads vào shared_files.");
            } catch (Exception e) {
                System.out.println("[INFO] Cột max_downloads đã tồn tại hoặc bỏ qua: " + e.getMessage());
            }

            // 3. Mở rộng permission của shared_files thành VARCHAR(20)
            try {
                stmt.executeUpdate("ALTER TABLE shared_files MODIFY COLUMN permission VARCHAR(20) NOT NULL DEFAULT 'DOWNLOAD'");
                System.out.println("[OK] Đã mở rộng cột permission thành VARCHAR(20).");
            } catch (Exception e) {
                System.out.println("[INFO] Lỗi sửa permission: " + e.getMessage());
            }

            // 4. Mở rộng action_type của transfer_logs thành VARCHAR(50)
            try {
                stmt.executeUpdate("ALTER TABLE transfer_logs MODIFY COLUMN action_type VARCHAR(50) NOT NULL");
                System.out.println("[OK] Đã mở rộng action_type thành VARCHAR(50).");
            } catch (Exception e) {
                System.out.println("[INFO] Lỗi sửa action_type: " + e.getMessage());
            }

            System.out.println("=== MIGRATION PHASE 4 HOÀN TẤT THÀNH CÔNG ===");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

