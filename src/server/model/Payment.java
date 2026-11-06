package server.model;

import java.sql.Timestamp;

/**
 * Model biểu diễn Giao dịch thanh toán mô phỏng (Payment).
 */
public class Payment {
    private int paymentId;
    private int orderId;
    private String paymentCode; // Idempotency key
    private double amount;
    private String paymentMethod;
    private String status; // SUCCESS, FAILED
    private String rawResponse;
    private Timestamp paidAt;

    public Payment() {}

    public Payment(int paymentId, int orderId, String paymentCode, double amount,
                   String paymentMethod, String status, String rawResponse, Timestamp paidAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.paymentCode = paymentCode;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.rawResponse = rawResponse;
        this.paidAt = paidAt;
    }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

    public Timestamp getPaidAt() { return paidAt; }
    public void setPaidAt(Timestamp paidAt) { this.paidAt = paidAt; }
}

