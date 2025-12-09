package necom.eduvn.neihvn.fragments.seller;

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

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.activities.FoodFormActivity;
import necom.eduvn.neihvn.adapters.FoodAdapter;
import necom.eduvn.neihvn.databinding.FragmentSellerMenuBinding;
import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.utils.CategoryUtils;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SellerMenuFragment extends Fragment {
    private FragmentSellerMenuBinding binding;
    private FoodAdapter adapter;
    private List<FoodItem> foodList;
    private List<FoodItem> filteredList;
    private String restaurantId;
    private boolean isRestaurantApproved = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSellerMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodList = new ArrayList<>();
        filteredList = new ArrayList<>();

        setupRecyclerView();
        setupTabs();
        loadRestaurantId();

        binding.fabAddFood.setOnClickListener(v -> {
            if (restaurantId != null && isRestaurantApproved) {
                Intent intent = new Intent(getContext(), FoodFormActivity.class);
                intent.putExtra("restaurantId", restaurantId);
                startActivity(intent);
            } else if (restaurantId != null && !isRestaurantApproved) {
                Toast.makeText(getContext(), "⏳ Nhà hàng của bạn đang chờ quản trị viên phê duyệt trước khi có thể thêm món ăn", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Vui lòng chờ trong khi hệ thống tải thông tin nhà hàng...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new FoodAdapter(filteredList, true, new FoodAdapter.OnFoodActionListener() {
            @Override
            public void onEdit(FoodItem food) {
                if (restaurantId != null && food.getFoodId() != null) {
                    Intent intent = new Intent(getContext(), FoodFormActivity.class);
                    intent.putExtra("restaurantId", restaurantId);
                    intent.putExtra("foodId", food.getFoodId());
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Không thể chỉnh sửa món này. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDelete(FoodItem food) {
                if (food != null && food.getFoodId() != null) {
                    deleteFood(food);
                } else {
                    Toast.makeText(getContext(), "Không thể xóa món này.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onToggleAvailability(FoodItem food) {
                if (food != null && food.getFoodId() != null) {
                    toggleAvailability(food);
                } else {
                    Toast.makeText(getContext(), "Không thể cập nhật trạng thái bán.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onClick(FoodItem food) {
                // Navigate to FoodDetailActivity
                if (getContext() != null && food != null && food.getFoodId() != null) {
                    Intent intent = new Intent(getContext(), necom.eduvn.neihvn.activities.FoodDetailActivity.class);
                    intent.putExtra("foodId", food.getFoodId());
                    intent.putExtra("restaurantId", food.getRestaurantId());
                    intent.putExtra("isSellerMode", true);
                    startActivity(intent);
                }
            }

            @Override
            public void onAddToCart(FoodItem food) {

            }

            @Override
            public void onToggleFavorite(FoodItem food) {

            }
        });

        binding.recyclerViewMenu.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recyclerViewMenu.setAdapter(adapter);
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
    }

    private void loadRestaurantId() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String userId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return; // Fragment destroyed
                    
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Restaurant restaurant = queryDocumentSnapshots.getDocuments().get(0).toObject(Restaurant.class);
                        if (restaurant != null) {
                            restaurantId = restaurant.getRestaurantId();
                            isRestaurantApproved = restaurant.isApproved();
                            
                            if (isRestaurantApproved) {
                                loadFoods();
                            } else {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.tvEmptyState.setVisibility(View.VISIBLE);
                                binding.tvEmptyState.setText("🏪 Nhà hàng đang chờ duyệt\n\nNhà hàng của bạn đang được quản trị viên xem xét.\nBạn có thể thêm món sau khi được phê duyệt!");
                                binding.fabAddFood.setVisibility(View.VISIBLE); // Show FAB but disabled
                            }
                        }
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setText("Chưa có nhà hàng nào. Vui lòng tạo nhà hàng trước.");
                        binding.fabAddFood.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return; // Fragment destroyed
                    
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải nhà hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFoods() {
        FirebaseUtil.getFirestore().collection("foods")
                .whereEqualTo("restaurantId", restaurantId)
                .addSnapshotListener((value, error) -> {
                    if (binding == null) return; // Fragment destroyed
                    
                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (error != null) {
                        Toast.makeText(getContext(), "Lỗi tải món ăn: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setText("Không thể tải danh sách món ăn. Vui lòng thử lại.");
                        return;
                    }

                    if (value != null) {
                        foodList.clear();
                        foodList.addAll(value.toObjects(FoodItem.class));
                        filterByCategory(binding.tabLayout.getSelectedTabPosition());
                        
                        // Show/hide FAB based on data
                        binding.fabAddFood.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void filterByCategory(int position) {
        filteredList.clear();

        if (position == 0) { // All
            filteredList.addAll(foodList);
        } else {
            String categoryCode = position == 1 ? "Main" : position == 2 ? "Drink" : "Dessert";
            String canonical = CategoryUtils.getCanonicalCode(categoryCode);
            
            for (FoodItem food : foodList) {
                String foodCategory = CategoryUtils.getCanonicalCode(food.getCategory());
                if (foodCategory != null) {
                    foodCategory = foodCategory.trim();
                    // Case-insensitive comparison
                    if (!canonical.isEmpty() && canonical.equalsIgnoreCase(foodCategory)) {
                        filteredList.add(food);
                    }
                }
            }
        }

        adapter.notifyDataSetChanged();
        
        // Update empty state
        if (filteredList.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            if (foodList.isEmpty()) {
                binding.tvEmptyState.setText("Chưa có món nào.\nNhấn dấu + để thêm món đầu tiên! 🍽️");
            } else {
                String categoryCode = position == 1 ? "Main" : position == 2 ? "Drink" : "Dessert";
                String displayName = CategoryUtils.getDisplayName(categoryCode);
                binding.tvEmptyState.setText("Không tìm thấy món thuộc danh mục " + displayName + ".\nHãy thêm vài món " + displayName + " hấp dẫn! 😋");
            }
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
        }
        
        // Update title with count
        updateTitle();
    }
    
    private void updateTitle() {
        String title = "Thực đơn (" + filteredList.size() + ")";
        if (getActivity() != null && getActivity().getActionBar() != null) {
            getActivity().getActionBar().setTitle(title);
        }
    }

    private void deleteFood(FoodItem food) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa món ăn")
                .setMessage("Bạn có chắc muốn xóa \"" + food.getName() + "\"?\n\nHành động này không thể hoàn tác.")
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    // Show loading state
                    binding.progressBar.setVisibility(View.VISIBLE);
                    
                    FirebaseUtil.getFirestore().collection("foods")
                            .document(food.getFoodId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (binding == null) return; // Fragment destroyed
                                
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), String.format(Locale.getDefault(), "✅ Đã xóa %s thành công", food.getName()), Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null) return; // Fragment destroyed
                                
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "❌ Không thể xóa: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void toggleAvailability(FoodItem food) {
        boolean newStatus = !food.isAvailable();
        String actionText = newStatus ? "đang mở bán" : "tạm ngừng bán";
        String emoji = newStatus ? "✅" : "❌";

        FirebaseUtil.getFirestore().collection("foods")
                .document(food.getFoodId())
                .update("available", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), 
                            emoji + " " + food.getName() + " hiện " + actionText, 
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), 
                            "❌ Không thể cập nhật trạng thái bán: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to fragment
        if (restaurantId != null && isRestaurantApproved) {
            loadFoods();
        } else {
            loadRestaurantId(); // Re-check restaurant approval status
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}