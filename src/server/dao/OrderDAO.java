package server.dao;

import server.db.DBConnection;
import server.model.Order;
import server.model.Plan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Data Access Object cho bảng `orders`.
 */
public class OrderDAO {

    private final PlanDAO planDAO = new PlanDAO();

    /**
     * Tạo đơn hàng mới. Giá tiền và quyền lợi được lấy CHÍNH XÁC từ Database,
     * tuyệt đối không cho phép Client tự ý sửa đổi giá hoặc quota.
     */
    public Order createOrder(int userId, int planId, String paymentMethod) throws SQLException {
        Plan plan = planDAO.getPlanById(planId);
        if (plan == null || !plan.isActive()) {
            throw new IllegalArgumentException("Gói cước không tồn tại hoặc đã ngừng kích hoạt (Plan ID: " + planId + ")");
        }

        String transactionCode = "ORD-" + System.currentTimeMillis() + "-" + (1000 + new Random().nextInt(9000));
        String method = (paymentMethod != null && !paymentMethod.trim().isEmpty()) ? paymentMethod : "MOCK_BANKING";

        String sql = "INSERT INTO orders (user_id, plan_id, amount, payment_method, status, transaction_code) " +
                     "VALUES (?, ?, ?, ?, 'PENDING', ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, plan.getPlanId());
            ps.setDouble(3, plan.getPrice()); // Lấy giá từ DB
            ps.setString(4, method);
            ps.setString(5, transactionCode);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int orderId = rs.getInt(1);
                    return new Order(orderId, userId, plan.getPlanId(), plan.getPlanName(),
                            plan.getPrice(), method, "PENDING", transactionCode, new Timestamp(System.currentTimeMillis()));
                }
            }
        }
        return null;
    }

    public Order getOrderById(int orderId) {
        String sql = "SELECT o.order_id, o.user_id, o.plan_id, p.plan_name, o.amount, o.payment_method, " +
                     "o.status, o.transaction_code, o.created_at " +
                     "FROM orders o JOIN plans p ON o.plan_id = p.plan_id WHERE o.order_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error getOrderById: " + e.getMessage());
        }
        return null;
    }

    public Order getOrderByCode(String transactionCode) {
        String sql = "SELECT o.order_id, o.user_id, o.plan_id, p.plan_name, o.amount, o.payment_method, " +
                     "o.status, o.transaction_code, o.created_at " +
                     "FROM orders o JOIN plans p ON o.plan_id = p.plan_id WHERE o.transaction_code = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error getOrderByCode: " + e.getMessage());
        }
        return null;
    }

    public List<Order> getUserOrders(int userId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.order_id, o.user_id, o.plan_id, p.plan_name, o.amount, o.payment_method, " +
                     "o.status, o.transaction_code, o.created_at " +
                     "FROM orders o JOIN plans p ON o.plan_id = p.plan_id WHERE o.user_id = ? ORDER BY o.order_id DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error getUserOrders: " + e.getMessage());
        }
        return list;
    }

    /**
     * Hủy đơn hàng đang ở trạng thái PENDING.
     * Chống IDOR: Chỉ chủ sở hữu đơn hàng mới có thể hủy.
     */
    public boolean cancelOrder(int orderId, int userId) {
        String sql = "UPDATE orders SET status = 'CANCELLED' WHERE order_id = ? AND user_id = ? AND status = 'PENDING'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error cancelOrder: " + e.getMessage());
        }
        return false;
    }

    public void updateOrderStatus(Connection conn, int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    private Order mapResultSet(ResultSet rs) throws SQLException {
        return new Order(
                rs.getInt("order_id"),
                rs.getInt("user_id"),
                rs.getInt("plan_id"),
                rs.getString("plan_name"),
                rs.getDouble("amount"),
                rs.getString("payment_method"),
                rs.getString("status"),
                rs.getString("transaction_code"),
                rs.getTimestamp("created_at")
        );
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.order_id, o.user_id, o.plan_id, p.plan_name, o.amount, o.payment_method, " +
                     "o.status, o.transaction_code, o.created_at " +
                     "FROM orders o JOIN plans p ON o.plan_id = p.plan_id ORDER BY o.order_id DESC LIMIT 100";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error getAllOrders: " + e.getMessage());
        }
        return list;
    }

}
