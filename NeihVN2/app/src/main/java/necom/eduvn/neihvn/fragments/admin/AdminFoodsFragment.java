package necom.eduvn.neihvn.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import necom.eduvn.neihvn.adapters.AdminFoodAdapter;
import necom.eduvn.neihvn.databinding.FragmentAdminFoodsBinding;
import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.NotificationUtil;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminFoodsFragment extends Fragment {
    private FragmentAdminFoodsBinding binding;
    private AdminFoodAdapter adapter;
    private List<FoodItem> foodList;
    private List<FoodItem> filteredList;
    private Map<String, String> restaurantNames;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminFoodsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodList = new ArrayList<>();
        filteredList = new ArrayList<>();
        restaurantNames = new HashMap<>();

        setupRecyclerView();
        setupTabs();
        loadRestaurantNames();
    }

    private void setupRecyclerView() {
        adapter = new AdminFoodAdapter(filteredList, new AdminFoodAdapter.OnFoodActionListener() {
            @Override
            public void onApprove(FoodItem food) {
                approveFood(food);
            }

            @Override
            public void onReject(FoodItem food) {
                rejectFood(food);
            }

            @Override
            public void onView(FoodItem food) {
                Toast.makeText(getContext(), "Xem: " + food.getName(), Toast.LENGTH_SHORT).show();
            }
        }, restaurantNames);

        binding.recyclerViewFoods.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewFoods.setAdapter(adapter);
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterByStatus(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadRestaurantNames() {
        // First load restaurant names
        FirebaseUtil.getFirestore().collection("restaurants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    restaurantNames.clear();
                    queryDocumentSnapshots.forEach(doc -> {
                        restaurantNames.put(doc.getId(), doc.getString("name"));
                    });
                    loadFoods();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải danh sách nhà hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadFoods(); // Load foods anyway
                });
    }

    private void loadFoods() {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseUtil.getFirestore().collection("foods")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (value != null) {
                        foodList.clear();
                        foodList.addAll(value.toObjects(FoodItem.class));
                        filterByStatus(binding.tabLayout.getSelectedTabPosition());
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void filterByStatus(int position) {
        filteredList.clear();

        switch (position) {
            case 0: // All
                filteredList.addAll(foodList);
                break;
            case 1: // Pending
                for (FoodItem food : foodList) {
                    // Pending: not approved but still available (waiting for review)
                    if (!food.isApproved() && food.isAvailable()) {
                        filteredList.add(food);
                    }
                }
                break;
            case 2: // Approved
                for (FoodItem food : foodList) {
                    if (food.isApproved() && food.isAvailable()) {
                        filteredList.add(food);
                    }
                }
                break;
            case 3: // Rejected
                for (FoodItem food : foodList) {
                    // Rejected: not approved and not available
                    if (!food.isApproved() && !food.isAvailable()) {
                        filteredList.add(food);
                    }
                }
                break;
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        binding.tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void approveFood(FoodItem food) {
        FirebaseUtil.getFirestore().collection("foods")
                .document(food.getFoodId())
                .update("approved", true, "available", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã duyệt món ăn", Toast.LENGTH_SHORT).show();
                    // Get sellerId from restaurant and send notification
                    getSellerId(food.getRestaurantId(), sellerId -> {
                        if (sellerId != null) {
                            NotificationUtil.sendFoodApprovedNotification(
                                    sellerId, 
                                    food.getFoodId(), 
                                    food.getName()
                            );
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectFood(FoodItem food) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Từ chối món ăn")
                .setMessage("Bạn có chắc muốn từ chối \"" + food.getName() + "\"?\nMón ăn sẽ bị ẩn khỏi người mua nhưng có thể phê duyệt lại sau này.")
                .setPositiveButton("Từ chối", (dialog, which) -> {
                    // Update status instead of deleting: set approved = false, available = false
                    FirebaseUtil.getFirestore().collection("foods")
                            .document(food.getFoodId())
                            .update("approved", false, "available", false)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Đã từ chối món ăn", Toast.LENGTH_SHORT).show();
                                // Get sellerId and send notification
                                getSellerId(food.getRestaurantId(), sellerId -> {
                                    if (sellerId != null) {
                                        NotificationUtil.sendFoodRejectedNotification(
                                                sellerId, 
                                                food.getFoodId(), 
                                                food.getName()
                                        );
                                    }
                                });
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void getSellerId(String restaurantId, SellerIdCallback callback) {
        FirebaseUtil.getFirestore().collection("restaurants")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String sellerId = documentSnapshot.getString("sellerId");
                        callback.onResult(sellerId);
                    } else {
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    private interface SellerIdCallback {
        void onResult(String sellerId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
