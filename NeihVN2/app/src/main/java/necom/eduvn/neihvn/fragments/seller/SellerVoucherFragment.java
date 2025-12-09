package necom.eduvn.neihvn.fragments.seller;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import necom.eduvn.neihvn.activities.VoucherFormActivity;
import necom.eduvn.neihvn.adapters.VoucherAdapter;
import necom.eduvn.neihvn.databinding.FragmentSellerVoucherBinding;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.models.Voucher;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.VoucherManager;

public class SellerVoucherFragment extends Fragment implements VoucherAdapter.OnVoucherActionListener {
    
    private static final String TAG = "SellerVoucherFragment";
    private FragmentSellerVoucherBinding binding;
    private VoucherAdapter adapter;
    private List<Voucher> voucherList;
    private String restaurantId;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSellerVoucherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        voucherList = new ArrayList<>();
        setupRecyclerView();
        loadRestaurantId();
        
        binding.fabAddVoucher.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked. RestaurantId: " + restaurantId);
            if (restaurantId != null) {
                Intent intent = new Intent(getContext(), VoucherFormActivity.class);
                intent.putExtra("restaurantId", restaurantId);
                Log.d(TAG, "Starting VoucherFormActivity with restaurantId: " + restaurantId);
                startActivity(intent);
            } else {
                Log.w(TAG, "RestaurantId is null, cannot create voucher");
                Toast.makeText(getContext(), "Đang tải thông tin nhà hàng. Vui lòng đợi...", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (restaurantId != null) {
            loadVouchers();
        }
    }
    
    private void setupRecyclerView() {
        adapter = new VoucherAdapter(voucherList, this);
        binding.recyclerViewVouchers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewVouchers.setAdapter(adapter);
    }
    
    private void loadRestaurantId() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        String userId = FirebaseUtil.getCurrentUserId();
        Log.d(TAG, "Loading restaurant for userId: " + userId);
        
        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Restaurant query returned " + queryDocumentSnapshots.size() + " results");
                    
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Restaurant restaurant = queryDocumentSnapshots.getDocuments()
                                .get(0).toObject(Restaurant.class);
                        if (restaurant != null) {
                            restaurantId = restaurant.getRestaurantId();
                            Log.d(TAG, "Restaurant loaded successfully. ID: " + restaurantId);
                            loadVouchers();
                        } else {
                            Log.e(TAG, "Restaurant object is null");
                        }
                    } else {
                        Log.w(TAG, "No restaurant found for this seller");
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.tvEmptyState.setVisibility(View.VISIBLE);
                            binding.tvEmptyState.setText("Bạn chưa có nhà hàng\nHãy đăng ký nhà hàng để tạo voucher");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading restaurant: " + e.getMessage(), e);
                    if (binding != null && getContext() != null) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadVouchers() {
        if (binding == null) return;
        
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);
        
        // Load all vouchers for this restaurant (not just active ones)
        FirebaseUtil.getFirestore().collection("vouchers")
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    
                    voucherList.clear();
                    queryDocumentSnapshots.forEach(doc -> {
                        Voucher voucher = doc.toObject(Voucher.class);
                        voucher.setVoucherId(doc.getId());
                        voucherList.add(voucher);
                    });
                    
                    adapter.updateList(voucherList);
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvEmptyState.setVisibility(voucherList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (binding != null && getContext() != null) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    @Override
    public void onEditVoucher(Voucher voucher) {
        Intent intent = new Intent(getContext(), VoucherFormActivity.class);
        intent.putExtra("restaurantId", restaurantId);
        intent.putExtra("voucherId", voucher.getVoucherId());
        startActivity(intent);
    }
    
    @Override
    public void onDeleteVoucher(Voucher voucher) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa voucher")
                .setMessage("Bạn có chắc muốn xóa voucher \"" + voucher.getCode() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    FirebaseUtil.getFirestore().collection("vouchers")
                            .document(voucher.getVoucherId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Đã xóa voucher", Toast.LENGTH_SHORT).show();
                                    loadVouchers();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    @Override
    public void onToggleActive(Voucher voucher) {
        boolean newStatus = !voucher.isActive();
        
        FirebaseUtil.getFirestore().collection("vouchers")
                .document(voucher.getVoucherId())
                .update("active", newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        String message = newStatus ? "Đã kích hoạt voucher" : "Đã vô hiệu hóa voucher";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        loadVouchers();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

