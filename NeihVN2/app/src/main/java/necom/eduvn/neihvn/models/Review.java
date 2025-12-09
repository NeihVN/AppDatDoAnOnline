package necom.eduvn.neihvn.models;

public class Review {
    private String reviewId;
    private String orderId;
    private String buyerId;
    private String buyerName;
    private String buyerAvatar;
    private String restaurantId;
    private String foodId;
    private float rating;
    private String comment;
    private String sellerReply;
    private long createdAt;

    public Review() {}

    public Review(String reviewId, String orderId, String buyerId, String restaurantId, float rating) {
        this.reviewId = reviewId;
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.restaurantId = restaurantId;
        this.rating = rating;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getBuyerAvatar() { return buyerAvatar; }
    public void setBuyerAvatar(String buyerAvatar) { this.buyerAvatar = buyerAvatar; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getSellerReply() { return sellerReply; }
    public void setSellerReply(String sellerReply) { this.sellerReply = sellerReply; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}