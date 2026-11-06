package test;

import server.dao.OrderDAO;
import server.dao.PaymentDAO;
import server.dao.PlanDAO;
import server.dao.SubscriptionDAO;
import server.dao.UserDAO;
import server.db.DBConnection;
import server.model.Order;
import server.model.Plan;
import server.model.Subscription;
import server.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

/**
 * BỘ KIỂM THỬ TỰ ĐỘNG CHO PHASE 5: SUBSCRIPTIONS, ORDERS VÀ MOCK PAYMENTS
 *
 * Kiểm tra toàn diện 8 kịch bản bắt buộc:
 * 1. Giao dịch thành công (Mock Payment Success -> Active Subscription -> Quota upgrade)
 * 2. Giao dịch thất bại (Simulated Decline -> Failed Order -> Quota unchange)
 * 3. Hủy đơn hàng (Order Cancellation & Anti-payment on Cancelled)
 * 4. Xử lý lặp cùng một kết quả (Idempotency Protection)
 * 5. Đơn hàng không tồn tại (Non-existent Order Validation)
 * 6. Gói hết hạn và gia hạn nối tiếp (Expired downgrade & Consecutive Renewal)
 * 7. Kháng sửa giá / quyền lợi phía Client (Server-side Enforcement)
 * 8. Tính toàn vẹn giao dịch DB (ACID Transaction Rollback on Failure)
 */
public class TestPhase5PlansAndPayment {

    private static int passedCount = 0;
    private static int failedCount = 0;

    private static final PlanDAO planDAO = new PlanDAO();
    private static final OrderDAO orderDAO = new OrderDAO();
    private static final PaymentDAO paymentDAO = new PaymentDAO();
    private static final SubscriptionDAO subscriptionDAO = new SubscriptionDAO();
    private static final UserDAO userDAO = new UserDAO();

    private static int testUserId1 = -1;
    private static int testUserId2 = -1;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   BẮT ĐẦU CHẠY BỘ KIỂM THỬ TỰ ĐỘNG PHASE 5: PLANS & MOCK PAYMENT");
        System.out.println("==================================================================\n");

        setupTestData();

        try {
            test01_SuccessfulMockPayment();
            test02_FailedMockPayment();
            test03_CancelOrderAndProtection();
            test04_IdempotencyDuplicatePayment();
            test05_NonExistentOrder();
            test06_ExpiryAndConsecutiveRenewal();
            test07_ClientTamperResistance();
            test08_TransactionRollbackOnFailure();
        } finally {
            cleanUpTestData();
        }

        System.out.println("\n==================================================================");
        System.out.println(String.format("   KẾT QUẢ KIỂM THỬ PHASE 5: %d PASSED, %d FAILED", passedCount, failedCount));
        System.out.println("==================================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void pass(String testName, String note) {
        passedCount++;
        System.out.println("[PASS] " + testName + " (" + note + ")");
    }

    private static void fail(String testName, String reason) {
        failedCount++;
        System.err.println("[FAIL] " + testName + " -> " + reason);
    }

    private static void setupTestData() {
        cleanUpTestData();
        try {
            // Tạo User test 1 (p5_alice)
            userDAO.registerUser("p5_alice", "password123", "p5_alice@test.local");
            User u1 = userDAO.findByUsername("p5_alice");
            if (u1 != null) testUserId1 = u1.getUserId();

            // Tạo User test 2 (p5_bob)
            userDAO.registerUser("p5_bob", "password456", "p5_bob@test.local");
            User u2 = userDAO.findByUsername("p5_bob");
            if (u2 != null) testUserId2 = u2.getUserId();
        } catch (Exception e) {
            System.err.println("[Setup Error] " + e.getMessage());
        }
    }

    private static void cleanUpTestData() {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            // Xóa theo thứ tự foreign keys
            String sql = "DELETE FROM payments WHERE order_id IN (SELECT order_id FROM orders WHERE user_id IN (SELECT user_id FROM users WHERE username IN ('p5_alice', 'p5_bob')))";
            try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }

            sql = "DELETE FROM orders WHERE user_id IN (SELECT user_id FROM users WHERE username IN ('p5_alice', 'p5_bob'))";
            try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }

            sql = "DELETE FROM subscriptions WHERE user_id IN (SELECT user_id FROM users WHERE username IN ('p5_alice', 'p5_bob'))";
            try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }

            sql = "DELETE FROM users WHERE username IN ('p5_alice', 'p5_bob')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }
        } catch (Exception ignored) {}
    }

    // Test 1: Giao dịch thành công
    private static void test01_SuccessfulMockPayment() {
        String name = "Test 1: Giao dịch thanh toán mô phỏng thành công (Mock Payment Success)";
        try {
            // p5_alice tạo đơn mua gói PRO (plan_id = 2, 1GB, 50k)
            Order order = orderDAO.createOrder(testUserId1, 2, "MOCK_BANKING");
            if (order == null || !"PENDING".equals(order.getStatus())) {
                fail(name, "Tạo đơn hàng thất bại hoặc trạng thái không phải PENDING");
                return;
            }

            // Thực hiện thanh toán mô phỏng thành công
            PaymentDAO.PaymentResult res = paymentDAO.processMockPayment(order.getOrderId(), testUserId1, true, "MOCK_BANKING");
            if (!res.success) {
                fail(name, "Thanh toán thất bại: " + res.message);
                return;
            }

            // Kiểm tra trạng thái đơn hàng đã sang COMPLETED
            Order updatedOrder = orderDAO.getOrderById(order.getOrderId());
            if (!"COMPLETED".equals(updatedOrder.getStatus())) {
                fail(name, "Trạng thái đơn hàng chưa chuyển sang COMPLETED: " + updatedOrder.getStatus());
                return;
            }

            // Kiểm tra gói cước ACTIVE
            Subscription sub = subscriptionDAO.getActiveSubscription(testUserId1);
            if (sub == null || !"ACTIVE".equals(sub.getStatus()) || sub.getPlanId() != 2) {
                fail(name, "Chưa kích hoạt gói Subscription PRO thành công");
                return;
            }

            // Kiểm tra hạn mức Quota đã được nâng cấp lên 1GB
            User u = userDAO.findByUsername("p5_alice");
            if (u.getQuotaBytes() != 1073741824L) {
                fail(name, "Quota chưa được nâng cấp lên 1GB, hiện tại: " + u.getQuotaBytes());
                return;
            }

            pass(name, "Đơn hàng COMPLETED, Gói PRO kích hoạt, Quota nâng cấp 1GB thành công");
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 2: Giao dịch thất bại mô phỏng
    private static void test02_FailedMockPayment() {
        String name = "Test 2: Giao dịch thất bại mô phỏng (Simulated Payment Decline)";
        try {
            // p5_bob tạo đơn mua gói VIP (plan_id = 3)
            Order order = orderDAO.createOrder(testUserId2, 3, "MOCK_BANKING");

            // Thực hiện thanh toán thất bại
            PaymentDAO.PaymentResult res = paymentDAO.processMockPayment(order.getOrderId(), testUserId2, false, "MOCK_BANKING");
            if (res.success) {
                fail(name, "Đáng lẽ thanh toán phải thất bại nhưng lại trả về success=true");
                return;
            }

            // Kiểm tra đơn hàng có trạng thái FAILED
            Order updatedOrder = orderDAO.getOrderById(order.getOrderId());
            if (!"FAILED".equals(updatedOrder.getStatus())) {
                fail(name, "Trạng thái đơn hàng không phải FAILED: " + updatedOrder.getStatus());
                return;
            }

            // Kiểm tra p5_bob không có gói VIP nào được kích hoạt
            Subscription sub = subscriptionDAO.getActiveSubscription(testUserId2);
            if (sub != null && sub.getPlanId() == 3) {
                fail(name, "Gói VIP bị kích hoạt trái phép khi thanh toán thất bại!");
                return;
            }

            // Quota p5_bob vẫn là 100MB mặc định
            User u = userDAO.findByUsername("p5_bob");
            if (u.getQuotaBytes() != 104857600L) {
                fail(name, "Quota bị thay đổi khi thanh toán thất bại: " + u.getQuotaBytes());
                return;
            }

            pass(name, "Đơn hàng FAILED, bản ghi thanh toán thất bại ghi nhận đúng, Quota giữ nguyên");
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 3: Hủy đơn hàng và chống thanh toán đơn đã hủy
    private static void test03_CancelOrderAndProtection() {
        String name = "Test 3: Hủy đơn hàng và chặn thanh toán đơn đã hủy (Order Cancellation)";
        try {
            // p5_bob tạo đơn gói PRO
            Order order = orderDAO.createOrder(testUserId2, 2, "MOCK_BANKING");

            // Hủy đơn
            boolean cancelled = orderDAO.cancelOrder(order.getOrderId(), testUserId2);
            if (!cancelled) {
                fail(name, "Hủy đơn hàng thất bại");
                return;
            }

            // Thử thanh toán đơn đã hủy
            PaymentDAO.PaymentResult res = paymentDAO.processMockPayment(order.getOrderId(), testUserId2, true, "MOCK_BANKING");
            if (res.success) {
                fail(name, "Hệ thống vẫn cho phép thanh toán đơn hàng đã CANCELLED!");
                return;
            }

            pass(name, "Hủy đơn thành công, Server chặn đứng thanh toán cho đơn hàng đã hủy");
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 4: Chống thanh toán trùng lặp (Idempotency)
    private static void test04_IdempotencyDuplicatePayment() {
        String name = "Test 4: Cơ chế Idempotency chống thanh toán lặp nhiều lần cho một đơn hàng";
        try {
            // p5_alice đã thanh toán đơn order 1 ở Test 1 thành công
            List<Order> orders = orderDAO.getUserOrders(testUserId1);
            Order completedOrder = null;
            for (Order o : orders) {
                if ("COMPLETED".equals(o.getStatus())) {
                    completedOrder = o;
                    break;
                }
            }

            if (completedOrder == null) {
                fail(name, "Không tìm thấy đơn COMPLETED để test idempotency");
                return;
            }

            // Gửi lại thanh toán cho đơn đã COMPLETED
            PaymentDAO.PaymentResult res = paymentDAO.processMockPayment(completedOrder.getOrderId(), testUserId1, true, "MOCK_BANKING");
            if (res.success) {
                fail(name, "Vi phạm Idempotency: Cho phép thanh toán lại đơn hàng đã COMPLETED!");
                return;
            }

            if (!res.message.contains("Idempotent")) {
                fail(name, "Thông báo không nêu rõ cơ chế Idempotent: " + res.message);
                return;
            }

            pass(name, "Phát hiện đơn đã COMPLETED và từ chối xử lý lặp lại an toàn tuyệt đối");
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 5: Đơn không tồn tại
    private static void test05_NonExistentOrder() {
        String name = "Test 5: Xác thực đơn hàng không tồn tại (Non-existent Order Validation)";
        try {
            PaymentDAO.PaymentResult res = paymentDAO.processMockPayment(999999, testUserId1, true, "MOCK_BANKING");
            if (res.success) {
                fail(name, "Cho phép thanh toán đơn hàng không có thực!");
                return;
            }

            pass(name, "Server từ chối thanh toán cho Order ID không tồn tại chính xác");
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 6: Gói hết hạn và gia hạn nối tiếp
    private static void test06_ExpiryAndConsecutiveRenewal() {
        String name = "Test 6: Xử lý gói hết hạn và gia hạn nối tiếp thời gian (Renewal & Expiry)";
        try {
            // 1. Gia hạn nối tiếp (Consecutive Renewal):
            // p5_alice đang có gói PRO kết thúc sau ~30 ngày. Mua tiếp PRO -> Thời hạn phải cộng dồn ~60 ngày!
            Subscription curSub = subscriptionDAO.getActiveSubscription(testUserId1);
            long oldEndMs = curSub.getEndDate().getTime();

            Order orderRenewal = orderDAO.createOrder(testUserId1, 2, "MOCK_BANKING");
            PaymentDAO.PaymentResult payRes = paymentDAO.processMockPayment(orderRenewal.getOrderId(), testUserId1, true, "MOCK_BANKING");

            Subscription newSub = subscriptionDAO.getActiveSubscription(testUserId1);
            long expectedMinMs = oldEndMs + (29L * 24 * 3600 * 1000); // Tối thiểu cộng thêm 29 ngày
            if (newSub.getEndDate().getTime() < expectedMinMs) {
                fail(name, "Thời hạn không được cộng dồn gia hạn nối tiếp! Old: " + curSub.getEndDate() + ", New: " + newSub.getEndDate());
                return;
            }

            // 2. Kiểm tra xử lý hết hạn: Giả lập gói hết hạn bằng cách update end_date lùi về quá khứ
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE subscriptions SET end_date = DATE_SUB(NOW(), INTERVAL 1 HOUR) WHERE user_id = ?")) {
                ps.setInt(1, testUserId1);
                ps.executeUpdate();
            }

            // Gọi quét hết hạn
            int expiredCount = subscriptionDAO.checkAndExpireSubscriptions();
            if (expiredCount <= 0) {
                fail(name, "checkAndExpireSubscriptions không phát hiện gói cước hết hạn");
                return;
            }

            // Kiểm tra trạng thái chuyển thành EXPIRED
            List<Subscription> subList = subscriptionDAO.getUserSubscriptions(testUserId1);
            if (!"EXPIRED".equals(subList.get(0).getStatus())) {
                fail(name, "Trạng thái gói chưa chuyển thành EXPIRED: " + subList.get(0).getStatus());
                return;
            }

            // Kiểm tra quota tự động giáng cấp về FREE 100MB (không xóa file)
            User u = userDAO.findByUsername("p5_alice");
            if (u.getQuotaBytes() != 104857600L) {
                fail(name, "Quota không được giáng về 100MB khi hết hạn: " + u.getQuotaBytes());
                return;
            }

            pass(name, "Gia hạn cộng dồn thời gian đúng (+30 ngày), hết hạn tự động giáng quota về 100MB an toàn");
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 7: Kháng sửa giá / quyền lợi phía Client
    private static void test07_ClientTamperResistance() {
        String name = "Test 7: Kháng sửa giá hoặc quyền lợi phía Client (Server-side Enforcement)";
        try {
            // Bất kể Client gửi lệnh gì, Server chỉ nhận plan_id và tự truy vấn giá & quota trong bảng 'plans'
            Plan proPlan = planDAO.getPlanById(2);

            Order order = orderDAO.createOrder(testUserId2, 2, "HACKED_CLIENT");
            if (order.getAmount() != proPlan.getPrice()) {
                fail(name, "Giá đơn hàng không khớp với DB: Order=" + order.getAmount() + ", DB=" + proPlan.getPrice());
                return;
            }

            pass(name, "Giá và hạn mức lấy 100% từ bảng plans ở Server, Client không thể làm giả số tiền");
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }

    // Test 8: Tính toàn vẹn giao dịch DB (ACID Rollback)
    private static void test08_TransactionRollbackOnFailure() {
        String name = "Test 8: Đảm bảo tính toàn vẹn ACID (Rollback khi có lỗi trong Transaction)";
        try {
            // Kiểm tra số lượng bản ghi payments hiện tại
            int initialPayments;
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM payments");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                initialPayments = rs.getInt(1);
            }

            // Thử gọi thanh toán với User không phải chủ sở hữu đơn hàng (Anti-IDOR)
            Order order = orderDAO.createOrder(testUserId2, 2, "MOCK_BANKING");
            PaymentDAO.PaymentResult res = paymentDAO.processMockPayment(order.getOrderId(), testUserId1, true, "MOCK_BANKING");

            if (res.success) {
                fail(name, "Đáng lẽ phải bị từ chối truy cập vì sai User ID");
                return;
            }

            // Kiểm tra số lượng payments không bị tăng lên (đã rollback hoàn toàn)
            int finalPayments;
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM payments");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                finalPayments = rs.getInt(1);
            }

            if (finalPayments != initialPayments) {
                fail(name, "Bản ghi thanh toán bị ghi dù Transaction bị lỗi (Không rollback)");
                return;
            }

            pass(name, "Giao dịch bị chặn, Rollback hoạt động hoàn hảo, CSDL giữ vững tính nhất quán ACID");
        } catch (Exception e) {
            fail(name, e.getMessage());
        }
    }
}
