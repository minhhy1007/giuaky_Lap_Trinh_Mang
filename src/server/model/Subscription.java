package server.model;

import java.sql.Timestamp;

/**
 * Model biểu diễn Đăng ký dịch vụ (Subscription) của người dùng.
 */
public class Subscription {
    private int subId;
    private int userId;
    private int planId;
    private String planName;
    private Timestamp startDate;
    private Timestamp endDate;
    private String status; // ACTIVE, EXPIRED, CANCELLED, UPGRADED

    public Subscription() {}

    public Subscription(int subId, int userId, int planId, String planName, Timestamp startDate, Timestamp endDate, String status) {
        this.subId = subId;
        this.userId = userId;
        this.planId = planId;
        this.planName = planName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public int getSubId() { return subId; }
    public void setSubId(int subId) { this.subId = subId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isExpired() {
        if ("EXPIRED".equalsIgnoreCase(status)) return true;
        if (endDate != null && endDate.before(new Timestamp(System.currentTimeMillis()))) {
            return true;
        }
        return false;
    }

    public long getRemainingDays() {
        if (endDate == null) return 0;
        long diffMs = endDate.getTime() - System.currentTimeMillis();
        if (diffMs <= 0) return 0;
        return diffMs / (1000L * 60 * 60 * 24);
    }
}

