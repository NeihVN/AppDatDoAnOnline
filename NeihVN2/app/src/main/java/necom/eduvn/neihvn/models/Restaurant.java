package necom.eduvn.neihvn.models;

public class Restaurant {
    private String restaurantId;
    private String sellerId;
    private String name;
    private String description;
    private String imageUrl;
    private String address;
    private String phone;
    private double rating;
    private int totalReviews;
    private boolean isApproved;
    private boolean isActive;
    private long createdAt;
    private java.util.List<String> categories;

    public Restaurant() {}

    public Restaurant(String restaurantId, String sellerId, String name, String address) {
        this.restaurantId = restaurantId;
        this.sellerId = sellerId;
        this.name = name;
        this.address = address;
        this.rating = 0.0;
        this.totalReviews = 0;
        this.isApproved = false;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
        this.categories = new java.util.ArrayList<>();
    }

    // Getters and Setters
    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public java.util.List<String> getCategories() { return categories; }
    public void setCategories(java.util.List<String> categories) { this.categories = categories; }
}