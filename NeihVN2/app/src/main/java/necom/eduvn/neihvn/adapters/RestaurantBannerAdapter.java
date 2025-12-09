package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemRestaurantBannerBinding;
import necom.eduvn.neihvn.models.Restaurant;

import java.util.List;
import java.util.Locale;

public class RestaurantBannerAdapter extends RecyclerView.Adapter<RestaurantBannerAdapter.ViewHolder> {
    private List<Restaurant> restaurants;
    private OnRestaurantClickListener listener;

    public interface OnRestaurantClickListener {
        void onClick(Restaurant restaurant);
    }

    public RestaurantBannerAdapter(List<Restaurant> restaurants, OnRestaurantClickListener listener) {
        this.restaurants = restaurants;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRestaurantBannerBinding binding = ItemRestaurantBannerBinding.inflate(
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
        private ItemRestaurantBannerBinding binding;

        ViewHolder(ItemRestaurantBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Restaurant restaurant) {
            binding.tvRestaurantName.setText(restaurant.getName());
            
            // Display rating with review count
            double rating = restaurant.getRating();
            int reviewCount = restaurant.getTotalReviews();
            if (reviewCount > 0) {
                binding.tvRating.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d)", rating, reviewCount));
            } else {
                binding.tvRating.setText("⭐ Chưa có đánh giá");
            }

            if (restaurant.getImageUrl() != null) {
                Glide.with(binding.getRoot())
                        .load(restaurant.getImageUrl())
                        .placeholder(R.drawable.placeholder_restaurant)
                        .centerCrop()
                        .into(binding.ivRestaurantImage);
            }

            binding.getRoot().setOnClickListener(v -> listener.onClick(restaurant));
        }
    }
}