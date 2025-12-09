package necom.eduvn.neihvn.models;

public class OrderItem {
    private String foodId;
    private String foodName;
    private String foodImageUrl;
    private double price;
    private int quantity;

    public OrderItem() {}

    public OrderItem(String foodId, String foodName, double price, int quantity) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.price = price;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getFoodImageUrl() { return foodImageUrl; }
    public void setFoodImageUrl(String foodImageUrl) { this.foodImageUrl = foodImageUrl; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getSubtotal() {
        return price * quantity;
    }
}