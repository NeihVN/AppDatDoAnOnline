package necom.eduvn.neihvn.models;

public class FoodItem {
    private String foodId;
    private String restaurantId;
    private String name;
    private String description;
    private String imageUrl;
    private double price;
    private String category; // "Main", "Drink", "Dessert"
    private double rating;
    private int totalReviews;
    private boolean isAvailable;
    private boolean approved;
    private long createdAt;

    public FoodItem() {}

    public FoodItem(String foodId, String restaurantId, String name, double price, String category) {
        this.foodId = foodId;
        this.restaurantId = restaurantId;
        this.name = name;
        this.price = price;
        this.category = category;
        this.rating = 0.0;
        this.totalReviews = 0;
        this.isAvailable = true;
        this.approved = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}