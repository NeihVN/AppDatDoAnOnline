package necom.eduvn.neihvn.utils;

import necom.eduvn.neihvn.models.Notification;

public class NotificationUtil {

    // Notification types
    public static final String TYPE_RESTAURANT_APPROVED = "restaurant_approved";
    public static final String TYPE_RESTAURANT_REJECTED = "restaurant_rejected";
    public static final String TYPE_FOOD_APPROVED = "food_approved";
    public static final String TYPE_FOOD_REJECTED = "food_rejected";

    public static void sendRestaurantApprovedNotification(String sellerId, String restaurantId, String restaurantName) {
        String notificationId = FirebaseUtil.getFirestore().collection("notifications").document().getId();
        String title = "Restaurant Approved! 🎉";
        String message = "Congratulations! Your restaurant '" + restaurantName + "' has been approved and is now live!";
        
        Notification notification = new Notification(notificationId, sellerId, title, message, 
                                                    TYPE_RESTAURANT_APPROVED, restaurantId);
        
        FirebaseUtil.getFirestore().collection("notifications")
                .document(notificationId)
                .set(notification);
    }

    public static void sendRestaurantRejectedNotification(String sellerId, String restaurantId, String restaurantName) {
        String notificationId = FirebaseUtil.getFirestore().collection("notifications").document().getId();
        String title = "Restaurant Not Approved ❌";
        String message = "Sorry, your restaurant '" + restaurantName + "' was not approved. Please contact admin for more information.";
        
        Notification notification = new Notification(notificationId, sellerId, title, message, 
                                                    TYPE_RESTAURANT_REJECTED, restaurantId);
        
        FirebaseUtil.getFirestore().collection("notifications")
                .document(notificationId)
                .set(notification);
    }

    public static void sendFoodApprovedNotification(String sellerId, String foodId, String foodName) {
        String notificationId = FirebaseUtil.getFirestore().collection("notifications").document().getId();
        String title = "Food Approved! ✅";
        String message = "Your food item '" + foodName + "' has been approved and is now available for customers!";
        
        Notification notification = new Notification(notificationId, sellerId, title, message, 
                                                    TYPE_FOOD_APPROVED, foodId);
        
        FirebaseUtil.getFirestore().collection("notifications")
                .document(notificationId)
                .set(notification);
    }

    public static void sendFoodRejectedNotification(String sellerId, String foodId, String foodName) {
        String notificationId = FirebaseUtil.getFirestore().collection("notifications").document().getId();
        String title = "Food Not Approved ❌";
        String message = "Your food item '" + foodName + "' was not approved. Please contact admin for more information.";
        
        Notification notification = new Notification(notificationId, sellerId, title, message, 
                                                    TYPE_FOOD_REJECTED, foodId);
        
        FirebaseUtil.getFirestore().collection("notifications")
                .document(notificationId)
                .set(notification);
    }
}
