package necom.eduvn.neihvn.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.adapters.FoodAdapter;
import necom.eduvn.neihvn.adapters.ReviewAdapter;
import necom.eduvn.neihvn.databinding.ActivityRestaurantDetailBinding;
import necom.eduvn.neihvn.models.Favorite;
import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.models.Review;
import necom.eduvn.neihvn.utils.CartManager;
import necom.eduvn.neihvn.utils.CategoryUtils;
import necom.eduvn.neihvn.utils.FavoriteManager;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Locale;

public class RestaurantDetailActivity extends AppCompatActivity {
    private ActivityRestaurantDetailBinding binding;
    private String restaurantId;
    private Restaurant restaurant;
    private FoodAdapter foodAdapter;
    private ReviewAdapter reviewAdapter;
    private List<FoodItem> foodList;
    private List<FoodItem> filteredFoodList;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRestaurantDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        restaurantId = getIntent().getStringExtra("restaurantId");
        
        foodList = new ArrayList<>();
        filteredFoodList = new ArrayList<>();
        reviewList = new ArrayList<>();

        setupToolbar();
        setupRecyclerView();
        setupTabs();
        
        if (restaurantId != null) {
            loadRestaurant();
            loadFoods();
            loadReviews();
        } else {
            finish();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        foodAdapter = new FoodAdapter(filteredFoodList, false, new FoodAdapter.OnFoodActionListener() {
            @Override
            public void onEdit(FoodItem food) {}

            @Override
            public void onDelete(FoodItem food) {}

            @Override
            public void onToggleAvailability(FoodItem food) {}

            @Override
            public void onClick(FoodItem food) {
                Intent intent = new Intent(RestaurantDetailActivity.this, FoodDetailActivity.class);
                intent.putExtra("foodId", food.getFoodId());
                intent.putExtra("restaurantId", restaurantId);
                startActivity(intent);
            }

            @Override
            public void onAddToCart(FoodItem food) {
                CartManager cartManager = CartManager.getInstance();
                cartManager.addItem(food, restaurantId);
                Toast.makeText(RestaurantDetailActivity.this, "Đã thêm " + food.getName() + " vào giỏ hàng! 🛒", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onToggleFavorite(FoodItem food) {
                FavoriteManager favoriteManager = FavoriteManager.getInstance();
                
                favoriteManager.isFavorite(food.getFoodId(), isFavorite -> {
                    if (isFavorite) {
                        favoriteManager.removeFavorite(food.getFoodId(), new FavoriteManager.OnFavoriteChangeListener() {
                            @Override
                            public void onAdded(Favorite favorite) {}

                            @Override
                            public void onRemoved(String foodId) {
                                foodAdapter.updateFavoriteStatus(foodId, false);
                                Toast.makeText(RestaurantDetailActivity.this, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(RestaurantDetailActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        favoriteManager.addFavorite(food, new FavoriteManager.OnFavoriteChangeListener() {
                            @Override
                            public void onAdded(Favorite favorite) {
                                foodAdapter.updateFavoriteStatus(food.getFoodId(), true);
                                Toast.makeText(RestaurantDetailActivity.this, "Đã thêm vào danh sách yêu thích! ❤️", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onRemoved(String foodId) {}

                            @Override
                            public void onError(String error) {
                                Toast.makeText(RestaurantDetailActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        binding.recyclerViewFoods.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerViewFoods.setAdapter(foodAdapter);
        
        // Setup reviews recycler view
        reviewAdapter = new ReviewAdapter(reviewList, false, null);
        binding.recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewReviews.setAdapter(reviewAdapter);
    }

    private void setupTabs() {
        binding.tabLayoutCategories.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterByCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadRestaurant() {
        FirebaseUtil.getFirestore().collection("restaurants")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        restaurant = documentSnapshot.toObject(Restaurant.class);
                        if (restaurant != null) {
                            displayRestaurantInfo();
                            setupCategoryTabs();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin nhà hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayRestaurantInfo() {
        binding.tvRestaurantName.setText(restaurant.getName());
        binding.tvRestaurantAddress.setText(restaurant.getAddress());
        
        // Display rating with proper formatting
        double rating = restaurant.getRating();
        int reviewCount = restaurant.getTotalReviews();
        if (reviewCount > 0) {
            binding.tvRestaurantRating.setText(String.format(Locale.getDefault(),
                    "⭐ %.1f (%d đánh giá)", rating, reviewCount));
        } else {
            binding.tvRestaurantRating.setText("⭐ Chưa có đánh giá");
        }

        if (restaurant.getImageUrl() != null && !restaurant.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(restaurant.getImageUrl())
                    .placeholder(R.drawable.placeholder_restaurant)
                    .centerCrop()
                    .into(binding.ivRestaurantImage);
        }

        binding.collapsingToolbar.setTitle(restaurant.getName());
    }

    private void setupCategoryTabs() {
        binding.tabLayoutCategories.removeAllTabs();
        
        // Add "All" tab
        binding.tabLayoutCategories.addTab(binding.tabLayoutCategories.newTab().setText("Tất cả"));
        
        // Add category tabs if restaurant has categories
        if (restaurant.getCategories() != null) {
            for (String category : restaurant.getCategories()) {
                binding.tabLayoutCategories.addTab(
                        binding.tabLayoutCategories.newTab()
                                .setText(CategoryUtils.getDisplayName(category)));
            }
        }
    }

    private void loadFoods() {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseUtil.getFirestore().collection("foods")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("approved", true)
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBar.setVisibility(View.GONE);
                    
                    foodList.clear();
                    foodList.addAll(queryDocumentSnapshots.toObjects(FoodItem.class));
                    filterByCategory(0); // Show all by default
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải thực đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterByCategory(int position) {
        filteredFoodList.clear();

        if (position == 0) { // All
            filteredFoodList.addAll(foodList);
        } else if (restaurant != null && restaurant.getCategories() != null && 
                   position > 0 && position <= restaurant.getCategories().size()) {
            String selectedCategory = CategoryUtils.getCanonicalCode(restaurant.getCategories().get(position - 1));
            
            // Normalize category (trim and case-insensitive comparison)
            if (selectedCategory != null) {
                selectedCategory = selectedCategory.trim();
                
                for (FoodItem food : foodList) {
                    String foodCategory = CategoryUtils.getCanonicalCode(food.getCategory());
                    if (foodCategory != null) {
                        foodCategory = foodCategory.trim();
                        // Case-insensitive comparison
                        if (selectedCategory.equalsIgnoreCase(foodCategory)) {
                            filteredFoodList.add(food);
                        }
                    }
                }
            }
        }

        foodAdapter.notifyDataSetChanged();
        binding.tvEmptyState.setVisibility(filteredFoodList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadReviews() {
        if (restaurantId == null) return;
        
        // Load restaurant reviews (foodId is null or empty)
        FirebaseUtil.getFirestore().collection("reviews")
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Review review = doc.toObject(Review.class);
                        // Only show restaurant reviews (not food-specific reviews)
                        if (review != null && (review.getFoodId() == null || review.getFoodId().isEmpty())) {
                            reviewList.add(review);
                        }
                    }
                    
                    // Sort by createdAt descending (newest first)
                    reviewList.sort((r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                    
                    reviewAdapter.notifyDataSetChanged();
                    binding.tvNoReviews.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    // Error loading reviews - just hide section
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh reviews when returning from ReviewActivity
        if (restaurantId != null) {
            loadReviews();
        }
    }
}
