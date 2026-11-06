package server.dao;

import server.db.DBConnection;
import server.model.Order;
import server.model.Payment;
import server.model.Plan;
import server.model.Subscription;

import java.sql.*;
import java.util.Random;

/**
 * Data Access Object và Service điều phối Giao dịch thanh toán mô phỏng (Mock Payment).
 * Đảm bảo tính toàn vẹn ACID (Transaction), Idempotency (chống nạp trùng),
 * và Server-side validation (Client không thể can thiệp giá hay quota).
 */
public class PaymentDAO {

    public static class PaymentResult {
        public boolean success;
        public String message;
        public String paymentCode;
        public Order order;
        public Subscription subscription;

        public PaymentResult(boolean success, String message, String paymentCode, Order order, Subscription subscription) {
            this.success = success;
            this.message = message;
            this.paymentCode = paymentCode;
            this.order = order;
            this.subscription = subscription;
        }
    }

    /**
     * Xử lý thanh toán mô phỏng trong một JDBC Transaction duy nhất.
     */
    public PaymentResult processMockPayment(int orderId, int userId, boolean simulateSuccess, String mockMethod) {
        String method = (mockMethod != null && !mockMethod.trim().isEmpty()) ? mockMethod : "MOCK_BANKING";
        String paymentCode = "PAY-" + System.currentTimeMillis() + "-" + (1000 + new Random().nextInt(9000));

        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // BẮT ĐẦU TRANSACTION

            // 1. Kiểm tra đơn hàng kèm khóa bản ghi (Pessimistic Locking FOR UPDATE)
            String queryOrderSql = "SELECT o.order_id, o.user_id, o.plan_id, p.plan_name, p.max_quota_bytes, " +
                                   "p.duration_days, o.amount, o.status, o.transaction_code, o.created_at " +
                                   "FROM orders o JOIN plans p ON o.plan_id = p.plan_id " +
                                   "WHERE o.order_id = ? FOR UPDATE";

            Order order = null;
            long maxQuotaBytes = 0;
            int durationDays = 30;

            try (PreparedStatement psOrder = conn.prepareStatement(queryOrderSql)) {
                psOrder.setInt(1, orderId);
                try (ResultSet rs = psOrder.executeQuery()) {
                    if (rs.next()) {
                        order = new Order(
                                rs.getInt("order_id"),
                                rs.getInt("user_id"),
                                rs.getInt("plan_id"),
                                rs.getString("plan_name"),
                                rs.getDouble("amount"),
                                method,
                                rs.getString("status"),
                                rs.getString("transaction_code"),
                                rs.getTimestamp("created_at")
                        );
                        maxQuotaBytes = rs.getLong("max_quota_bytes");
                        durationDays = rs.getInt("duration_days");
                    }
                }
            }

            if (order == null) {
                conn.rollback();
                return new PaymentResult(false, "Đơn hàng không tồn tại (Order ID: " + orderId + ")", null, null, null);
            }

            // Kiểm tra phân quyền: Đơn hàng phải thuộc về chính User này
            if (order.getUserId() != userId) {
                conn.rollback();
                return new PaymentResult(false, "Từ chối truy cập: Đơn hàng không thuộc về tài khoản của bạn (Anti-IDOR)", null, null, null);
            }

            // IDEMPOTENCY CHECK: Đơn hàng đã COMPLETED từ trước thì không trừ tiền/kích hoạt trùng
            if ("COMPLETED".equalsIgnoreCase(order.getStatus())) {
                conn.rollback();
                return new PaymentResult(false, "Đơn hàng đã được thanh toán thành công trước đó (Idempotent: Quyền lợi đã kích hoạt, không xử lý lại)", null, order, null);
            }

            if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
                conn.rollback();
                return new PaymentResult(false, "Đơn hàng này đã bị hủy, không thể tiếp tục thanh toán", null, order, null);
            }

            // 2. Kịch bản THANH TOÁN THẤT BẠI (Mô phỏng từ chối thẻ / số dư không đủ)
            if (!simulateSuccess) {
                String insertPayFail = "INSERT INTO payments (order_id, payment_code, amount, payment_method, status, raw_response) " +
                                       "VALUES (?, ?, ?, ?, 'FAILED', 'Simulated Decline: The bank declined the transaction')";
                try (PreparedStatement psPay = conn.prepareStatement(insertPayFail)) {
                    psPay.setInt(1, orderId);
                    psPay.setString(2, paymentCode);
                    psPay.setDouble(3, order.getAmount());
                    psPay.setString(4, method);
                    psPay.executeUpdate();
                }

                String updateOrderFail = "UPDATE orders SET status = 'FAILED' WHERE order_id = ?";
                try (PreparedStatement psOrd = conn.prepareStatement(updateOrderFail)) {
                    psOrd.setInt(1, orderId);
                    psOrd.executeUpdate();
                }

                conn.commit();
                order.setStatus("FAILED");
                return new PaymentResult(false, "Thanh toán mô phỏng thất bại: Giao dịch bị ngân hàng từ chối (Mã GD: " + paymentCode + ")", paymentCode, order, null);
            }

            // 3. Kịch bản THANH TOÁN THÀNH CÔNG (Mock Payment SUCCESS)
            // 3.1. Ghi bản ghi thanh toán (payments)
            String insertPaySuccess = "INSERT INTO payments (order_id, payment_code, amount, payment_method, status, raw_response) " +
                                      "VALUES (?, ?, ?, ?, 'SUCCESS', 'Mock Payment approved 200 OK')";
            try (PreparedStatement psPay = conn.prepareStatement(insertPaySuccess)) {
                psPay.setInt(1, orderId);
                psPay.setString(2, paymentCode);
                psPay.setDouble(3, order.getAmount());
                psPay.setString(4, method);
                psPay.executeUpdate();
            }

            // 3.2. Cập nhật trạng thái đơn hàng sang COMPLETED
            String updateOrderSuccess = "UPDATE orders SET status = 'COMPLETED' WHERE order_id = ?";
            try (PreparedStatement psOrd = conn.prepareStatement(updateOrderSuccess)) {
                psOrd.setInt(1, orderId);
                psOrd.executeUpdate();
            }
            order.setStatus("COMPLETED");

            // 3.3. Tính toán thời hạn Đăng ký (Gia hạn nối tiếp nếu gói đang ACTIVE)
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Timestamp startDate = now;
            Timestamp endDate;

            // Tìm gói ACTIVE hiện tại của user để kiểm tra gia hạn
            String findActiveSub = "SELECT sub_id, plan_id, end_date FROM subscriptions " +
                                   "WHERE user_id = ? AND status = 'ACTIVE' AND end_date > NOW() " +
                                   "ORDER BY end_date DESC LIMIT 1";
            Timestamp existingEndDate = null;
            int existingPlanId = -1;

            try (PreparedStatement psActive = conn.prepareStatement(findActiveSub)) {
                psActive.setInt(1, userId);
                try (ResultSet rs = psActive.executeQuery()) {
                    if (rs.next()) {
                        existingPlanId = rs.getInt("plan_id");
                        existingEndDate = rs.getTimestamp("end_date");
                    }
                }
            }

            long durationMs = durationDays * 24L * 60 * 60 * 1000L;

            if (existingEndDate != null && existingEndDate.after(now) && existingPlanId == order.getPlanId()) {
                // Gia hạn cùng gói: Cộng dồn thời hạn tiếp theo
                endDate = new Timestamp(existingEndDate.getTime() + durationMs);
            } else {
                // Nâng cấp gói mới hoặc gói cũ đã hết hạn: Tính từ thời điểm hiện tại
                endDate = new Timestamp(now.getTime() + durationMs);
            }

            // 3.4. Lưu trữ gói cũ sang UPGRADED
            String archiveOld = "UPDATE subscriptions SET status = 'UPGRADED' WHERE user_id = ? AND status = 'ACTIVE'";
            try (PreparedStatement psArchive = conn.prepareStatement(archiveOld)) {
                psArchive.setInt(1, userId);
                psArchive.executeUpdate();
            }

            // 3.5. Tạo gói Subscription mới
            String insertSub = "INSERT INTO subscriptions (user_id, plan_id, start_date, end_date, status) VALUES (?, ?, ?, ?, 'ACTIVE')";
            int newSubId = -1;
            try (PreparedStatement psSub = conn.prepareStatement(insertSub, Statement.RETURN_GENERATED_KEYS)) {
                psSub.setInt(1, userId);
                psSub.setInt(2, order.getPlanId());
                psSub.setTimestamp(3, startDate);
                psSub.setTimestamp(4, endDate);
                psSub.executeUpdate();
                try (ResultSet rs = psSub.getGeneratedKeys()) {
                    if (rs.next()) newSubId = rs.getInt(1);
                }
            }

            Subscription sub = new Subscription(newSubId, userId, order.getPlanId(), order.getPlanName(), startDate, endDate, "ACTIVE");

            // 3.6. Cập nhật hạn mức Quota mới cho User trong bảng users
            String updateUserQuota = "UPDATE users SET quota_bytes = ? WHERE user_id = ?";
            try (PreparedStatement psUser = conn.prepareStatement(updateUserQuota)) {
                psUser.setLong(1, maxQuotaBytes);
                psUser.setInt(2, userId);
                psUser.executeUpdate();
            }

            // 3.7. COMMIT TOÀN BỘ TRANSACTION
            conn.commit();

            String msg = String.format("Thanh toán thành công! Đã kích hoạt gói %s (%s) có hiệu lực đến %s.",
                    order.getPlanName(), formatBytes(maxQuotaBytes), endDate.toString());
            return new PaymentResult(true, msg, paymentCode, order, sub);

        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            System.err.println("[PaymentDAO] Transaction rollback error: " + ex.getMessage());
            return new PaymentResult(false, "Lỗi cơ sở dữ liệu khi thanh toán: " + ex.getMessage(), null, null, null);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return String.format("%.1f GB", (double) bytes / (1024 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%d MB", bytes / (1024 * 1024));
        } else {
            return String.format("%d KB", bytes / 1024);
        }
    }
}

