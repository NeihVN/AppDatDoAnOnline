package necom.eduvn.neihvn.fragments.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.activities.FoodDetailActivity;
import necom.eduvn.neihvn.activities.FoodListActivity;
import necom.eduvn.neihvn.activities.RestaurantDetailActivity;
import necom.eduvn.neihvn.activities.RestaurantListActivity;
import necom.eduvn.neihvn.adapters.FoodAdapter;
import necom.eduvn.neihvn.adapters.RestaurantBannerAdapter;
import necom.eduvn.neihvn.databinding.FragmentBuyerHomeBinding;
import necom.eduvn.neihvn.models.Favorite;
import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.utils.CartManager;
import necom.eduvn.neihvn.utils.CategoryUtils;
import necom.eduvn.neihvn.utils.FavoriteManager;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.models.User;

public class BuyerHomeFragment extends Fragment {
    private FragmentBuyerHomeBinding binding;
    private FoodAdapter foodAdapter;
    private RestaurantBannerAdapter restaurantAdapter;
    private List<FoodItem> foodList;
    private List<FoodItem> filteredFoodList;
    private List<Restaurant> restaurantList;
    
    // Filter state
    private String selectedCategory = null; // null = all
    private String sortType = "default"; // default, price_asc, price_desc, name_asc
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodList = new ArrayList<>();
        filteredFoodList = new ArrayList<>();
        restaurantList = new ArrayList<>();

        setupGreeting();
        setupSearchView();
        setupRecyclerViews();
        setupClickListeners();
        loadUserProfile();
        loadRestaurants();
        loadFoods();
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Navigate to food list with search query
                Intent intent = new Intent(getContext(), FoodListActivity.class);
                intent.putExtra("searchQuery", query);
                startActivity(intent);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFoods(newText);
                return true;
            }
        });
    }

    private void setupRecyclerViews() {
        // Restaurant banner
        restaurantAdapter = new RestaurantBannerAdapter(restaurantList, restaurant -> {
            // Navigate to restaurant detail
            Intent intent = new Intent(getContext(), RestaurantDetailActivity.class);
            intent.putExtra("restaurantId", restaurant.getRestaurantId());
            startActivity(intent);
        });
        binding.recyclerViewRestaurants.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewRestaurants.setAdapter(restaurantAdapter);

        // Food grid
        foodAdapter = new FoodAdapter(filteredFoodList, false, new FoodAdapter.OnFoodActionListener() {
            @Override
            public void onEdit(FoodItem food) {}

            @Override
            public void onDelete(FoodItem food) {}

            @Override
            public void onToggleAvailability(FoodItem food) {}

            @Override
            public void onClick(FoodItem food) {
                Intent intent = new Intent(getContext(), FoodDetailActivity.class);
                intent.putExtra("foodId", food.getFoodId());
                intent.putExtra("restaurantId", food.getRestaurantId());
                startActivity(intent);
            }

            @Override
            public void onAddToCart(FoodItem food) {
                // Add to cart manager
                CartManager cartManager = CartManager.getInstance();
                cartManager.addItem(food, food.getRestaurantId());
                
                // Show feedback
                Toast.makeText(getContext(), "Đã thêm " + food.getName() + " vào giỏ hàng! 🛒", Toast.LENGTH_SHORT).show();
                
                // Update cart badge in MainActivity if available
                if (getActivity() != null && getActivity() instanceof necom.eduvn.neihvn.activities.BuyerMainActivity) {
                    ((necom.eduvn.neihvn.activities.BuyerMainActivity) getActivity()).updateCartBadge();
                }
            }

            @Override
            public void onToggleFavorite(FoodItem food) {
                FavoriteManager favoriteManager = FavoriteManager.getInstance();
                
                // Check current status
                favoriteManager.isFavorite(food.getFoodId(), isFavorite -> {
                    if (isFavorite) {
                        // Remove from favorites
                        favoriteManager.removeFavorite(food.getFoodId(), new FavoriteManager.OnFavoriteChangeListener() {
                            @Override
                            public void onAdded(Favorite favorite) {}

                            @Override
                            public void onRemoved(String foodId) {
                                // Update adapter
                                foodAdapter.updateFavoriteStatus(foodId, false);
                                Toast.makeText(getContext(), "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Add to favorites
                        favoriteManager.addFavorite(food, new FavoriteManager.OnFavoriteChangeListener() {
                            @Override
                            public void onAdded(Favorite favorite) {
                                // Update adapter
                                foodAdapter.updateFavoriteStatus(food.getFoodId(), true);
                                Toast.makeText(getContext(), "Đã thêm vào yêu thích! ❤️", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onRemoved(String foodId) {}

                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        binding.recyclerViewFoods.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recyclerViewFoods.setAdapter(foodAdapter);
    }

    private void loadRestaurants() {
        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("approved", true)
                .whereEqualTo("active", true)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    restaurantList.clear();
                    restaurantList.addAll(queryDocumentSnapshots.toObjects(Restaurant.class));
                    restaurantAdapter.notifyDataSetChanged();
                });
    }

    private void loadFoods() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // First get approved restaurants
        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("approved", true)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(restaurantQuery -> {
                    List<String> approvedRestaurantIds = new ArrayList<>();
                    restaurantQuery.forEach(doc -> approvedRestaurantIds.add(doc.getId()));
                    
                    if (approvedRestaurantIds.isEmpty()) {
                        foodList.clear();
                        filteredFoodList.clear();
                        foodAdapter.notifyDataSetChanged();
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.tvEmptyState.setVisibility(View.VISIBLE);
                        }
                        return;
                    }

                    // Then get approved foods from approved restaurants
                    FirebaseUtil.getFirestore().collection("foods")
                            .whereEqualTo("available", true)
                            .whereEqualTo("approved", true)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                foodList.clear();
                                
                                // Filter foods to only include those from approved restaurants
                                for (FoodItem food : queryDocumentSnapshots.toObjects(FoodItem.class)) {
                                    if (approvedRestaurantIds.contains(food.getRestaurantId())) {
                                        foodList.add(food);
                                    }
                                }
                                
                                // Apply filters after loading
                                applyFiltersAfterLoad();

                                if (binding != null) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.tvEmptyState.setVisibility(filteredFoodList.isEmpty() ? View.VISIBLE : View.GONE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                if (binding != null) {
                                    binding.progressBar.setVisibility(View.GONE);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    if (binding != null) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void filterFoods(String query) {
        currentSearchQuery = query;
        
        // Apply all filters (category + search + sort)
        filteredFoodList.clear();
        
        // First apply category filter
        List<FoodItem> categoryFiltered = new ArrayList<>();
        if (selectedCategory == null) {
            categoryFiltered.addAll(foodList);
        } else {
            String canonicalSelected = CategoryUtils.getCanonicalCode(selectedCategory.trim());
            for (FoodItem food : foodList) {
                String foodCategory = CategoryUtils.getCanonicalCode(food.getCategory());
                if (!canonicalSelected.isEmpty() && canonicalSelected.equalsIgnoreCase(foodCategory)) {
                    categoryFiltered.add(food);
                }
            }
        }
        
        // Then apply search filter
        if (query.isEmpty()) {
            filteredFoodList.addAll(categoryFiltered);
        } else {
            String lowerQuery = query.toLowerCase(Locale.US);
            for (FoodItem food : categoryFiltered) {
                String name = food.getName() != null ? food.getName().toLowerCase(Locale.US) : "";
                String categoryCode = CategoryUtils.getCanonicalCode(food.getCategory()).toLowerCase(Locale.US);
                String categoryDisplay = CategoryUtils.getDisplayName(food.getCategory()).toLowerCase(Locale.US);
                String description = food.getDescription() != null ? food.getDescription().toLowerCase(Locale.US) : "";

                if (name.contains(lowerQuery) ||
                        categoryCode.contains(lowerQuery) ||
                        categoryDisplay.contains(lowerQuery) ||
                        description.contains(lowerQuery)) {
                    filteredFoodList.add(food);
                }
            }
        }
        
        // Then apply sort
        switch (sortType) {
            case "price_asc":
                filteredFoodList.sort((f1, f2) -> Double.compare(f1.getPrice(), f2.getPrice()));
                break;
            case "price_desc":
                filteredFoodList.sort((f1, f2) -> Double.compare(f2.getPrice(), f1.getPrice()));
                break;
            case "name_asc":
                filteredFoodList.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                break;
            default:
                // Keep original order
                break;
        }

        foodAdapter.notifyDataSetChanged();
        if (binding != null) {
            binding.tvEmptyState.setVisibility(filteredFoodList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void setupGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        String greeting;
        if (hour < 12) {
            greeting = "Chào buổi sáng! ☀️";
        } else if (hour < 17) {
            greeting = "Chào buổi chiều! 🌤️";
        } else {
            greeting = "Chào buổi tối! 🌙";
        }
        
        binding.tvGreeting.setText(greeting);
    }


    private void setupClickListeners() {
        binding.btnViewAllRestaurants.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), RestaurantListActivity.class);
            startActivity(intent);
        });
        
        binding.btnViewAllFoods.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FoodListActivity.class);
            startActivity(intent);
        });
        
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());
    }
    
    private void showFilterDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_filter_bottom_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        ChipGroup chipGroupCategory = bottomSheetView.findViewById(R.id.chipGroupCategory);
        ChipGroup chipGroupSort = bottomSheetView.findViewById(R.id.chipGroupSort);
        Chip chipCategoryAll = bottomSheetView.findViewById(R.id.chipCategoryAll);
        Chip chipCategoryMain = bottomSheetView.findViewById(R.id.chipCategoryMain);
        Chip chipCategoryDrink = bottomSheetView.findViewById(R.id.chipCategoryDrink);
        Chip chipCategoryDessert = bottomSheetView.findViewById(R.id.chipCategoryDessert);
        Chip chipSortDefault = bottomSheetView.findViewById(R.id.chipSortDefault);
        Chip chipSortPriceAsc = bottomSheetView.findViewById(R.id.chipSortPriceAsc);
        Chip chipSortPriceDesc = bottomSheetView.findViewById(R.id.chipSortPriceDesc);
        Chip chipSortName = bottomSheetView.findViewById(R.id.chipSortName);
        View btnReset = bottomSheetView.findViewById(R.id.btnReset);
        View btnApplyFilter = bottomSheetView.findViewById(R.id.btnApplyFilter);
        
        // Set current selection
        if (selectedCategory == null) {
            chipCategoryAll.setChecked(true);
        } else {
            switch (selectedCategory) {
                case "Main":
                    chipCategoryMain.setChecked(true);
                    break;
                case "Drink":
                    chipCategoryDrink.setChecked(true);
                    break;
                case "Dessert":
                    chipCategoryDessert.setChecked(true);
                    break;
            }
        }
        
        switch (sortType) {
            case "default":
                chipSortDefault.setChecked(true);
                break;
            case "price_asc":
                chipSortPriceAsc.setChecked(true);
                break;
            case "price_desc":
                chipSortPriceDesc.setChecked(true);
                break;
            case "name_asc":
                chipSortName.setChecked(true);
                break;
        }
        
        // Category selection listener
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                chipCategoryAll.setChecked(true);
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipCategoryAll) {
                selectedCategory = null;
            } else if (checkedId == R.id.chipCategoryMain) {
                selectedCategory = "Main";
            } else if (checkedId == R.id.chipCategoryDrink) {
                selectedCategory = "Drink";
            } else if (checkedId == R.id.chipCategoryDessert) {
                selectedCategory = "Dessert";
            }
        });
        
        // Sort selection listener
        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                chipSortDefault.setChecked(true);
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipSortDefault) {
                sortType = "default";
            } else if (checkedId == R.id.chipSortPriceAsc) {
                sortType = "price_asc";
            } else if (checkedId == R.id.chipSortPriceDesc) {
                sortType = "price_desc";
            } else if (checkedId == R.id.chipSortName) {
                sortType = "name_asc";
            }
        });
        
        // Reset button
        btnReset.setOnClickListener(v -> {
            selectedCategory = null;
            sortType = "default";
            chipCategoryAll.setChecked(true);
            chipSortDefault.setChecked(true);
        });
        
        // Apply button
        btnApplyFilter.setOnClickListener(v -> {
            applyFilters();
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetDialog.show();
    }
    
    private void applyFilters() {
        // First apply category filter
        filteredFoodList.clear();
        
        if (selectedCategory == null) {
            // All categories
            filteredFoodList.addAll(foodList);
        } else {
            String canonicalSelected = CategoryUtils.getCanonicalCode(selectedCategory.trim());
            for (FoodItem food : foodList) {
                String foodCategory = CategoryUtils.getCanonicalCode(food.getCategory());
                if (!canonicalSelected.isEmpty() && canonicalSelected.equalsIgnoreCase(foodCategory)) {
                    filteredFoodList.add(food);
                }
            }
        }
        
        // Then apply search filter if exists
        if (!currentSearchQuery.isEmpty()) {
            List<FoodItem> searchFiltered = new ArrayList<>();
            String lowerQuery = currentSearchQuery.toLowerCase(Locale.US);
            for (FoodItem food : filteredFoodList) {
                String name = food.getName() != null ? food.getName().toLowerCase(Locale.US) : "";
                String categoryCode = CategoryUtils.getCanonicalCode(food.getCategory()).toLowerCase(Locale.US);
                String categoryDisplay = CategoryUtils.getDisplayName(food.getCategory()).toLowerCase(Locale.US);
                String description = food.getDescription() != null ? food.getDescription().toLowerCase(Locale.US) : "";

                if (name.contains(lowerQuery) ||
                        categoryCode.contains(lowerQuery) ||
                        categoryDisplay.contains(lowerQuery) ||
                        description.contains(lowerQuery)) {
                    searchFiltered.add(food);
                }
            }
            filteredFoodList.clear();
            filteredFoodList.addAll(searchFiltered);
        }
        
        // Then apply sort
        switch (sortType) {
            case "price_asc":
                filteredFoodList.sort((f1, f2) -> Double.compare(f1.getPrice(), f2.getPrice()));
                break;
            case "price_desc":
                filteredFoodList.sort((f1, f2) -> Double.compare(f2.getPrice(), f1.getPrice()));
                break;
            case "name_asc":
                filteredFoodList.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                break;
            default:
                // Keep original order
                break;
        }
        
        foodAdapter.notifyDataSetChanged();
        if (binding != null) {
            binding.tvEmptyState.setVisibility(filteredFoodList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void applyFiltersAfterLoad() {
        // Apply all filters to newly loaded data
        filteredFoodList.clear();
        
        // First apply category filter
        if (selectedCategory == null) {
            filteredFoodList.addAll(foodList);
        } else {
            String canonicalSelected = CategoryUtils.getCanonicalCode(selectedCategory.trim());
            for (FoodItem food : foodList) {
                String foodCategory = CategoryUtils.getCanonicalCode(food.getCategory());
                if (!canonicalSelected.isEmpty() && canonicalSelected.equalsIgnoreCase(foodCategory)) {
                    filteredFoodList.add(food);
                }
            }
        }
        
        // Then apply search filter if exists
        if (!currentSearchQuery.isEmpty()) {
            List<FoodItem> searchFiltered = new ArrayList<>();
            String lowerQuery = currentSearchQuery.toLowerCase(Locale.US);
            for (FoodItem food : filteredFoodList) {
                String name = food.getName() != null ? food.getName().toLowerCase(Locale.US) : "";
                String categoryCode = CategoryUtils.getCanonicalCode(food.getCategory()).toLowerCase(Locale.US);
                String categoryDisplay = CategoryUtils.getDisplayName(food.getCategory()).toLowerCase(Locale.US);
                String description = food.getDescription() != null ? food.getDescription().toLowerCase(Locale.US) : "";

                if (name.contains(lowerQuery) ||
                        categoryCode.contains(lowerQuery) ||
                        categoryDisplay.contains(lowerQuery) ||
                        description.contains(lowerQuery)) {
                    searchFiltered.add(food);
                }
            }
            filteredFoodList.clear();
            filteredFoodList.addAll(searchFiltered);
        }
        
        // Then apply sort
        switch (sortType) {
            case "price_asc":
                filteredFoodList.sort((f1, f2) -> Double.compare(f1.getPrice(), f2.getPrice()));
                break;
            case "price_desc":
                filteredFoodList.sort((f1, f2) -> Double.compare(f2.getPrice(), f1.getPrice()));
                break;
            case "name_asc":
                filteredFoodList.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                break;
            default:
                // Keep original order
                break;
        }
        
        foodAdapter.notifyDataSetChanged();
    }
    
    private void loadUserProfile() {
        String userId = FirebaseUtil.getCurrentUserId();
        
        FirebaseUtil.getFirestore().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && binding != null) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                            Glide.with(this)
                                    .load(user.getAvatarUrl())
                                    .placeholder(R.drawable.ic_user_placeholder)
                                    .circleCrop()
                                    .into(binding.ivUserAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep default placeholder
                });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}