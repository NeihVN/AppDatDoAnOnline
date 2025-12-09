package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemAdminFoodBinding;
import necom.eduvn.neihvn.models.FoodItem;

import java.util.List;
import java.util.Map;

import necom.eduvn.neihvn.utils.CategoryUtils;
import necom.eduvn.neihvn.utils.CurrencyFormatter;

public class AdminFoodAdapter extends RecyclerView.Adapter<AdminFoodAdapter.ViewHolder> {
    private List<FoodItem> foods;
    private OnFoodActionListener listener;
    private Map<String, String> restaurantNames;

    public interface OnFoodActionListener {
        void onApprove(FoodItem food);
        void onReject(FoodItem food);
        void onView(FoodItem food);
    }

    public AdminFoodAdapter(List<FoodItem> foods, OnFoodActionListener listener, Map<String, String> restaurantNames) {
        this.foods = foods;
        this.listener = listener;
        this.restaurantNames = restaurantNames;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminFoodBinding binding = ItemAdminFoodBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(foods.get(position));
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemAdminFoodBinding binding;

        ViewHolder(ItemAdminFoodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FoodItem food) {
            binding.tvFoodName.setText(food.getName());
            binding.tvDescription.setText(food.getDescription());
            binding.tvPrice.setText(CurrencyFormatter.format(food.getPrice()));
            binding.tvCategory.setText(CategoryUtils.getDisplayName(food.getCategory()));

            // Restaurant name
            String restaurantName = restaurantNames.get(food.getRestaurantId());
            binding.tvRestaurantName.setText("Thuộc: " + (restaurantName != null ? restaurantName : "Chưa rõ"));

            // Status badge - Show buttons based on status
            if (food.isApproved() && food.isAvailable()) {
                // Approved and available
                binding.tvStatus.setText("Đã duyệt");
                binding.tvStatus.setBackgroundResource(R.color.status_success);
                binding.btnApprove.setVisibility(View.GONE);
                binding.btnReject.setVisibility(View.VISIBLE); // Can reject even if approved
            } else if (!food.isApproved() && !food.isAvailable()) {
                // Rejected
                binding.tvStatus.setText("Bị từ chối");
                binding.tvStatus.setBackgroundResource(R.color.status_error);
                binding.btnApprove.setVisibility(View.VISIBLE); // Can approve again
                binding.btnReject.setVisibility(View.GONE);
            } else {
                // Pending (not approved but available)
                binding.tvStatus.setText("Đang chờ duyệt");
                binding.tvStatus.setBackgroundResource(R.color.status_warning);
                binding.btnApprove.setVisibility(View.VISIBLE);
                binding.btnReject.setVisibility(View.VISIBLE);
            }

            // Load image
            if (food.getImageUrl() != null && !food.getImageUrl().isEmpty()) {
                Glide.with(binding.getRoot())
                        .load(food.getImageUrl())
                        .placeholder(R.drawable.placeholder_restaurant)
                        .centerCrop()
                        .into(binding.ivFoodImage);
            }

            // Actions
            binding.btnApprove.setOnClickListener(v -> listener.onApprove(food));
            binding.btnReject.setOnClickListener(v -> listener.onReject(food));
            binding.getRoot().setOnClickListener(v -> listener.onView(food));
        }
    }
}
