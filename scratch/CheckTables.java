
import java.sql.*;
import server.db.DBConnection;

public class CheckTables {
    public static void main(String[] args) throws Exception {
        Connection conn = DBConnection.getInstance().getConnection();
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getTables("custom_ftp_db", null, "%", new String[]{"TABLE"});
        while (rs.next()) {
            System.out.println("TABLE: " + rs.getString("TABLE_NAME"));
        }
        
        System.out.println("--- CHECKING COLUMNS FOR orders ---");
        ResultSet rsc = md.getColumns("custom_ftp_db", null, "orders", "%");
        while (rsc.next()) {
            System.out.println("  COL: " + rsc.getString("COLUMN_NAME") + " (" + rsc.getString("TYPE_NAME") + ")");
        }

        System.out.println("--- CHECKING COLUMNS FOR subscriptions ---");
        rsc = md.getColumns("custom_ftp_db", null, "subscriptions", "%");
        while (rsc.next()) {
            System.out.println("  COL: " + rsc.getString("COLUMN_NAME") + " (" + rsc.getString("TYPE_NAME") + ")");
        }

        System.out.println("--- CHECKING PLANS DATA ---");
        Statement st = conn.createStatement();
        ResultSet rsp = st.executeQuery("SELECT * FROM plans");
        while (rsp.next()) {
            System.out.println("  PLAN: " + rsp.getInt("plan_id") + " - " + rsp.getString("plan_name") + " - " + rsp.getLong("max_quota_bytes") + "B - " + rsp.getDouble("price") + "VND");
        }
    }
}
