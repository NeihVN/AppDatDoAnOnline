package necom.eduvn.neihvn.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.tabs.TabLayout;

import necom.eduvn.neihvn.adapters.FoodAdapter;
import necom.eduvn.neihvn.databinding.ActivityFoodListBinding;
import necom.eduvn.neihvn.models.Favorite;
import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.utils.CartManager;
import necom.eduvn.neihvn.utils.CategoryUtils;
import necom.eduvn.neihvn.utils.FavoriteManager;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodListActivity extends AppCompatActivity {
    private ActivityFoodListBinding binding;
    private FoodAdapter adapter;
    private List<FoodItem> foodList;
    private List<FoodItem> filteredList;
    private String initialSearchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFoodListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        foodList = new ArrayList<>();
        filteredList = new ArrayList<>();

        String searchQuery = getIntent().getStringExtra("searchQuery");
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            initialSearchQuery = searchQuery.trim();
        }

        setupToolbar();
        setupRecyclerView();
        setupSearchView();
        setupTabs();
        loadFoods();

        if (initialSearchQuery != null) {
            binding.searchView.setQuery(initialSearchQuery, false);
            filterFoods(initialSearchQuery);
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
        adapter = new FoodAdapter(filteredList, false, new FoodAdapter.OnFoodActionListener() {
            @Override
            public void onEdit(FoodItem food) {}

            @Override
            public void onDelete(FoodItem food) {}

            @Override
            public void onToggleAvailability(FoodItem food) {}

            @Override
            public void onClick(FoodItem food) {
                Intent intent = new Intent(FoodListActivity.this, FoodDetailActivity.class);
                intent.putExtra("foodId", food.getFoodId());
                intent.putExtra("restaurantId", food.getRestaurantId());
                startActivity(intent);
            }

            @Override
            public void onAddToCart(FoodItem food) {
                CartManager cartManager = CartManager.getInstance();
                cartManager.addItem(food, food.getRestaurantId());
                Toast.makeText(FoodListActivity.this, "Đã thêm " + food.getName() + " vào giỏ! 🛒", Toast.LENGTH_SHORT).show();
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
                                adapter.updateFavoriteStatus(foodId, false);
                                Toast.makeText(FoodListActivity.this, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(FoodListActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        favoriteManager.addFavorite(food, new FavoriteManager.OnFavoriteChangeListener() {
                            @Override
                            public void onAdded(Favorite favorite) {
                                adapter.updateFavoriteStatus(food.getFoodId(), true);
                                Toast.makeText(FoodListActivity.this, "Đã thêm vào danh sách yêu thích! ❤️", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onRemoved(String foodId) {}

                            @Override
                            public void onError(String error) {
                                Toast.makeText(FoodListActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        binding.recyclerViewFoods.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerViewFoods.setAdapter(adapter);
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFoods(newText);
                return true;
            }
        });
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterByCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.btnFilter.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng lọc sẽ sớm có mặt!", Toast.LENGTH_SHORT).show());
    }

    private void loadFoods() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Load approved restaurants first
        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("approved", true)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(restaurantQuery -> {
                    List<String> approvedRestaurantIds = new ArrayList<>();
                    restaurantQuery.forEach(doc -> approvedRestaurantIds.add(doc.getId()));

                    if (approvedRestaurantIds.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        updateEmptyState();
                        return;
                    }

                    // Load approved foods from approved restaurants
                    FirebaseUtil.getFirestore().collection("foods")
                            .whereEqualTo("approved", true)
                            .whereEqualTo("available", true)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                binding.progressBar.setVisibility(View.GONE);
                                
                                foodList.clear();
                                for (FoodItem food : queryDocumentSnapshots.toObjects(FoodItem.class)) {
                                    if (approvedRestaurantIds.contains(food.getRestaurantId())) {
                                        foodList.add(food);
                                    }
                                }

                                filteredList.clear();
                                filteredList.addAll(foodList);
                                if (initialSearchQuery != null) {
                                    filterFoods(initialSearchQuery);
                                } else {
                                    adapter.notifyDataSetChanged();
                                }

                                updateEmptyState();
                            })
                            .addOnFailureListener(e -> {
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Lỗi tải danh sách món ăn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                updateEmptyState();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải danh sách nhà hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void filterFoods(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(foodList);
        } else {
            String lowerQuery = query.toLowerCase(Locale.US);
            for (FoodItem food : foodList) {
                String foodName = food.getName() != null ? food.getName().toLowerCase(Locale.US) : "";
                String categoryCode = food.getCategory() != null ? food.getCategory().toLowerCase(Locale.US) : "";
                String categoryDisplay = CategoryUtils.getDisplayName(food.getCategory()).toLowerCase(Locale.US);
                String description = food.getDescription() != null ? food.getDescription().toLowerCase(Locale.US) : "";

                if (foodName.contains(lowerQuery) ||
                        categoryCode.contains(lowerQuery) ||
                        categoryDisplay.contains(lowerQuery) ||
                        description.contains(lowerQuery)) {
                    filteredList.add(food);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void filterByCategory(int position) {
        filteredList.clear();

        String categoryCode = null;
        switch (position) {
            case 0: // All
                filteredList.addAll(foodList);
                break;
            case 1: // Main
                categoryCode = "Main";
                break;
            case 2: // Drinks
                categoryCode = "Drink";
                break;
            case 3: // Dessert
                categoryCode = "Dessert";
                break;
        }

        if (categoryCode != null) {
            String canonical = CategoryUtils.getCanonicalCode(categoryCode);
            for (FoodItem food : foodList) {
                String foodCategory = CategoryUtils.getCanonicalCode(food.getCategory());
                if (!canonical.isEmpty() && canonical.equalsIgnoreCase(foodCategory)) {
                    filteredList.add(food);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewFoods.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewFoods.setVisibility(View.VISIBLE);
        }
    }
}
