package server.model;

import java.sql.Timestamp;

/**
 * Model biểu diễn Đơn hàng đăng ký gói cước (Order).
 */
public class Order {
    private int orderId;
    private int userId;
    private int planId;
    private String planName;
    private double amount;
    private String paymentMethod;
    private String status; // PENDING, COMPLETED, FAILED, CANCELLED
    private String transactionCode;
    private Timestamp createdAt;

    public Order() {}

    public Order(int orderId, int userId, int planId, String planName, double amount,
                 String paymentMethod, String status, String transactionCode, Timestamp createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.planId = planId;
        this.planName = planName;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionCode = transactionCode;
        this.createdAt = createdAt;
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getFormattedAmount() {
        return String.format("%,.0f VND", amount);
    }
}

