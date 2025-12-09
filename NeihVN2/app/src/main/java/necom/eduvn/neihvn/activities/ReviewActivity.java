package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ActivityReviewBinding;
import necom.eduvn.neihvn.models.Review;
import necom.eduvn.neihvn.models.User;
import necom.eduvn.neihvn.utils.FirebaseUtil;

public class ReviewActivity extends AppCompatActivity {
    private ActivityReviewBinding binding;
    private String orderId;
    private String restaurantId;
    private String foodId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("orderId");
        restaurantId = getIntent().getStringExtra("restaurantId");
        foodId = getIntent().getStringExtra("foodId");

        setupToolbar();
        loadUserInfo();

        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Set title based on what is being reviewed
        if (foodId != null && !foodId.isEmpty()) {
            // Load food name for title
            loadFoodInfo();
        } else {
            // Review for restaurant
            loadRestaurantInfo();
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void loadRestaurantInfo() {
        if (restaurantId != null) {
            FirebaseUtil.getFirestore().collection("restaurants")
                    .document(restaurantId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            necom.eduvn.neihvn.models.Restaurant restaurant = 
                                    documentSnapshot.toObject(necom.eduvn.neihvn.models.Restaurant.class);
                            if (restaurant != null && getSupportActionBar() != null) {
                                getSupportActionBar().setTitle("Đánh giá: " + restaurant.getName());
                                // Update UI to show restaurant review
                                updateReviewTypeUI("restaurant", restaurant.getName());
                            }
                        }
                    });
        }
    }
    
    private void loadFoodInfo() {
        if (foodId != null) {
            FirebaseUtil.getFirestore().collection("foods")
                    .document(foodId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            necom.eduvn.neihvn.models.FoodItem food = 
                                    documentSnapshot.toObject(necom.eduvn.neihvn.models.FoodItem.class);
                            if (food != null && getSupportActionBar() != null) {
                                getSupportActionBar().setTitle("Đánh giá: " + food.getName());
                                // Update UI to show food review
                                updateReviewTypeUI("food", food.getName());
                            }
                        }
                    });
        }
    }
    
    private void updateReviewTypeUI(String type, String name) {
        if (binding != null) {
            if ("restaurant".equals(type)) {
                binding.tvReviewTitle.setText("Trải nghiệm của bạn tại nhà hàng này thế nào?");
                binding.tvReviewTarget.setText("Đang đánh giá: " + name);
                binding.tvReviewTarget.setVisibility(android.view.View.VISIBLE);
            } else if ("food".equals(type)) {
                binding.tvReviewTitle.setText("Món ăn này có ngon không?");
                binding.tvReviewTarget.setText("Đang đánh giá: " + name);
                binding.tvReviewTarget.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    private void loadUserInfo() {
        String userId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            binding.tvUserName.setText(user.getName());
                            if (user.getAvatarUrl() != null) {
                                Glide.with(this)
                                        .load(user.getAvatarUrl())
                                        .placeholder(R.drawable.ic_user_placeholder)
                                        .circleCrop()
                                        .into(binding.ivUserAvatar);
                            }
                        }
                    }
                });
    }

    private void submitReview() {
        float rating = binding.ratingBar.getRating();
        String comment = binding.etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            binding.etComment.setError("Vui lòng nhập nhận xét");
            return;
        }

        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.btnSubmitReview.setEnabled(false);

        String reviewId = FirebaseUtil.getFirestore().collection("reviews").document().getId();
        String userId = FirebaseUtil.getCurrentUserId();

        Review review = new Review(reviewId, orderId, userId, restaurantId, rating);
        review.setFoodId(foodId);
        review.setComment(comment);

        // Get user info for review
        FirebaseUtil.getFirestore().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            review.setBuyerName(user.getName());
                            review.setBuyerAvatar(user.getAvatarUrl());
                        }
                    }

                    // Save review
                    FirebaseUtil.getFirestore().collection("reviews")
                            .document(reviewId)
                            .set(review)
                            .addOnSuccessListener(aVoid -> {
                                // Update ratings based on what was reviewed
                                if (foodId != null && !foodId.isEmpty()) {
                                    updateFoodRating();
                                }
                                updateRestaurantRating();
                                Toast.makeText(this, "Gửi đánh giá thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.progressBar.setVisibility(android.view.View.GONE);
                                binding.btnSubmitReview.setEnabled(true);
                            });
                });
    }

    private void updateRestaurantRating() {
        // Update restaurant rating with all reviews (both restaurant and food reviews count)
        // This gives a comprehensive rating for the restaurant
        FirebaseUtil.getFirestore().collection("reviews")
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    float totalRating = 0;
                    int count = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        // Only count restaurant reviews (foodId is null or empty)
                        String reviewFoodId = doc.getString("foodId");
                        if (reviewFoodId == null || reviewFoodId.isEmpty()) {
                            Float rating = doc.getDouble("rating").floatValue();
                            if (rating != null) {
                                totalRating += rating;
                                count++;
                            }
                        }
                    }
                    
                    // If there are restaurant reviews, update restaurant rating
                    // Otherwise, we can still use all reviews for a general rating
                    if (count == 0) {
                        // No restaurant-specific reviews, use all reviews for restaurant rating
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            Float rating = doc.getDouble("rating").floatValue();
                            if (rating != null) {
                                totalRating += rating;
                                count++;
                            }
                        }
                    }

                    double avgRating = count > 0 ? totalRating / count : 0;
                    
                    FirebaseUtil.getFirestore().collection("restaurants")
                            .document(restaurantId)
                            .update("rating", avgRating, "totalReviews", count);
                });
    }
    
    private void updateFoodRating() {
        if (foodId == null || foodId.isEmpty()) return;
        
        FirebaseUtil.getFirestore().collection("reviews")
                .whereEqualTo("foodId", foodId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    float totalRating = 0;
                    int count = queryDocumentSnapshots.size();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Float rating = doc.getDouble("rating").floatValue();
                        if (rating != null) totalRating += rating;
                    }

                    double avgRating = count > 0 ? totalRating / count : 0;

                    FirebaseUtil.getFirestore().collection("foods")
                            .document(foodId)
                            .update("rating", avgRating, "totalReviews", count);
                });
    }
}