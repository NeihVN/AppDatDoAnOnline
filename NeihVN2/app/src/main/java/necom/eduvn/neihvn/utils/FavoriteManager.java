package necom.eduvn.neihvn.utils;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import necom.eduvn.neihvn.models.Favorite;
import necom.eduvn.neihvn.models.FoodItem;

public class FavoriteManager {
    private static final String TAG = "FavoriteManager";
    private static final String COLLECTION_FAVORITES = "favorites";
    private static FavoriteManager instance;
    private FirebaseFirestore db;
    
    // Cache for favorite IDs to avoid repeated queries
    private List<String> favoriteFoodIds;
    private boolean favoritesLoaded = false;

    private FavoriteManager() {
        db = FirebaseUtil.getFirestore();
        favoriteFoodIds = new ArrayList<>();
    }

    public static FavoriteManager getInstance() {
        if (instance == null) {
            instance = new FavoriteManager();
        }
        return instance;
    }

    /**
     * Add food item to favorites
     */
    public void addFavorite(FoodItem food, OnFavoriteChangeListener listener) {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null || food == null || food.getFoodId() == null) {
            if (listener != null) {
                listener.onError("Invalid user or food item");
            }
            return;
        }

        // Check if already favorited
        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("foodId", food.getFoodId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Already favorited
                        if (listener != null) {
                            listener.onError("Already in favorites");
                        }
                    } else {
                        // Add to favorites
                        Favorite favorite = new Favorite(userId, food.getFoodId());
                        favorite.setFoodName(food.getName());
                        favorite.setFoodImageUrl(food.getImageUrl());
                        favorite.setFoodPrice(food.getPrice());
                        favorite.setRestaurantId(food.getRestaurantId());

                        // Get restaurant name
                        if (food.getRestaurantId() != null) {
                            db.collection("restaurants")
                                    .document(food.getRestaurantId())
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) {
                                            favorite.setRestaurantName(doc.getString("name"));
                                        }
                                        saveFavorite(favorite, listener);
                                    })
                                    .addOnFailureListener(e -> {
                                        saveFavorite(favorite, listener);
                                    });
                        } else {
                            saveFavorite(favorite, listener);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi kiểm tra danh sách yêu thích: " + e.getMessage());
                    if (listener != null) {
                        listener.onError("Lỗi: " + e.getMessage());
                    }
                });
    }

    private void saveFavorite(Favorite favorite, OnFavoriteChangeListener listener) {
        db.collection(COLLECTION_FAVORITES)
                .add(favorite)
                .addOnSuccessListener(documentReference -> {
                    favorite.setFavoriteId(documentReference.getId());
                    // Update cache
                    if (!favoriteFoodIds.contains(favorite.getFoodId())) {
                        favoriteFoodIds.add(favorite.getFoodId());
                    }
                    if (listener != null) {
                        listener.onAdded(favorite);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi thêm yêu thích: " + e.getMessage());
                    if (listener != null) {
                        listener.onError("Lỗi: " + e.getMessage());
                    }
                });
    }

    /**
     * Remove food item from favorites
     */
    public void removeFavorite(String foodId, OnFavoriteChangeListener listener) {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null || foodId == null) {
            if (listener != null) {
                listener.onError("Thông tin người dùng hoặc món ăn không hợp lệ");
            }
            return;
        }

        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("foodId", foodId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                    if (listener != null) {
                        listener.onError("Món ăn chưa nằm trong danh sách yêu thích");
                    }
                    } else {
                        // Delete all matching favorites (should only be one)
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            doc.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // Update cache
                                        favoriteFoodIds.remove(foodId);
                                        if (listener != null) {
                                            listener.onRemoved(foodId);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Lỗi khi bỏ yêu thích: " + e.getMessage());
                                        if (listener != null) {
                                            listener.onError("Lỗi: " + e.getMessage());
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi kiểm tra danh sách yêu thích: " + e.getMessage());
                    if (listener != null) {
                        listener.onError("Lỗi: " + e.getMessage());
                    }
                });
    }

    /**
     * Check if food item is favorited
     */
    public void isFavorite(String foodId, OnFavoriteCheckListener listener) {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null || foodId == null) {
            if (listener != null) {
                listener.onResult(false);
            }
            return;
        }

        // Check cache first
        if (favoritesLoaded && favoriteFoodIds.contains(foodId)) {
            if (listener != null) {
                listener.onResult(true);
            }
            return;
        }

        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("foodId", foodId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isFav = !queryDocumentSnapshots.isEmpty();
                    // Update cache
                    if (isFav && !favoriteFoodIds.contains(foodId)) {
                        favoriteFoodIds.add(foodId);
                    } else if (!isFav) {
                        favoriteFoodIds.remove(foodId);
                    }
                    if (listener != null) {
                        listener.onResult(isFav);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi kiểm tra trạng thái yêu thích: " + e.getMessage());
                    if (listener != null) {
                        listener.onResult(false);
                    }
                });
    }

    /**
     * Load all favorite food IDs for current user (for caching)
     */
    public void loadFavoriteIds(OnFavoritesLoadedListener listener) {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            favoriteFoodIds.clear();
            favoritesLoaded = true;
            if (listener != null) {
                listener.onLoaded(new ArrayList<>());
            }
            return;
        }

        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoriteFoodIds.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String foodId = doc.getString("foodId");
                        if (foodId != null) {
                            favoriteFoodIds.add(foodId);
                        }
                    }
                    favoritesLoaded = true;
                    if (listener != null) {
                        listener.onLoaded(favoriteFoodIds);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải danh sách yêu thích: " + e.getMessage());
                    favoriteFoodIds.clear();
                    favoritesLoaded = true;
                    if (listener != null) {
                        listener.onLoaded(new ArrayList<>());
                    }
                });
    }

    /**
     * Clear cache (call when user logs out)
     */
    public void clearCache() {
        favoriteFoodIds.clear();
        favoritesLoaded = false;
    }

    // Listener interfaces
    public interface OnFavoriteChangeListener {
        void onAdded(Favorite favorite);
        void onRemoved(String foodId);
        void onError(String error);
    }

    public interface OnFavoriteCheckListener {
        void onResult(boolean isFavorite);
    }

    public interface OnFavoritesLoadedListener {
        void onLoaded(List<String> favoriteIds);
    }
}

