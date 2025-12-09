package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemOrderBinding;
import necom.eduvn.neihvn.models.Order;
import necom.eduvn.neihvn.models.OrderItem;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private List<Order> orders;
    private boolean isSellerMode;
    private OnOrderActionListener listener;

    public interface OnOrderActionListener {
        void onUpdateStatus(Order order, String newStatus);
        void onViewDetails(Order order);
    }

    public OrderAdapter(List<Order> orders, boolean isSellerMode, OnOrderActionListener listener) {
        this.orders = orders;
        this.isSellerMode = isSellerMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemOrderBinding binding;

        ViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Order order) {
            binding.tvOrderId.setText(String.format(Locale.getDefault(), "Đơn #%s", order.getOrderId().substring(0, 8)));

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
            String date = sdf.format(new Date(order.getCreatedAt()));
            binding.tvOrderDate.setText(date);

            // Items count
            int itemCount = 0;
            for (OrderItem item : order.getItems()) {
                itemCount += item.getQuantity();
            }
            binding.tvItemCount.setText(String.format(Locale.getDefault(), "%d món", itemCount));

            // Total amount
            binding.tvTotalAmount.setText(CurrencyFormatter.format(order.getTotalAmount()));

            // Status
            binding.tvStatus.setText(getStatusDisplay(order.getStatus()));
            int statusColor;
            switch (order.getStatus()) {
                case "Processing":
                    statusColor = R.color.status_warning;
                    break;
                case "Delivering":
                    statusColor = R.color.accent_teal;
                    break;
                case "Completed":
                    statusColor = R.color.status_success;
                    break;
                case "Cancelled":
                    statusColor = R.color.status_error;
                    break;
                default:
                    statusColor = R.color.text_secondary;
            }
            binding.tvStatus.setBackgroundResource(statusColor);

            // Delivery address
            binding.tvDeliveryAddress.setText(order.getDeliveryAddress());

            // Load restaurant name
            loadRestaurantName(order.getRestaurantId());

            // Actions
            if (isSellerMode && !order.getStatus().equals("Completed") && !order.getStatus().equals("Cancelled")) {
                binding.btnUpdateStatus.setVisibility(View.VISIBLE);
                binding.btnUpdateStatus.setOnClickListener(v -> {
                    String newStatus = order.getStatus().equals("Processing") ? "Delivering" : "Completed";
                    listener.onUpdateStatus(order, newStatus);
                });

                String buttonText = order.getStatus().equals("Processing") ? "Bắt đầu giao hàng" : "Đánh dấu hoàn tất";
                binding.btnUpdateStatus.setText(buttonText);
            } else {
                binding.btnUpdateStatus.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> listener.onViewDetails(order));
        }

        private void loadRestaurantName(String restaurantId) {
            if (restaurantId != null && binding.tvRestaurantName != null) {
                FirebaseUtil.getFirestore().collection("restaurants")
                        .document(restaurantId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists() && binding != null) {
                                Restaurant restaurant = documentSnapshot.toObject(Restaurant.class);
                                if (restaurant != null && binding.tvRestaurantName != null) {
                                    binding.tvRestaurantName.setText(restaurant.getName());
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Restaurant name is optional, don't show error
                            if (binding != null && binding.tvRestaurantName != null) {
                                binding.tvRestaurantName.setText("");
                            }
                        });
            }
        }
    }

    private String getStatusDisplay(String status) {
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
}