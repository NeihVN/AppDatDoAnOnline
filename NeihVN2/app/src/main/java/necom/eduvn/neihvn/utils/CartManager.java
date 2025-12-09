package necom.eduvn.neihvn.utils;

import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.models.OrderItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManager {
    private static CartManager instance;
    private Map<String, OrderItem> cartItems;
    private String currentRestaurantId;

    private CartManager() {
        cartItems = new HashMap<>();
    }

    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addItem(FoodItem food, String restaurantId) {
        if (currentRestaurantId == null) {
            currentRestaurantId = restaurantId;
        } else if (!currentRestaurantId.equals(restaurantId)) {
            cartItems.clear();
            currentRestaurantId = restaurantId;
        }

        if (cartItems.containsKey(food.getFoodId())) {
            OrderItem item = cartItems.get(food.getFoodId());
            item.setQuantity(item.getQuantity() + 1);
        } else {
            OrderItem newItem = new OrderItem(food.getFoodId(), food.getName(), food.getPrice(), 1);
            newItem.setFoodImageUrl(food.getImageUrl());
            cartItems.put(food.getFoodId(), newItem);
        }
    }

    public void removeItem(String foodId) {
        cartItems.remove(foodId);
        if (cartItems.isEmpty()) {
            currentRestaurantId = null;
        }
    }

    public void updateQuantity(String foodId, int quantity) {
        if (quantity <= 0) {
            removeItem(foodId);
        } else {
            OrderItem item = cartItems.get(foodId);
            if (item != null) {
                item.setQuantity(quantity);
            }
        }
    }

    public List<OrderItem> getCartItems() {
        return new ArrayList<>(cartItems.values());
    }

    public double getTotalAmount() {
        double total = 0;
        for (OrderItem item : cartItems.values()) {
            total += item.getSubtotal();
        }
        return total;
    }

    public int getItemCount() {
        int count = 0;
        for (OrderItem item : cartItems.values()) {
            count += item.getQuantity();
        }
        return count;
    }

    public void clearCart() {
        cartItems.clear();
        currentRestaurantId = null;
    }

    public String getCurrentRestaurantId() {
        return currentRestaurantId;
    }
}