package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;

import android.content.Intent;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.adapters.FoodReviewAdapter;
import necom.eduvn.neihvn.adapters.OrderItemDetailAdapter;
import necom.eduvn.neihvn.databinding.ActivityOrderDetailBinding;
import necom.eduvn.neihvn.models.Order;
import necom.eduvn.neihvn.models.OrderItem;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.models.Review;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.recyclerview.widget.LinearLayoutManager;

public class OrderDetailActivity extends AppCompatActivity {
    private ActivityOrderDetailBinding binding;
    private Order order;
    private OrderItemDetailAdapter adapter;
    private FoodReviewAdapter foodReviewAdapter;
    private String orderId;
    private boolean isSellerMode = false;
    private String restaurantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("orderId");
        
        // Check if user is seller
        checkSellerMode();
        
        setupToolbar();
        setupRecyclerView();
        
        if (orderId != null) {
            loadOrderDetails();
        } else {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkSellerMode() {
        String currentUserId = FirebaseUtil.getCurrentUserId();
        if (currentUserId != null) {
            // Check if user owns a restaurant
            FirebaseUtil.getFirestore().collection("restaurants")
                    .whereEqualTo("sellerId", currentUserId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            isSellerMode = true;
                            restaurantId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            // Update UI when order is loaded
                            if (order != null) {
                                setupSellerControls();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Not a seller, keep default
                        isSellerMode = false;
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh review section when returning from ReviewActivity
        if (order != null && "Completed".equals(order.getStatus())) {
            checkExistingRestaurantReview();
            loadFoodReviews();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        binding.recyclerViewOrderItems.setLayoutManager(new LinearLayoutManager(this));
        
        // Setup food reviews recycler view - will be initialized when order is loaded
        binding.recyclerViewFoodReviews.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadOrderDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseUtil.getFirestore().collection("orders")
                .document(orderId)
                .addSnapshotListener(this, (documentSnapshot, e) -> {
                    if (binding == null) return; // Activity destroyed
                    
                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (e != null) {
                        Toast.makeText(this, "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        order = documentSnapshot.toObject(Order.class);
                        if (order != null) {
                            displayOrderInfo();
                            loadRestaurantInfo();
                            // Setup seller controls after order is loaded
                            if (isSellerMode) {
                                setupSellerControls();
                            }
                        } else {
                            Toast.makeText(this, "Không thể tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayOrderInfo() {
        // Order ID
        String shortId = order.getOrderId().substring(0, 8);
        binding.tvOrderId.setText(String.format(Locale.getDefault(), "Đơn #%s", shortId));
        binding.collapsingToolbar.setTitle(String.format(Locale.getDefault(), "Đơn #%s", shortId));

        // Order Date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String orderDate = sdf.format(new Date(order.getCreatedAt()));
        binding.tvOrderDate.setText(orderDate);
        binding.tvOrderPlacedTime.setText(orderDate);

        // Last Updated
        if (order.getUpdatedAt() > 0) {
            String updatedDate = sdf.format(new Date(order.getUpdatedAt()));
            binding.tvLastUpdatedTime.setText(updatedDate);
        } else {
            binding.tvLastUpdatedTime.setText(orderDate);
        }

        // Status
        binding.tvStatus.setText(translateStatus(order.getStatus()));
        int statusColor;
        int statusBgColor;
        switch (order.getStatus()) {
            case "Processing":
                statusColor = R.color.status_warning;
                statusBgColor = R.color.status_warning;
                break;
            case "Delivering":
                statusColor = R.color.accent_teal;
                statusBgColor = R.color.accent_teal;
                break;
            case "Completed":
                statusColor = R.color.status_success;
                statusBgColor = R.color.status_success;
                break;
            case "Cancelled":
                statusColor = R.color.status_error;
                statusBgColor = R.color.status_error;
                break;
            default:
                statusColor = R.color.text_secondary;
                statusBgColor = R.color.text_secondary;
        }
        binding.statusCard.setCardBackgroundColor(getResources().getColor(statusBgColor, null));

        // Delivery Address
        binding.tvDeliveryAddress.setText(order.getDeliveryAddress() != null ? 
                order.getDeliveryAddress() : "Chưa có thông tin");

        // Payment Method
        binding.tvPaymentMethod.setText(order.getPaymentMethod() != null ? 
                getPaymentMethodDisplay(order.getPaymentMethod()) : "Thanh toán khi nhận hàng");

        // Price Breakdown
        double subtotal = 0;
        for (necom.eduvn.neihvn.models.OrderItem item : order.getItems()) {
            subtotal += item.getSubtotal();
        }
        double deliveryFee = 2.00; // Default delivery fee
        double total = order.getTotalAmount();

        binding.tvSubtotal.setText(CurrencyFormatter.format(subtotal));
        binding.tvDeliveryFee.setText(CurrencyFormatter.format(deliveryFee));
        binding.tvTotalAmount.setText(CurrencyFormatter.format(total));

        // Order Items
        adapter = new OrderItemDetailAdapter(order.getItems());
        binding.recyclerViewOrderItems.setAdapter(adapter);

        // Show Cancel Order button if status is Processing and user is the buyer
        setupCancelOrderButton();
        
        // Show Review section if status is Completed and user is the buyer
        setupReviewSection();
    }

    private void setupCancelOrderButton() {
        String currentUserId = FirebaseUtil.getCurrentUserId();
        
        // Check if user is the buyer and order is in Processing status
        if (currentUserId != null && 
            currentUserId.equals(order.getBuyerId()) && 
            "Processing".equals(order.getStatus())) {
            
            binding.btnCancelOrder.setVisibility(View.VISIBLE);
            binding.btnCancelOrder.setOnClickListener(v -> showCancelOrderConfirmation());
        } else {
            binding.btnCancelOrder.setVisibility(View.GONE);
        }
    }

    private void showCancelOrderConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn này? Hành động này không thể hoàn tác.")
                .setPositiveButton("Hủy đơn", (dialog, which) -> cancelOrder())
                .setNegativeButton("Giữ lại", null)
                .show();
    }

    private void cancelOrder() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnCancelOrder.setEnabled(false);

        FirebaseUtil.getFirestore().collection("orders")
                .document(order.getOrderId())
                .update("status", "Cancelled", "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    // Listener will automatically update the UI
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnCancelOrder.setEnabled(true);
                    Toast.makeText(this, "Không thể hủy đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupReviewSection() {
        String currentUserId = FirebaseUtil.getCurrentUserId();
        
        // Check if user is the buyer and order is Completed
        if (currentUserId != null && 
            currentUserId.equals(order.getBuyerId()) && 
            "Completed".equals(order.getStatus())) {
            
            binding.cardRestaurantReview.setVisibility(View.VISIBLE);
            binding.cardFoodReviews.setVisibility(View.VISIBLE);
            
            checkExistingRestaurantReview();
            loadFoodReviews();
        } else {
            binding.cardRestaurantReview.setVisibility(View.GONE);
            binding.cardFoodReviews.setVisibility(View.GONE);
        }
    }

    private void checkExistingRestaurantReview() {
        String currentUserId = FirebaseUtil.getCurrentUserId();
        
        // Check if user already reviewed this restaurant for this order (foodId is null)
        FirebaseUtil.getFirestore().collection("reviews")
                .whereEqualTo("orderId", order.getOrderId())
                .whereEqualTo("buyerId", currentUserId)
                .whereEqualTo("restaurantId", order.getRestaurantId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    
                    Review restaurantReview = null;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Review review = doc.toObject(Review.class);
                        if (review != null && (review.getFoodId() == null || review.getFoodId().isEmpty())) {
                            restaurantReview = review;
                            break;
                        }
                    }
                    
                    if (restaurantReview != null) {
                        displayExistingRestaurantReview(restaurantReview);
                    } else {
                        // User hasn't reviewed restaurant yet
                        binding.layoutExistingRestaurantReview.setVisibility(View.GONE);
                        binding.btnWriteRestaurantReview.setVisibility(View.VISIBLE);
                        binding.btnWriteRestaurantReview.setOnClickListener(v -> openReviewActivityForRestaurant());
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        binding.layoutExistingRestaurantReview.setVisibility(View.GONE);
                        binding.btnWriteRestaurantReview.setVisibility(View.VISIBLE);
                        binding.btnWriteRestaurantReview.setOnClickListener(v -> openReviewActivityForRestaurant());
                    }
                });
    }

    private void displayExistingRestaurantReview(Review review) {
        binding.layoutExistingRestaurantReview.setVisibility(View.VISIBLE);
        binding.btnWriteRestaurantReview.setVisibility(View.GONE);
        
        binding.ratingBarRestaurantReview.setRating(review.getRating());
        binding.tvRestaurantReviewComment.setText(review.getComment() != null ? review.getComment() : "Chưa có nội dung");
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String reviewDate = sdf.format(new Date(review.getCreatedAt()));
        binding.tvRestaurantReviewDate.setText(reviewDate);
    }

    private void loadFoodReviews() {
        String currentUserId = FirebaseUtil.getCurrentUserId();
        
        if (order.getItems() == null || order.getItems().isEmpty()) {
            binding.cardFoodReviews.setVisibility(View.GONE);
            return;
        }
        
        // Initialize adapter with order items
        List<OrderItem> foodItems = new ArrayList<>(order.getItems());
        foodReviewAdapter = new FoodReviewAdapter(foodItems, orderItem -> {
            openReviewActivityForFood(orderItem.getFoodId());
        });
        binding.recyclerViewFoodReviews.setAdapter(foodReviewAdapter);
        
        // Load all reviews for food items in this order
        FirebaseUtil.getFirestore().collection("reviews")
                .whereEqualTo("orderId", order.getOrderId())
                .whereEqualTo("buyerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    
                    Map<String, Review> reviewsMap = new HashMap<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Review review = doc.toObject(Review.class);
                        if (review != null && review.getFoodId() != null && !review.getFoodId().isEmpty()) {
                            reviewsMap.put(review.getFoodId(), review);
                        }
                    }
                    
                    if (foodReviewAdapter != null) {
                        foodReviewAdapter.setReviews(reviewsMap);
                    }
                });
    }

    private void openReviewActivityForRestaurant() {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("orderId", order.getOrderId());
        intent.putExtra("restaurantId", order.getRestaurantId());
        intent.putExtra("foodId", (String) null); // null = reviewing restaurant
        startActivity(intent);
    }

    private void openReviewActivityForFood(String foodId) {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("orderId", order.getOrderId());
        intent.putExtra("restaurantId", order.getRestaurantId());
        intent.putExtra("foodId", foodId); // specific food item
        startActivity(intent);
    }

    private void setupSellerControls() {
        // Only show update status button if seller owns this restaurant's order
        if (isSellerMode && restaurantId != null && restaurantId.equals(order.getRestaurantId())) {
            String status = order.getStatus();
            
            // Show update status button for Processing and Delivering orders
            if (!"Completed".equals(status) && !"Cancelled".equals(status)) {
                binding.btnUpdateStatus.setVisibility(View.VISIBLE);
                
                String buttonText;
                String newStatus;
                if ("Processing".equals(status)) {
                    buttonText = "Bắt đầu giao hàng";
                    newStatus = "Delivering";
                } else if ("Delivering".equals(status)) {
                    buttonText = "Đánh dấu hoàn tất";
                    newStatus = "Completed";
                } else {
                    binding.btnUpdateStatus.setVisibility(View.GONE);
                    return;
                }
                
                binding.btnUpdateStatus.setText(buttonText);
                binding.btnUpdateStatus.setOnClickListener(v -> updateOrderStatus(newStatus));
            } else {
                binding.btnUpdateStatus.setVisibility(View.GONE);
            }
        } else {
            binding.btnUpdateStatus.setVisibility(View.GONE);
        }
    }

    private void updateOrderStatus(String newStatus) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnUpdateStatus.setEnabled(false);

        FirebaseUtil.getFirestore().collection("orders")
                .document(order.getOrderId())
                .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnUpdateStatus.setEnabled(true);
                    Toast.makeText(this, "Đã cập nhật trạng thái thành " + translateStatus(newStatus), Toast.LENGTH_SHORT).show();
                    // UI will be updated automatically by the snapshot listener
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnUpdateStatus.setEnabled(true);
                    Toast.makeText(this, "Không thể cập nhật trạng thái đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRestaurantInfo() {
        if (order.getRestaurantId() != null) {
            FirebaseUtil.getFirestore().collection("restaurants")
                    .document(order.getRestaurantId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Restaurant restaurant = documentSnapshot.toObject(Restaurant.class);
                            if (restaurant != null) {
                                binding.tvRestaurantName.setText(restaurant.getName());
                                binding.tvRestaurantAddress.setText(restaurant.getAddress());

                                if (restaurant.getImageUrl() != null && !restaurant.getImageUrl().isEmpty()) {
                                    Glide.with(this)
                                            .load(restaurant.getImageUrl())
                                            .placeholder(R.drawable.placeholder_restaurant)
                                            .centerCrop()
                                            .into(binding.ivRestaurantImage);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Restaurant info is optional, don't show error
                    });
        }
    }

    private String translateStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "Processing":
                return "Đang xử lý";
            case "Delivering":
                return "Đang giao";
            case "Completed":
                return "Đã hoàn thành";
            case "Cancelled":
                return "Đã hủy";
            case "Pending Payment":
                return "Chờ thanh toán";
            default:
                return status;
        }
    }

    private String getPaymentMethodDisplay(String method) {
        if (method == null) return "";
        switch (method) {
            case "Cash":
                return "Tiền mặt";
            case "Bank Transfer":
                return "Chuyển khoản";
            case "VNPay":
                return "VNPay";
            default:
                return method;
        }
    }
}
