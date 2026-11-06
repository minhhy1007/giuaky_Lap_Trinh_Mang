package server.dao;

import server.db.DBConnection;
import server.model.Subscription;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bảng `subscriptions`.
 */
public class SubscriptionDAO {

    public Subscription getActiveSubscription(int userId) {
        String sql = "SELECT s.sub_id, s.user_id, s.plan_id, p.plan_name, s.start_date, s.end_date, s.status " +
                     "FROM subscriptions s " +
                     "JOIN plans p ON s.plan_id = p.plan_id " +
                     "WHERE s.user_id = ? AND s.status = 'ACTIVE' AND s.end_date > NOW() " +
                     "ORDER BY s.end_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SubscriptionDAO] Error getActiveSubscription: " + e.getMessage());
        }
        return null;
    }

    public List<Subscription> getUserSubscriptions(int userId) {
        List<Subscription> list = new ArrayList<>();
        String sql = "SELECT s.sub_id, s.user_id, s.plan_id, p.plan_name, s.start_date, s.end_date, s.status " +
                     "FROM subscriptions s " +
                     "JOIN plans p ON s.plan_id = p.plan_id " +
                     "WHERE s.user_id = ? " +
                     "ORDER BY s.sub_id DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SubscriptionDAO] Error getUserSubscriptions: " + e.getMessage());
        }
        return list;
    }

    public int createSubscription(Connection conn, Subscription sub) throws SQLException {
        String sql = "INSERT INTO subscriptions (user_id, plan_id, start_date, end_date, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sub.getUserId());
            ps.setInt(2, sub.getPlanId());
            ps.setTimestamp(3, sub.getStartDate());
            ps.setTimestamp(4, sub.getEndDate());
            ps.setString(5, sub.getStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    sub.setSubId(rs.getInt(1));
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public void archiveActiveSubscriptions(Connection conn, int userId, String archiveStatus) throws SQLException {
        String sql = "UPDATE subscriptions SET status = ? WHERE user_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, archiveStatus);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Quét và xử lý các gói cước đã hết hạn (end_date < NOW() và status = 'ACTIVE').
     * Giáng cấp hạn mức về FREE (100MB) nhưng TUYỆT ĐỐI KHÔNG xóa file của người dùng.
     */
    public int checkAndExpireSubscriptions() {
        int count = 0;
        String findExpiredSql = "SELECT s.sub_id, s.user_id, s.plan_id, u.username " +
                                "FROM subscriptions s " +
                                "JOIN users u ON s.user_id = u.user_id " +
                                "WHERE s.status = 'ACTIVE' AND s.end_date <= NOW()";

        String updateSubSql = "UPDATE subscriptions SET status = 'EXPIRED' WHERE sub_id = ?";
        String resetQuotaSql = "UPDATE users SET quota_bytes = 104857600 WHERE user_id = ?"; // 100MB FREE

        try (Connection conn = DBConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            List<int[]> expiredList = new ArrayList<>(); // [sub_id, user_id]

            try (PreparedStatement psFind = conn.prepareStatement(findExpiredSql);
                 ResultSet rs = psFind.executeQuery()) {
                while (rs.next()) {
                    expiredList.add(new int[]{rs.getInt("sub_id"), rs.getInt("user_id")});
                }
            }

            if (!expiredList.isEmpty()) {
                try (PreparedStatement psSub = conn.prepareStatement(updateSubSql);
                     PreparedStatement psUser = conn.prepareStatement(resetQuotaSql)) {
                    for (int[] item : expiredList) {
                        psSub.setInt(1, item[0]);
                        psSub.addBatch();

                        psUser.setInt(1, item[1]);
                        psUser.addBatch();
                    }
                    psSub.executeBatch();
                    psUser.executeBatch();
                }
                conn.commit();
                count = expiredList.size();
                System.out.println("[SubscriptionDAO] Đã xử lý hết hạn cho " + count + " gói cước.");
            } else {
                conn.commit();
            }
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            System.err.println("[SubscriptionDAO] Error checkAndExpireSubscriptions: " + e.getMessage());
        }
        return count;
    }

    private Subscription mapResultSet(ResultSet rs) throws SQLException {
        return new Subscription(
                rs.getInt("sub_id"),
                rs.getInt("user_id"),
                rs.getInt("plan_id"),
                rs.getString("plan_name"),
                rs.getTimestamp("start_date"),
                rs.getTimestamp("end_date"),
                rs.getString("status")
        );
    }
}

