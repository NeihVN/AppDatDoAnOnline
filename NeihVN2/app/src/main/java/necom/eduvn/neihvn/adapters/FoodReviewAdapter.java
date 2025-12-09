package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemReviewFoodBinding;
import necom.eduvn.neihvn.models.OrderItem;
import necom.eduvn.neihvn.models.Review;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodReviewAdapter extends RecyclerView.Adapter<FoodReviewAdapter.ViewHolder> {
    private List<OrderItem> orderItems;
    private Map<String, Review> reviewsMap; // foodId -> Review
    private OnReviewFoodClickListener listener;

    public interface OnReviewFoodClickListener {
        void onReviewFood(OrderItem orderItem);
    }

    public FoodReviewAdapter(List<OrderItem> orderItems, OnReviewFoodClickListener listener) {
        this.orderItems = orderItems;
        this.listener = listener;
        this.reviewsMap = new HashMap<>();
    }

    public void setReviews(Map<String, Review> reviews) {
        this.reviewsMap.clear();
        if (reviews != null) {
            this.reviewsMap.putAll(reviews);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewFoodBinding binding = ItemReviewFoodBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(orderItems.get(position));
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemReviewFoodBinding binding;

        ViewHolder(ItemReviewFoodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderItem item) {
            binding.tvFoodName.setText(item.getFoodName());
            binding.tvQuantity.setText("Số lượng: " + item.getQuantity());

            if (item.getFoodImageUrl() != null && !item.getFoodImageUrl().isEmpty()) {
                Glide.with(binding.getRoot())
                        .load(item.getFoodImageUrl())
                        .placeholder(R.drawable.placeholder_food)
                        .centerCrop()
                        .into(binding.ivFoodImage);
            }

            // Check if this food item has been reviewed
            Review review = reviewsMap.get(item.getFoodId());
            if (review != null) {
                // Show reviewed status
                binding.layoutReviewed.setVisibility(View.VISIBLE);
                binding.btnReviewFood.setVisibility(View.GONE);
                binding.ratingBarReview.setRating(review.getRating());
            } else {
                // Show review button
                binding.layoutReviewed.setVisibility(View.GONE);
                binding.btnReviewFood.setVisibility(View.VISIBLE);
                binding.btnReviewFood.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onReviewFood(item);
                    }
                });
            }
        }
    }
}
