
import java.sql.*;
import server.db.DBConnection;

public class UpdateSchemaPhase5 {
    public static void main(String[] args) throws Exception {
        Connection conn = DBConnection.getInstance().getConnection();
        Statement st = conn.createStatement();
        
        System.out.println("Updating orders status ENUM to include CANCELLED...");
        try {
            st.executeUpdate("ALTER TABLE orders MODIFY COLUMN status ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING'");
            System.out.println("  [OK] orders.status altered");
        } catch (Exception e) {
            System.out.println("  [INFO] " + e.getMessage());
        }

        System.out.println("Updating subscriptions status ENUM to include UPGRADED...");
        try {
            st.executeUpdate("ALTER TABLE subscriptions MODIFY COLUMN status ENUM('ACTIVE', 'EXPIRED', 'CANCELLED', 'UPGRADED') NOT NULL DEFAULT 'ACTIVE'");
            System.out.println("  [OK] subscriptions.status altered");
        } catch (Exception e) {
            System.out.println("  [INFO] " + e.getMessage());
        }

        System.out.println("Creating payments table if not exists...");
        String createPayments = "CREATE TABLE IF NOT EXISTS payments (" +
                "payment_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "order_id INT NOT NULL, " +
                "payment_code VARCHAR(100) UNIQUE NOT NULL, " +
                "amount DECIMAL(12,2) NOT NULL, " +
                "payment_method VARCHAR(50) NOT NULL DEFAULT 'MOCK_BANKING', " +
                "status ENUM('SUCCESS', 'FAILED') NOT NULL, " +
                "raw_response VARCHAR(255) NULL, " +
                "paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE, " +
                "INDEX idx_payments_order (order_id), " +
                "INDEX idx_payments_code (payment_code)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
        st.executeUpdate(createPayments);
        System.out.println("  [OK] payments table ready");
        
        // Also check if any seed subscriptions exist for users
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM subscriptions");
        rs.next();
        System.out.println("Total existing subscriptions: " + rs.getInt(1));
    }
}
