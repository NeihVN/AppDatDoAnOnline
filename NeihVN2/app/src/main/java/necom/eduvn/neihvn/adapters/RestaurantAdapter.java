package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemRestaurantBinding;
import necom.eduvn.neihvn.models.Restaurant;

import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {
    private List<Restaurant> restaurants;
    private OnRestaurantActionListener listener;

    public interface OnRestaurantActionListener {
        void onApprove(Restaurant restaurant);
        void onReject(Restaurant restaurant);
        void onView(Restaurant restaurant);
    }

    public RestaurantAdapter(List<Restaurant> restaurants, OnRestaurantActionListener listener) {
        this.restaurants = restaurants;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRestaurantBinding binding = ItemRestaurantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(restaurants.get(position));
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemRestaurantBinding binding;

        ViewHolder(ItemRestaurantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Restaurant restaurant) {
            binding.tvRestaurantName.setText(restaurant.getName());
            binding.tvAddress.setText(restaurant.getAddress());
            binding.tvRating.setText(String.format("⭐ %.1f (%d)",
                    restaurant.getRating(), restaurant.getTotalReviews()));

            // Status badge - Show buttons based on status
            if (restaurant.isApproved() && restaurant.isActive()) {
                // Approved and active
                binding.tvStatus.setText("Đã duyệt");
                binding.tvStatus.setBackgroundResource(R.color.status_success);
                binding.btnApprove.setVisibility(View.GONE);
                binding.btnReject.setVisibility(View.VISIBLE); // Can reject even if approved
            } else if (!restaurant.isApproved() && !restaurant.isActive()) {
                // Rejected
                binding.tvStatus.setText("Bị từ chối");
                binding.tvStatus.setBackgroundResource(R.color.status_error);
                binding.btnApprove.setVisibility(View.VISIBLE); // Can approve again
                binding.btnReject.setVisibility(View.GONE);
            } else {
                // Pending (not approved but active)
                binding.tvStatus.setText("Đang chờ duyệt");
                binding.tvStatus.setBackgroundResource(R.color.status_warning);
                binding.btnApprove.setVisibility(View.VISIBLE);
                binding.btnReject.setVisibility(View.VISIBLE);
            }

            // Load image
            if (restaurant.getImageUrl() != null) {
                Glide.with(binding.getRoot())
                        .load(restaurant.getImageUrl())
                        .placeholder(R.drawable.placeholder_restaurant)
                        .centerCrop()
                        .into(binding.ivRestaurantImage);
            }

            // Actions
            binding.btnApprove.setOnClickListener(v -> listener.onApprove(restaurant));
            binding.btnReject.setOnClickListener(v -> listener.onReject(restaurant));
            binding.getRoot().setOnClickListener(v -> listener.onView(restaurant));
        }
    }
}