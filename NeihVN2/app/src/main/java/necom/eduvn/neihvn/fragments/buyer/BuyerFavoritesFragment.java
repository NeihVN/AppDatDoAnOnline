package necom.eduvn.neihvn.fragments.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.activities.FoodDetailActivity;
import necom.eduvn.neihvn.adapters.FoodAdapter;
import necom.eduvn.neihvn.databinding.FragmentBuyerFavoritesBinding;
import necom.eduvn.neihvn.models.Favorite;
import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.utils.CartManager;
import necom.eduvn.neihvn.utils.FavoriteManager;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuyerFavoritesFragment extends Fragment {
    private FragmentBuyerFavoritesBinding binding;
    private FoodAdapter adapter;
    private List<FoodItem> favoriteFoodList;
    private FavoriteManager favoriteManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        favoriteFoodList = new ArrayList<>();
        favoriteManager = FavoriteManager.getInstance();

        setupRecyclerView();
        loadFavorites();
    }

    private void setupRecyclerView() {
        adapter = new FoodAdapter(favoriteFoodList, false, new FoodAdapter.OnFoodActionListener() {
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
                CartManager cartManager = CartManager.getInstance();
                cartManager.addItem(food, food.getRestaurantId());
                
                Toast.makeText(getContext(), "Đã thêm " + food.getName() + " vào giỏ hàng! 🛒", Toast.LENGTH_SHORT).show();
                
                if (getActivity() != null && getActivity() instanceof necom.eduvn.neihvn.activities.BuyerMainActivity) {
                    ((necom.eduvn.neihvn.activities.BuyerMainActivity) getActivity()).updateCartBadge();
                }
            }

            @Override
            public void onToggleFavorite(FoodItem food) {
                favoriteManager.isFavorite(food.getFoodId(), isFavorite -> {
                    if (isFavorite) {
                        // Remove from favorites
                        favoriteManager.removeFavorite(food.getFoodId(), new FavoriteManager.OnFavoriteChangeListener() {
                            @Override
                            public void onAdded(Favorite favorite) {}

                            @Override
                            public void onRemoved(String foodId) {
                                // Remove from list
                                favoriteFoodList.removeIf(f -> f.getFoodId().equals(foodId));
                                adapter.notifyDataSetChanged();
                                
                                if (favoriteFoodList.isEmpty()) {
                                    binding.layoutEmptyState.setVisibility(View.VISIBLE);
                                    binding.recyclerViewFavorites.setVisibility(View.GONE);
                                }
                                
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

        binding.recyclerViewFavorites.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recyclerViewFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        if (isLoading) return; // Prevent concurrent loads
        
        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.recyclerViewFavorites.setVisibility(View.VISIBLE);
        
        // Clear existing list first
        favoriteFoodList.clear();
        adapter.notifyDataSetChanged();

        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            isLoading = false;
            binding.progressBar.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewFavorites.setVisibility(View.GONE);
            return;
        }

        // Load favorites from Firestore
        FirebaseUtil.getFirestore().collection("favorites")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) {
                        isLoading = false;
                        return;
                    }

                    binding.progressBar.setVisibility(View.GONE);
                    isLoading = false;
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        favoriteFoodList.clear();
                        adapter.notifyDataSetChanged();
                        binding.layoutEmptyState.setVisibility(View.VISIBLE);
                        binding.recyclerViewFavorites.setVisibility(View.GONE);
                        return;
                    }

                    // Get all favorite food IDs (using Set to avoid duplicates)
                    Set<String> favoriteFoodIdSet = new HashSet<>();
                    Map<String, Favorite> favoriteMap = new HashMap<>();
                    
                    for (necom.eduvn.neihvn.models.Favorite favorite : queryDocumentSnapshots.toObjects(necom.eduvn.neihvn.models.Favorite.class)) {
                        if (favorite.getFoodId() != null) {
                            favoriteFoodIdSet.add(favorite.getFoodId());
                            favoriteMap.put(favorite.getFoodId(), favorite);
                        }
                    }

                    if (favoriteFoodIdSet.isEmpty()) {
                        favoriteFoodList.clear();
                        adapter.notifyDataSetChanged();
                        binding.layoutEmptyState.setVisibility(View.VISIBLE);
                        binding.recyclerViewFavorites.setVisibility(View.GONE);
                        return;
                    }

                    // Convert to List and load food items
                    List<String> favoriteFoodIds = new ArrayList<>(favoriteFoodIdSet);
                    loadFavoriteFoodItems(favoriteFoodIds, favoriteMap);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) {
                        isLoading = false;
                        return;
                    }
                    
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải danh sách yêu thích: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.layoutEmptyState.setVisibility(View.VISIBLE);
                    binding.recyclerViewFavorites.setVisibility(View.GONE);
                });
    }

    private void loadFavoriteFoodItems(List<String> foodIds, Map<String, Favorite> favoriteMap) {
        // Clear list and use a temporary list to avoid duplicates
        favoriteFoodList.clear();
        List<FoodItem> tempFoodList = new ArrayList<>();
        Set<String> loadedFoodIds = new HashSet<>();
        
        if (foodIds.isEmpty()) {
            adapter.notifyDataSetChanged();
            if (binding != null) {
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewFavorites.setVisibility(View.GONE);
            }
            return;
        }
        
        // Remove duplicates from foodIds
        List<String> uniqueFoodIds = new ArrayList<>(new HashSet<>(foodIds));
        
        // Load foods by document ID (foodId is the document ID in Firestore)
        int[] loadedCount = {0};
        int totalCount = uniqueFoodIds.size();
        
        for (String foodId : uniqueFoodIds) {
            FirebaseUtil.getFirestore().collection("foods")
                    .document(foodId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (binding == null) return;
                        
                        synchronized (loadedFoodIds) {
                            // Check if already loaded to prevent duplicates
                            if (loadedFoodIds.contains(foodId)) {
                                loadedCount[0]++;
                                if (loadedCount[0] >= totalCount) {
                                    updateUI(tempFoodList);
                                }
                                return;
                            }
                            
                            loadedCount[0]++;
                            
                            if (documentSnapshot.exists()) {
                                FoodItem food = documentSnapshot.toObject(FoodItem.class);
                                if (food != null) {
                                    // Set foodId from document ID if not set
                                    food.setFoodId(documentSnapshot.getId());
                                    
                                    // Only add approved and available foods
                                    if (food.isApproved() && food.isAvailable() && !loadedFoodIds.contains(foodId)) {
                                        loadedFoodIds.add(foodId);
                                        tempFoodList.add(food);
                                    }
                                }
                            }
                            
                            // Update UI when all foods are loaded
                            if (loadedCount[0] >= totalCount) {
                                updateUI(tempFoodList);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (binding == null) return;
                        Log.e("BuyerFavoritesFragment", "Error loading food item " + foodId + ": " + e.getMessage());
                        
                        synchronized (loadedFoodIds) {
                            if (!loadedFoodIds.contains(foodId)) {
                                loadedCount[0]++;
                            }
                            
                            // Update UI even if some items fail
                            if (loadedCount[0] >= totalCount) {
                                updateUI(tempFoodList);
                            }
                        }
                    });
        }
    }
    
    private void updateUI(List<FoodItem> tempFoodList) {
        if (binding == null) return;
        
        // Update the main list
        favoriteFoodList.clear();
        favoriteFoodList.addAll(tempFoodList);
        
        adapter.notifyDataSetChanged();
        
        if (favoriteFoodList.isEmpty()) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewFavorites.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewFavorites.setVisibility(View.VISIBLE);
        }
    }

    private boolean isLoading = false;
    
    @Override
    public void onResume() {
        super.onResume();
        // Only reload if not currently loading to prevent duplicate loads
        if (!isLoading) {
            loadFavorites();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

