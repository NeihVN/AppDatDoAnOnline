package necom.eduvn.neihvn.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.adapters.ReviewAdapter;
import necom.eduvn.neihvn.databinding.ActivityFoodDetailBinding;
import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.models.Review;
import necom.eduvn.neihvn.utils.CartManager;
import necom.eduvn.neihvn.utils.CategoryUtils;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.LinearLayoutManager;

public class FoodDetailActivity extends AppCompatActivity {
    private ActivityFoodDetailBinding binding;
    private String foodId;
    private String restaurantId;
    private FoodItem currentFood;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private int quantity = 1;
    private boolean isSellerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFoodDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        foodId = getIntent().getStringExtra("foodId");
        restaurantId = getIntent().getStringExtra("restaurantId");
        isSellerMode = getIntent().getBooleanExtra("isSellerMode", false);

        reviewList = new ArrayList<>();
        
        setupToolbar();
        setupQuantityControls();
        setupReviewsRecyclerView();
        setupSellerMode();
        loadFoodDetails();
        loadRestaurantInfo();
        loadReviews();

        if (!isSellerMode) {
            binding.btnAddToCart.setOnClickListener(v -> addToCart());
        }
    }

    private void setupSellerMode() {
        if (isSellerMode) {
            // Hide buyer-specific controls
            binding.layoutQuantity.setVisibility(View.GONE);
            binding.btnAddToCart.setVisibility(View.GONE);
            
            // Show edit button in toolbar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                getSupportActionBar().setTitle("Thông tin món ăn");
            }
        } else {
            // Show buyer controls
            binding.layoutQuantity.setVisibility(View.VISIBLE);
            binding.btnAddToCart.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        
        // Add edit button for seller mode
        if (isSellerMode) {
            binding.toolbar.setNavigationOnClickListener(v -> finish());
            // Edit button will be added when food is loaded
        } else {
            binding.toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupQuantityControls() {
        binding.tvQuantity.setText(String.valueOf(quantity));

        binding.btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.tvQuantity.setText(String.valueOf(quantity));
                updateTotalPrice();
            }
        });

        binding.btnPlus.setOnClickListener(v -> {
            if (quantity < 99) {
                quantity++;
                binding.tvQuantity.setText(String.valueOf(quantity));
                updateTotalPrice();
            }
        });
    }

    private void setupReviewsRecyclerView() {
        reviewAdapter = new ReviewAdapter(reviewList, false, null);
        binding.recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewReviews.setAdapter(reviewAdapter);
    }

    private void loadFoodDetails() {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);

        FirebaseUtil.getFirestore().collection("foods")
                .document(foodId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentFood = documentSnapshot.toObject(FoodItem.class);
                        if (currentFood != null) {
                            displayFoodDetails();
                        }
                    }
                    binding.progressBar.setVisibility(android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                });
    }

    private void displayFoodDetails() {
        binding.tvFoodName.setText(currentFood.getName());
        binding.tvDescription.setText(currentFood.getDescription());
        binding.tvPrice.setText(CurrencyFormatter.format(currentFood.getPrice()));
        binding.tvCategory.setText(CategoryUtils.getDisplayName(currentFood.getCategory()));
        
        // Rating will be updated when reviews are loaded (via updateRatingFromReviews)
        // For now, show initial rating from food item
        if (reviewList == null || reviewList.isEmpty()) {
            // Reviews not loaded yet, show rating from food item
            float rating = (float) currentFood.getRating();
            int reviewCount = currentFood.getTotalReviews();
            
            binding.ratingBar.setVisibility(android.view.View.VISIBLE);
            
            if (reviewCount > 0 && rating > 0) {
                binding.ratingBar.setRating(rating);
                binding.tvRatingText.setText(String.format(Locale.getDefault(), "%.1f (%d đánh giá)",
                        rating, reviewCount));
            } else {
                binding.ratingBar.setRating(0);
                binding.tvRatingText.setText("Chưa có đánh giá");
            }
        } else {
            // Reviews already loaded, use rating from reviews
            updateRatingFromReviews();
        }

        Glide.with(this)
                .load(currentFood.getImageUrl())
                .placeholder(R.drawable.placeholder_food)
                .error(R.drawable.placeholder_food)
                .centerCrop()
                .into(binding.ivFoodImage);

        updateTotalPrice();
        
        // Setup edit button for seller mode
        if (isSellerMode) {
            setupEditButton();
        }
    }

    private void setupEditButton() {
        // Add menu item for edit in toolbar
        if (binding.toolbar.getMenu() != null) {
            binding.toolbar.getMenu().clear();
        }
        binding.toolbar.inflateMenu(R.menu.menu_food_detail);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                editFood();
                return true;
            }
            return false;
        });
    }

    private void editFood() {
        if (currentFood != null && foodId != null && restaurantId != null) {
            Intent intent = new Intent(this, FoodFormActivity.class);
            intent.putExtra("restaurantId", restaurantId);
            intent.putExtra("foodId", foodId);
            startActivity(intent);
        }
    }

    private void loadRestaurantInfo() {
        FirebaseUtil.getFirestore().collection("restaurants")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Restaurant restaurant = documentSnapshot.toObject(Restaurant.class);
                        if (restaurant != null) {
                            binding.tvRestaurantName.setText(restaurant.getName());
                            binding.tvRestaurantAddress.setText(restaurant.getAddress());
                        }
                    }
                });
    }

    private void updateTotalPrice() {
        if (currentFood != null) {
            double total = currentFood.getPrice() * quantity;
        binding.btnAddToCart.setText(String.format(Locale.getDefault(), "Thêm vào giỏ - %s", CurrencyFormatter.format(total)));
        }
    }

    private void addToCart() {
        if (currentFood != null) {
            CartManager cartManager = CartManager.getInstance();

            for (int i = 0; i < quantity; i++) {
                cartManager.addItem(currentFood, restaurantId);
            }

            Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();

            // Update badge if in BuyerMainActivity
            if (getParent() instanceof BuyerMainActivity) {
                ((BuyerMainActivity) getParent()).updateCartBadge();
            }

            finish();
        }
    }

    private void loadReviews() {
        if (foodId == null) return;
        
        // Load food-specific reviews
        FirebaseUtil.getFirestore().collection("reviews")
                .whereEqualTo("foodId", foodId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();
                    reviewList.addAll(queryDocumentSnapshots.toObjects(Review.class));
                    
                    // Sort by createdAt descending (newest first)
                    reviewList.sort((r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                    
                    // Calculate rating from reviews and update UI
                    updateRatingFromReviews();
                    
                    reviewAdapter.notifyDataSetChanged();
                    binding.tvNoReviews.setVisibility(reviewList.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    // Error loading reviews
                });
    }

    private void updateRatingFromReviews() {
        if (binding == null) return;
        
        // Always show rating bar
        binding.ratingBar.setVisibility(android.view.View.VISIBLE);
        
        if (reviewList == null || reviewList.isEmpty()) {
            // No reviews - use rating from food item if available
            if (currentFood != null) {
                float rating = (float) currentFood.getRating();
                int reviewCount = currentFood.getTotalReviews();
                
                if (reviewCount > 0 && rating > 0) {
                    binding.ratingBar.setRating(rating);
                    binding.tvRatingText.setText(String.format(Locale.getDefault(), "%.1f (%d đánh giá)",
                            rating, reviewCount));
                } else {
                    binding.ratingBar.setRating(0);
                    binding.tvRatingText.setText("Chưa có đánh giá");
                }
            } else {
                binding.ratingBar.setRating(0);
                binding.tvRatingText.setText("Chưa có đánh giá");
            }
            return;
        }

        // Calculate average rating from reviews (always prioritize reviews over food rating)
        float totalRating = 0;
        int count = 0;
        
        for (Review review : reviewList) {
            if (review != null && review.getRating() > 0) {
                totalRating += review.getRating();
                count++;
            }
        }
        
        float avgRating = count > 0 ? totalRating / count : 0;
        
        // Update UI with calculated rating from reviews
        binding.ratingBar.setRating(avgRating);
        binding.tvRatingText.setText(String.format(Locale.getDefault(), "%.1f (%d đánh giá)",
                avgRating, count));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh reviews and food details when returning from ReviewActivity
        if (foodId != null) {
            loadFoodDetails(); // Reload food to get updated rating
            loadReviews();
        }
    }
}