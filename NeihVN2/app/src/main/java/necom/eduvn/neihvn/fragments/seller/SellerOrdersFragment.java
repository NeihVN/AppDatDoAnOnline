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
import androidx.recyclerview.widget.LinearLayoutManager;

import necom.eduvn.neihvn.adapters.OrderAdapter;
import necom.eduvn.neihvn.databinding.FragmentOrdersBinding;
import necom.eduvn.neihvn.models.Order;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class SellerOrdersFragment extends Fragment {
    private FragmentOrdersBinding binding;
    private OrderAdapter adapter;
    private List<Order> orderList;
    private List<Order> filteredList;
    private String restaurantId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        orderList = new ArrayList<>();
        filteredList = new ArrayList<>();

        setupRecyclerView();
        setupTabs();
        loadRestaurantId();
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(filteredList, true, new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onUpdateStatus(Order order, String newStatus) {
                updateOrderStatus(order, newStatus);
            }

            @Override
            public void onViewDetails(Order order) {
                showOrderDetails(order);
            }
        });

        binding.recyclerViewOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewOrders.setAdapter(adapter);
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

    private void loadRestaurantId() {
        String userId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        restaurantId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        loadOrders();
                    }
                });
    }

    private void loadOrders() {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseUtil.getFirestore().collection("orders")
                .whereEqualTo("restaurantId", restaurantId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (value != null) {
                        orderList.clear();
                        orderList.addAll(value.toObjects(Order.class));
                        filterByStatus(binding.tabLayout.getSelectedTabPosition());
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void filterByStatus(int position) {
        filteredList.clear();

        String status = null;
        switch (position) {
            case 0: // All
                filteredList.addAll(orderList);
                break;
            case 1: // Processing
                status = "Processing";
                break;
            case 2: // Delivering
                status = "Delivering";
                break;
            case 3: // Completed
                status = "Completed";
                break;
            case 4: // Cancelled
                status = "Cancelled";
                break;
        }

        if (status != null) {
            for (Order order : orderList) {
                if (status.equals(order.getStatus())) {
                    filteredList.add(order);
                }
            }
        }

        adapter.notifyDataSetChanged();
        binding.tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateOrderStatus(Order order, String newStatus) {
        FirebaseUtil.getFirestore().collection("orders")
                .document(order.getOrderId())
                .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Đã cập nhật trạng thái đơn", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showOrderDetails(Order order) {
        // Navigate to OrderDetailActivity
        if (getContext() != null && order != null && order.getOrderId() != null) {
            Intent intent = new Intent(getContext(), necom.eduvn.neihvn.activities.OrderDetailActivity.class);
            intent.putExtra("orderId", order.getOrderId());
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}