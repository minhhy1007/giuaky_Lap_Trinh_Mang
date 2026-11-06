import server.db.DBConnection;
import java.sql.*;

public class CheckSchema {
    public static void main(String[] args) throws Exception {
        Connection conn = DBConnection.getInstance().getConnection();
        ResultSet rs = conn.getMetaData().getColumns("custom_ftp_db", null, "transfer_logs", null);
        System.out.println("COLUMNS IN transfer_logs:");
        while (rs.next()) {
            System.out.println(" - " + rs.getString("COLUMN_NAME") + " (" + rs.getString("TYPE_NAME") + ")");
        }
    }
}
