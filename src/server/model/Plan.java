package server.model;

/**
 * Model biểu diễn Gói cước dịch vụ lưu trữ (Service Plan).
 */
public class Plan {
    private int planId;
    private String planName;
    private long maxQuotaBytes;
    private double price;
    private int durationDays;
    private String description;
    private boolean isActive;

    public Plan() {}

    public Plan(int planId, String planName, long maxQuotaBytes, double price, int durationDays, String description, boolean isActive) {
        this.planId = planId;
        this.planName = planName;
        this.maxQuotaBytes = maxQuotaBytes;
        this.price = price;
        this.durationDays = durationDays;
        this.description = description;
        this.isActive = isActive;
    }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public long getMaxQuotaBytes() { return maxQuotaBytes; }
    public void setMaxQuotaBytes(long maxQuotaBytes) { this.maxQuotaBytes = maxQuotaBytes; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getFormattedQuota() {
        if (maxQuotaBytes >= 1024L * 1024 * 1024) {
            return String.format("%.1f GB", (double) maxQuotaBytes / (1024 * 1024 * 1024));
        } else if (maxQuotaBytes >= 1024 * 1024) {
            return String.format("%d MB", maxQuotaBytes / (1024 * 1024));
        } else {
            return String.format("%d KB", maxQuotaBytes / 1024);
        }
    }

    public String getFormattedPrice() {
        if (price == 0) return "Miễn phí (0 VND)";
        return String.format("%,.0f VND", price);
    }
}

