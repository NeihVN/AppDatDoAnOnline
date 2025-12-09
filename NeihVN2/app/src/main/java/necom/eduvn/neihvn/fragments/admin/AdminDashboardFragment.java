package necom.eduvn.neihvn.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import necom.eduvn.neihvn.databinding.FragmentAdminDashboardBinding;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.FirebaseUtil;

public class AdminDashboardFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadDashboardData();
    }

    private void loadDashboardData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Count Users
        FirebaseUtil.getFirestore().collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalUsers = queryDocumentSnapshots.size();
                    int buyers = 0, sellers = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String role = doc.getString("role");
                        if ("buyer".equals(role)) buyers++;
                        else if ("seller".equals(role)) sellers++;
                    }

                    binding.tvTotalUsers.setText(String.valueOf(totalUsers));
                    binding.tvBuyersCount.setText("Người mua: " + buyers);
                    binding.tvSellersCount.setText("Người bán: " + sellers);
                });

        // Count Restaurants
        FirebaseUtil.getFirestore().collection("restaurants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    int approved = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isApproved = doc.getBoolean("approved");
                        if (isApproved != null && isApproved) approved++;
                    }

                    binding.tvTotalRestaurants.setText(String.valueOf(total));
                    binding.tvPendingApproval.setText("Chờ duyệt: " + (total - approved));
                });

        // Count Orders
        FirebaseUtil.getFirestore().collection("orders")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.tvTotalOrders.setText(String.valueOf(queryDocumentSnapshots.size()));

                    double totalRevenue = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Double amount = doc.getDouble("totalAmount");
                        if (amount != null) totalRevenue += amount;
                    }

                    binding.tvTotalRevenue.setText(CurrencyFormatter.format(totalRevenue));
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}