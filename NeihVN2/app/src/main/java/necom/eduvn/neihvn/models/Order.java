package necom.eduvn.neihvn.models;

import java.util.List;

public class Order {
    private String orderId;
    private String buyerId;
    private String restaurantId;
    private List<OrderItem> items;
    private double totalAmount;
    private String deliveryAddress;
    private String paymentMethod;
    private String status; // "Processing", "Delivering", "Completed", "Cancelled", "Pending Payment"
    private String transactionNo; // VNPay transaction number
    private long createdAt;
    private long updatedAt;
    
    // Voucher fields
    private String voucherId; // ID of voucher used (if any)
    private String voucherCode; // Voucher code for display
    private double discountAmount; // Actual discount applied
    private double subtotal; // Amount before discount

    public Order() {}

    public Order(String orderId, String buyerId, String restaurantId, List<OrderItem> items, double totalAmount) {
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.restaurantId = restaurantId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = "Processing";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
    
    public String getVoucherId() { return voucherId; }
    public void setVoucherId(String voucherId) { this.voucherId = voucherId; }
    
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}