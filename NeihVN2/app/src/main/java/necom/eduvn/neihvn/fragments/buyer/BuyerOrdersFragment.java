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
import androidx.recyclerview.widget.LinearLayoutManager;
import necom.eduvn.neihvn.activities.OrderDetailActivity;
import necom.eduvn.neihvn.adapters.OrderAdapter;
import necom.eduvn.neihvn.databinding.FragmentBuyerOrdersBinding;
import necom.eduvn.neihvn.models.Order;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class BuyerOrdersFragment extends Fragment {
    private FragmentBuyerOrdersBinding binding;
    private OrderAdapter adapter;
    private List<Order> orderList;
    private List<Order> filteredList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        orderList = new ArrayList<>();
        filteredList = new ArrayList<>();

        setupRecyclerView();
        setupTabs();
        loadOrders();
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(filteredList, false, new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onUpdateStatus(Order order, String newStatus) {
                // Buyer cannot update status
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
        // Check if tabLayout exists
        if (binding.tabLayout == null) {
            Toast.makeText(getContext(), "Không tìm thấy TabLayout trong layout", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void loadOrders() {
        if (binding.progressBar != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        String userId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("orders")
                .whereEqualTo("buyerId", userId)
                .addSnapshotListener((value, error) -> {
                    if (binding == null) return; // Fragment destroyed

                    if (binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.GONE);
                    }

                    if (error != null) {
                        Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        orderList.clear();
                        orderList.addAll(value.toObjects(Order.class));

                        // Get current tab position safely
                        int currentTab = 0;
                        if (binding.tabLayout != null) {
                            currentTab = binding.tabLayout.getSelectedTabPosition();
                        }
                        filterByStatus(currentTab);
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

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (binding.tvEmptyState != null) {
            binding.tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showOrderDetails(Order order) {
        if (getContext() != null && order != null && order.getOrderId() != null) {
            Intent intent = new Intent(getContext(), OrderDetailActivity.class);
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