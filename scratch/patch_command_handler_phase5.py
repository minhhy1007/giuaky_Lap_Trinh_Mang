# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\CommandHandler.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add imports
old_imports = """import server.dao.FileDAO;
import server.dao.LogDAO;
import server.dao.ShareDAO;
import server.dao.UserDAO;
import server.model.FileMetadata;
import server.model.SharedFile;
import server.model.TransferLog;
import server.model.User;"""

new_imports = """import server.dao.FileDAO;
import server.dao.LogDAO;
import server.dao.ShareDAO;
import server.dao.UserDAO;
import server.dao.OrderDAO;
import server.dao.PaymentDAO;
import server.dao.PlanDAO;
import server.dao.SubscriptionDAO;
import server.model.FileMetadata;
import server.model.SharedFile;
import server.model.TransferLog;
import server.model.User;
import server.model.Order;
import server.model.Plan;
import server.model.Subscription;"""

content = content.replace(old_imports, new_imports)

# 2. Add fields and constructor initialization
old_fields = """    private final LogDAO          logDAO;
    private final ShareDAO        shareDAO;
    private final FileDAO         fileDAO;
    private final UserDAO         userDAO;
    private String                clientIp = "127.0.0.1";

    public CommandHandler(Session session, UserDatabase userDatabase,
                          StorageManager storageManager) {
        this.session        = session;
        this.userDatabase   = userDatabase;
        this.storageManager = storageManager;
        this.logDAO         = new LogDAO();
        this.shareDAO       = new ShareDAO();
        this.fileDAO        = new FileDAO();
        this.userDAO        = new UserDAO();
    }"""

new_fields = """    private final LogDAO          logDAO;
    private final ShareDAO        shareDAO;
    private final FileDAO         fileDAO;
    private final UserDAO         userDAO;
    private final PlanDAO         planDAO;
    private final SubscriptionDAO subscriptionDAO;
    private final OrderDAO        orderDAO;
    private final PaymentDAO      paymentDAO;
    private String                clientIp = "127.0.0.1";

    public CommandHandler(Session session, UserDatabase userDatabase,
                          StorageManager storageManager) {
        this.session        = session;
        this.userDatabase   = userDatabase;
        this.storageManager = storageManager;
        this.logDAO         = new LogDAO();
        this.shareDAO       = new ShareDAO();
        this.fileDAO        = new FileDAO();
        this.userDAO        = new UserDAO();
        this.planDAO         = new PlanDAO();
        this.subscriptionDAO = new SubscriptionDAO();
        this.orderDAO        = new OrderDAO();
        this.paymentDAO      = new PaymentDAO();
    }"""

content = content.replace(old_fields, new_fields)

# 3. Add switch cases
old_switch = """            case "AUDITLOGS":
                return handleAuditLogs(argument);

            // ── Commands DÀNH RIÊNG CHO QUẢN TRỊ VIÊN (ADMIN ONLY) ────────"""

new_switch = """            case "AUDITLOGS":
                return handleAuditLogs(argument);

            // ── Phase 5: Plans, Subscriptions, Orders & Mock Payment ──────
            case "PLANS":
                return handlePlans();

            case "MYPLAN":
                return handleMyPlan();

            case "ORDER":
                return handleOrder(argument);

            case "CANCELORDER":
                return handleCancelOrder(argument);

            case "PAY":
                return handlePay(argument);

            case "MYORDERS":
                return handleMyOrders();

            // ── Commands DÀNH RIÊNG CHO QUẢN TRỊ VIÊN (ADMIN ONLY) ────────"""

content = content.replace(old_switch, new_switch)

# 4. Check and expire subscriptions on handleStor
old_stor = """        InputStream rawIn = lineReader.getRawInputStream();
        int userId = getUserId();"""

new_stor = """        // Tự động kiểm tra và giáng cấp gói nếu đã hết hạn
        subscriptionDAO.checkAndExpireSubscriptions();

        InputStream rawIn = lineReader.getRawInputStream();
        int userId = getUserId();"""

content = content.replace(old_stor, new_stor)

# 5. Add handler methods at the end of class before the last closing brace
new_methods = """
    // =========================================================================
    // PHASE 5: GÓI CƯỚC, ĐĂNG KÝ, ĐƠN HÀNG VÀ THANH TOÁN MÔ PHỎNG (MOCK PAYMENT)
    // =========================================================================

    private String handlePlans() {
        List<Plan> plans = planDAO.getAllPlans();
        StringBuilder sb = new StringBuilder();
        sb.append("150-Available service plans (").append(plans.size()).append("):\n");
        sb.append(String.format("  %-5s %-10s %-12s %-18s %-10s %-30s%n", "ID", "PLAN", "QUOTA", "PRICE", "DURATION", "DESCRIPTION"));
        sb.append("  -----------------------------------------------------------------------------------------\n");
        for (Plan p : plans) {
            sb.append(String.format("  %-5d %-10s %-12s %-18s %-10s %-30s%n",
                    p.getPlanId(), p.getPlanName(), p.getFormattedQuota(), p.getFormattedPrice(),
                    p.getDurationDays() + " days", p.getDescription()));
        }
        sb.append("226 End of plans list.");
        return sb.toString();
    }

    private String handleMyPlan() {
        if (!session.isAuthenticated()) return "530 Not logged in";
        int userId = getUserId();
        subscriptionDAO.checkAndExpireSubscriptions();
        User user = userDAO.findByUsername(session.getCurrentUser());
        if (user == null) return "550 User not found";

        Subscription activeSub = subscriptionDAO.getActiveSubscription(userId);
        String planName = (activeSub != null) ? activeSub.getPlanName() : "FREE (Default)";
        String status = (activeSub != null) ? activeSub.getStatus() : "ACTIVE";
        String expiry = (activeSub != null && activeSub.getEndDate() != null) ? activeSub.getEndDate().toString() : "Vĩnh viễn (Default)";

        long quota = user.getQuotaBytes();
        long used = user.getUsedBytes();
        long remaining = Math.max(0, quota - used);
        boolean overQuota = (used > quota);

        return String.format("200 Plan: %s | Status: %s%s | Expiry: %s | Quota: %s | Used: %s | Remaining: %s%s",
                planName, status, (overQuota ? " [OVER_QUOTA]" : ""),
                expiry, formatBytes(quota), formatBytes(used), formatBytes(remaining),
                (overQuota ? " (Cảnh báo: Đã vượt quá hạn mức dung lượng!)" : ""));
    }

    private String handleOrder(String argument) {
        if (!session.isAuthenticated()) return "530 Not logged in";
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: ORDER <plan_id> [payment_method]";
        }
        String[] parts = argument.trim().split("\\\\s+", 2);
        try {
            int planId = Integer.parseInt(parts[0]);
            String method = parts.length > 1 ? parts[1] : "MOCK_BANKING";
            int userId = getUserId();
            Order order = orderDAO.createOrder(userId, planId, method);
            if (order == null) {
                return "550 Failed to create order. Plan might be inactive.";
            }
            logDAO.log(userId, clientIp, "CREATE_ORDER", "Order: " + order.getTransactionCode(), 0, "SUCCESS",
                    "Plan: " + order.getPlanName() + " | Amount: " + order.getFormattedAmount());
            return String.format("250 Order created: %s | OrderID: %d | Plan: %s | Amount: %s | Status: %s. Use PAY %d SUCCESS to simulate payment.",
                    order.getTransactionCode(), order.getOrderId(), order.getPlanName(),
                    order.getFormattedAmount(), order.getStatus(), order.getOrderId());
        } catch (NumberFormatException e) {
            return "501 Invalid plan ID: " + parts[0];
        } catch (IllegalArgumentException e) {
            return "550 " + e.getMessage();
        } catch (Exception e) {
            return "550 Error creating order: " + e.getMessage();
        }
    }

    private String handleCancelOrder(String argument) {
        if (!session.isAuthenticated()) return "530 Not logged in";
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: CANCELORDER <order_id_or_code>";
        }
        int userId = getUserId();
        Order order = null;
        try {
            int orderId = Integer.parseInt(argument.trim());
            order = orderDAO.getOrderById(orderId);
        } catch (NumberFormatException e) {
            order = orderDAO.getOrderByCode(argument.trim());
        }

        if (order == null) {
            return "550 Order not found: " + argument.trim();
        }
        if (order.getUserId() != userId) {
            return "550 Access denied: Order belongs to another user (Anti-IDOR)";
        }
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            return "550 Cannot cancel order with status: " + order.getStatus();
        }

        boolean ok = orderDAO.cancelOrder(order.getOrderId(), userId);
        if (ok) {
            logDAO.log(userId, clientIp, "CANCEL_ORDER", "Order: " + order.getTransactionCode(), 0, "SUCCESS", "Status changed to CANCELLED");
            return "200 Order " + order.getTransactionCode() + " cancelled successfully.";
        } else {
            return "550 Failed to cancel order.";
        }
    }

    private String handlePay(String argument) {
        if (!session.isAuthenticated()) return "530 Not logged in";
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: PAY <order_id_or_code> [SUCCESS|FAIL]";
        }
        String[] parts = argument.trim().split("\\\\s+");
        String idOrCode = parts[0];
        boolean simulateSuccess = true;
        if (parts.length > 1 && ("FAIL".equalsIgnoreCase(parts[1]) || "FAILED".equalsIgnoreCase(parts[1]))) {
            simulateSuccess = false;
        }

        int userId = getUserId();
        Order order = null;
        try {
            int orderId = Integer.parseInt(idOrCode);
            order = orderDAO.getOrderById(orderId);
        } catch (NumberFormatException e) {
            order = orderDAO.getOrderByCode(idOrCode);
        }

        if (order == null) {
            return "550 Order not found: " + idOrCode;
        }

        PaymentDAO.PaymentResult result = paymentDAO.processMockPayment(order.getOrderId(), userId, simulateSuccess, "MOCK_BANKING");
        if (result.success) {
            logDAO.log(userId, clientIp, "PAYMENT_SUCCESS", "Order: " + order.getTransactionCode(), 0, "SUCCESS",
                    result.message + " [PaymentCode: " + result.paymentCode + "]");
            return "200 " + result.message + " (Transaction: " + result.paymentCode + ")";
        } else {
            logDAO.log(userId, clientIp, "PAYMENT_FAILED", "Order: " + order.getTransactionCode(), 0, "FAILED",
                    result.message);
            return "550 " + result.message;
        }
    }

    private String handleMyOrders() {
        if (!session.isAuthenticated()) return "530 Not logged in";
        int userId = getUserId();
        List<Order> orders = orderDAO.getUserOrders(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("150-Your orders list (").append(orders.size()).append("):\n");
        sb.append(String.format("  %-8s %-26s %-10s %-16s %-12s %-20s%n", "ORDER_ID", "CODE", "PLAN", "AMOUNT", "STATUS", "DATE"));
        sb.append("  ------------------------------------------------------------------------------------------------\n");
        for (Order o : orders) {
            sb.append(String.format("  %-8d %-26s %-10s %-16s %-12s %-20s%n",
                    o.getOrderId(), o.getTransactionCode(), o.getPlanName(),
                    o.getFormattedAmount(), o.getStatus(), o.getCreatedAt().toString()));
        }
        sb.append("226 End of orders list.");
        return sb.toString();
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
"""

last_brace = content.rfind("}")
if last_brace != -1:
    content = content[:last_brace] + new_methods

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\CommandHandler.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] CommandHandler.java patched for Phase 5!")

