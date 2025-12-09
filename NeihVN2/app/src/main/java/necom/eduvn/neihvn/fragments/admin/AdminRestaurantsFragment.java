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

import necom.eduvn.neihvn.adapters.RestaurantAdapter;
import necom.eduvn.neihvn.databinding.FragmentAdminRestaurantsBinding;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.NotificationUtil;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class AdminRestaurantsFragment extends Fragment {
    private FragmentAdminRestaurantsBinding binding;
    private RestaurantAdapter adapter;
    private List<Restaurant> restaurantList;
    private List<Restaurant> filteredList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminRestaurantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        restaurantList = new ArrayList<>();
        filteredList = new ArrayList<>();

        setupRecyclerView();
        setupTabs();
        loadRestaurants();
    }

    private void setupRecyclerView() {
        adapter = new RestaurantAdapter(filteredList, new RestaurantAdapter.OnRestaurantActionListener() {
            @Override
            public void onApprove(Restaurant restaurant) {
                approveRestaurant(restaurant);
            }

            @Override
            public void onReject(Restaurant restaurant) {
                rejectRestaurant(restaurant);
            }

            @Override
            public void onView(Restaurant restaurant) {
                Toast.makeText(getContext(), "Xem: " + restaurant.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        binding.recyclerViewRestaurants.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewRestaurants.setAdapter(adapter);
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

    private void loadRestaurants() {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseUtil.getFirestore().collection("restaurants")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (value != null) {
                        restaurantList.clear();
                        restaurantList.addAll(value.toObjects(Restaurant.class));
                        filterByStatus(binding.tabLayout.getSelectedTabPosition());
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void filterByStatus(int position) {
        filteredList.clear();

        switch (position) {
            case 0: // All
                filteredList.addAll(restaurantList);
                break;
            case 1: // Pending
                for (Restaurant r : restaurantList) {
                    // Pending: not approved but still active (waiting for review)
                    if (!r.isApproved() && r.isActive()) {
                        filteredList.add(r);
                    }
                }
                break;
            case 2: // Approved
                for (Restaurant r : restaurantList) {
                    if (r.isApproved() && r.isActive()) {
                        filteredList.add(r);
                    }
                }
                break;
            case 3: // Rejected
                for (Restaurant r : restaurantList) {
                    // Rejected: not approved and not active
                    if (!r.isApproved() && !r.isActive()) {
                        filteredList.add(r);
                    }
                }
                break;
        }

        adapter.notifyDataSetChanged();
        binding.tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void approveRestaurant(Restaurant restaurant) {
        FirebaseUtil.getFirestore().collection("restaurants")
                .document(restaurant.getRestaurantId())
                .update("approved", true, "active", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã duyệt nhà hàng", Toast.LENGTH_SHORT).show();
                    // Send notification to seller
                    NotificationUtil.sendRestaurantApprovedNotification(
                            restaurant.getSellerId(), 
                            restaurant.getRestaurantId(), 
                            restaurant.getName()
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectRestaurant(Restaurant restaurant) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Từ chối nhà hàng")
                .setMessage("Bạn có chắc muốn từ chối \"" + restaurant.getName() + "\"?\nNhà hàng sẽ bị ẩn khỏi người mua nhưng có thể phê duyệt lại sau này.")
                .setPositiveButton("Từ chối", (dialog, which) -> {
                    // Update status instead of deleting: set approved = false, active = false
                    FirebaseUtil.getFirestore().collection("restaurants")
                            .document(restaurant.getRestaurantId())
                            .update("approved", false, "active", false)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Đã từ chối nhà hàng", Toast.LENGTH_SHORT).show();
                                // Send notification to seller
                                NotificationUtil.sendRestaurantRejectedNotification(
                                        restaurant.getSellerId(), 
                                        restaurant.getRestaurantId(), 
                                        restaurant.getName()
                                );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}