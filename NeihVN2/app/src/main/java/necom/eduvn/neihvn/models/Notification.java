package necom.eduvn.neihvn.models;

public class Notification {
    private String notificationId;
    private String sellerId;
    private String title;
    private String message;
    private String type; // "restaurant_approved", "restaurant_rejected", "food_approved", "food_rejected"
    private boolean isRead;
    private long createdAt;
    private String relatedId; // ID của restaurant hoặc food liên quan

    public Notification() {}

    public Notification(String notificationId, String sellerId, String title, String message, 
                       String type, String relatedId) {
        this.notificationId = notificationId;
        this.sellerId = sellerId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.relatedId = relatedId;
        this.isRead = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
}
