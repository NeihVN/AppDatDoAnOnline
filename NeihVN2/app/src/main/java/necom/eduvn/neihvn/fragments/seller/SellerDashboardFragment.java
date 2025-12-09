package necom.eduvn.neihvn.fragments.seller;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.FragmentSellerDashboardBinding;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;

public class SellerDashboardFragment extends Fragment {
    private static final String TAG = "SellerDashboardFragment";
    private FragmentSellerDashboardBinding binding;
    private String restaurantId;
    private int loadingTasksCount = 0; // Track how many async tasks are running

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSellerDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadRestaurantAndStats();
    }

    private void loadRestaurantAndStats() {
        if (binding == null) return;
        
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);
        
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            if (binding != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setText("Vui lòng đăng nhập để xem bảng điều khiển");
            }
            return;
        }

        Log.d(TAG, "Loading restaurant for userId: " + userId);

        // Get restaurant
        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    
                    Log.d(TAG, "Restaurant query completed. Found: " + queryDocumentSnapshots.size() + " restaurants");
                    
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        restaurantId = doc.getId();
                        
                        Log.d(TAG, "Restaurant ID: " + restaurantId);
                        
                        // Restaurant name
                        String name = doc.getString("name");
                        binding.tvRestaurantName.setText(name != null ? name : "Nhà hàng chưa xác định");
                        
                        // Rating
                        Double rating = doc.getDouble("rating");
                        Long totalReviewsLong = doc.getLong("totalReviews");
                        Integer totalReviews = totalReviewsLong != null ? totalReviewsLong.intValue() : 0;
                        binding.tvRating.setText(String.format("⭐ %.1f (%d)", rating != null ? rating : 0.0, totalReviews));
                        
                        // Restaurant status
                        Boolean isApproved = doc.getBoolean("approved");
                        Boolean isActive = doc.getBoolean("active");
                        String statusText;
                        int statusColor;
                        if (isApproved != null && isApproved && isActive != null && isActive) {
                            statusText = "Đã duyệt ✓";
                            statusColor = ContextCompat.getColor(requireContext(), R.color.status_success);
                        } else if (isApproved != null && !isApproved && isActive != null && !isActive) {
                            statusText = "Bị từ chối ✗";
                            statusColor = ContextCompat.getColor(requireContext(), R.color.status_error);
                        } else {
                            statusText = "Đang chờ duyệt";
                            statusColor = ContextCompat.getColor(requireContext(), R.color.status_warning);
                        }
                        binding.tvRestaurantStatus.setText(statusText);
                        binding.tvRestaurantStatus.setTextColor(statusColor);
                        
                        // Restaurant address and phone
                        String address = doc.getString("address");
                        String phone = doc.getString("phone");
                        binding.tvRestaurantAddress.setText(address != null && !address.isEmpty() ? address : "Chưa có địa chỉ");
                        binding.tvRestaurantPhone.setText(phone != null && !phone.isEmpty() ? phone : "Chưa có số điện thoại");

                        // Load stats after restaurant is loaded
                        loadStats();
                    } else {
                        Log.w(TAG, "No restaurant found for user");
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setText("Không tìm thấy nhà hàng. Vui lòng tạo mới.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading restaurant: " + e.getMessage(), e);
                    if (binding != null) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setText("Lỗi tải nhà hàng: " + e.getMessage());
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadStats() {
        if (binding == null) return;
        
        if (restaurantId == null || restaurantId.isEmpty()) {
            Log.e(TAG, "Restaurant ID is null, cannot load stats");
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Loading stats for restaurant: " + restaurantId);
        
        loadingTasksCount = 0; // Reset counter
        int totalTasks = 2; // All orders (includes today's calculation), menu items

        // Calculate start of today
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        // Load all orders - then filter today's orders on client side to avoid index issues
        loadingTasksCount++;
        FirebaseUtil.getFirestore().collection("orders")
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    
                    Log.d(TAG, "All orders loaded: " + queryDocumentSnapshots.size());
                    
                    int totalOrders = queryDocumentSnapshots.size();
                    double totalRevenue = 0;
                    double todayRevenue = 0;
                    int todayOrders = 0;
                    int processingOrders = 0;
                    int deliveringOrders = 0;
                    int completedOrders = 0;
                    int cancelledOrders = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Double amount = doc.getDouble("totalAmount");
                        if (amount != null) {
                            totalRevenue += amount;
                        }
                        
                        // Check if order is from today
                        Long createdAt = doc.getLong("createdAt");
                        if (createdAt != null && createdAt >= startOfDay) {
                            todayOrders++;
                            if (amount != null) {
                                todayRevenue += amount;
                            }
                        }
                        
                        String status = doc.getString("status");
                        if (status != null) {
                            // Trim status to handle any whitespace issues
                            status = status.trim();
                            switch (status) {
                                case "Processing":
                                case "Pending Payment":
                                    processingOrders++;
                                    break;
                                case "Delivering":
                                    deliveringOrders++;
                                    break;
                                case "Completed":
                                    completedOrders++;
                                    break;
                                case "Cancelled":
                                    cancelledOrders++;
                                    break;
                                default:
                                    Log.w(TAG, "Unknown order status: '" + status + "'");
                                    break;
                            }
                        } else {
                            Log.w(TAG, "Order " + doc.getId() + " has no status field");
                        }
                    }

                    // Update all stats at once
                    Log.d(TAG, "Updating stats - Processing: " + processingOrders + 
                          ", Delivering: " + deliveringOrders + 
                          ", Completed: " + completedOrders + 
                          ", Cancelled: " + cancelledOrders);
                    
                    binding.tvTodayRevenue.setText(CurrencyFormatter.format(todayRevenue));
                    binding.tvTodayOrders.setText(String.valueOf(todayOrders));
                    binding.tvTotalOrders.setText(String.valueOf(totalOrders));
                    binding.tvTotalRevenue.setText(CurrencyFormatter.format(totalRevenue));
                    
                    // Update order status breakdown
                    if (binding.tvProcessingOrders != null) {
                        binding.tvProcessingOrders.setText(String.valueOf(processingOrders));
                        Log.d(TAG, "Processing orders set to: " + processingOrders);
                    } else {
                        Log.e(TAG, "tvProcessingOrders is null!");
                    }
                    
                    if (binding.tvDeliveringOrders != null) {
                        binding.tvDeliveringOrders.setText(String.valueOf(deliveringOrders));
                        Log.d(TAG, "Delivering orders set to: " + deliveringOrders);
                    } else {
                        Log.e(TAG, "tvDeliveringOrders is null!");
                    }
                    
                    if (binding.tvCancelledOrders != null) {
                        binding.tvCancelledOrders.setText(String.valueOf(cancelledOrders));
                        Log.d(TAG, "Cancelled orders set to: " + cancelledOrders);
                    } else {
                        Log.e(TAG, "tvCancelledOrders is null!");
                    }
                    
                    if (binding.tvCompletedOrders != null) {
                        binding.tvCompletedOrders.setText(String.valueOf(completedOrders));
                        Log.d(TAG, "Completed orders set to: " + completedOrders);
                    } else {
                        Log.e(TAG, "tvCompletedOrders is null!");
                    }
                    
                    checkAllTasksCompleted(totalTasks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders: " + e.getMessage(), e);
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("index")) {
                        Log.w(TAG, "Index error detected. This may require creating a composite index in Firestore.");
                        // Still show error but try to continue
                    }
                    if (binding != null) {
                        binding.tvTodayRevenue.setText(CurrencyFormatter.format(0));
                        binding.tvTodayOrders.setText("0");
                        binding.tvTotalOrders.setText("0");
                        binding.tvTotalRevenue.setText(CurrencyFormatter.format(0));
                        binding.tvProcessingOrders.setText("0");
                        binding.tvDeliveringOrders.setText("0");
                        binding.tvCompletedOrders.setText("0");
                        binding.tvCancelledOrders.setText("0");
                        checkAllTasksCompleted(totalTasks);
                    }
                });

        // Menu items statistics
        loadingTasksCount++;
        FirebaseUtil.getFirestore().collection("foods")
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    
                    Log.d(TAG, "Menu items loaded: " + queryDocumentSnapshots.size());
                    
                    int totalItems = queryDocumentSnapshots.size();
                    int approvedItems = 0;
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean approved = doc.getBoolean("approved");
                        if (approved != null && approved) {
                            approvedItems++;
                        }
                    }
                    
                    binding.tvTotalMenuItems.setText(String.valueOf(totalItems));
                    binding.tvApprovedMenuItems.setText(String.valueOf(approvedItems));
                    
                    checkAllTasksCompleted(totalTasks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading menu items: " + e.getMessage(), e);
                    if (binding != null) {
                        binding.tvTotalMenuItems.setText("0");
                        binding.tvApprovedMenuItems.setText("0");
                        checkAllTasksCompleted(totalTasks);
                    }
                });
    }

    private void checkAllTasksCompleted(int totalTasks) {
        loadingTasksCount--;
        if (binding != null && loadingTasksCount <= 0) {
            binding.progressBar.setVisibility(View.GONE);
            Log.d(TAG, "All stats loaded successfully");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}