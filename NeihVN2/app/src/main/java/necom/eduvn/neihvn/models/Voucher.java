package necom.eduvn.neihvn.models;

import necom.eduvn.neihvn.utils.CurrencyFormatter;

public class Voucher {
    private String voucherId;
    private String restaurantId;
    private String code; // Mã voucher (ví dụ: SUMMER2024)
    private String description;
    private String discountType; // "percentage" hoặc "fixed"
    private double discountValue; // Giá trị giảm (% hoặc số tiền)
    private double minOrderAmount; // Đơn hàng tối thiểu
    private double maxDiscount; // Giảm tối đa (cho percentage)
    private long startDate; // Timestamp
    private long endDate; // Timestamp
    private int usageLimit; // Số lần sử dụng tối đa (0 = unlimited)
    private int usedCount; // Số lần đã sử dụng
    private boolean active;
    private long createdAt;

    public Voucher() {
        this.createdAt = System.currentTimeMillis();
        this.usedCount = 0;
        this.active = true;
    }

    public Voucher(String voucherId, String restaurantId, String code, String description, 
                   String discountType, double discountValue, double minOrderAmount, 
                   double maxDiscount, long startDate, long endDate, int usageLimit) {
        this.voucherId = voucherId;
        this.restaurantId = restaurantId;
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscount = maxDiscount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageLimit = usageLimit;
        this.usedCount = 0;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public double getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(double minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public double getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(double maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public boolean isValid() {
        long now = System.currentTimeMillis();
        boolean withinDateRange = now >= startDate && now <= endDate;
        boolean hasUnlimitedUsage = usageLimit <= 0; // -1 (unlimited) or 0 (unlimited)
        boolean hasRemainingUsage = usageLimit > 0 && usedCount < usageLimit;
        
        return active &&
               withinDateRange &&
               (hasUnlimitedUsage || hasRemainingUsage);
    }

    public String getDiscountText() {
        if ("percentage".equals(discountType)) {
            return String.format("%.0f%%", discountValue);
        } else {
            return CurrencyFormatter.format(discountValue);
        }
    }

    public double calculateDiscount(double orderAmount) {
        if (!isValid() || orderAmount < minOrderAmount) {
            return 0;
        }

        if ("percentage".equals(discountType)) {
            double discount = orderAmount * (discountValue / 100.0);
            if (maxDiscount > 0) {
                discount = Math.min(discount, maxDiscount);
            }
            return discount;
        } else {
            return discountValue;
        }
    }
}

