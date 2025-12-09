package necom.eduvn.neihvn.models;

public class Favorite {
    private String favoriteId;
    private String userId;
    private String foodId;
    private String foodName;
    private String foodImageUrl;
    private double foodPrice;
    private String restaurantId;
    private String restaurantName;
    private long createdAt;

    public Favorite() {}

    public Favorite(String userId, String foodId) {
        this.userId = userId;
        this.foodId = foodId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getFavoriteId() { return favoriteId; }
    public void setFavoriteId(String favoriteId) { this.favoriteId = favoriteId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getFoodImageUrl() { return foodImageUrl; }
    public void setFoodImageUrl(String foodImageUrl) { this.foodImageUrl = foodImageUrl; }

    public double getFoodPrice() { return foodPrice; }
    public void setFoodPrice(double foodPrice) { this.foodPrice = foodPrice; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

