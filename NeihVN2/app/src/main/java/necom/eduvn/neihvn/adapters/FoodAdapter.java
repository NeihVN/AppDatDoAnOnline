package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemFoodBinding;
import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.utils.CategoryUtils;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.FavoriteManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {
    private List<FoodItem> foods;
    private boolean isSellerMode;
    private OnFoodActionListener listener;
    private Set<String> favoriteFoodIds; // Cache for favorite status
    private FavoriteManager favoriteManager;

    public interface OnFoodActionListener {
        void onEdit(FoodItem food);
        void onDelete(FoodItem food);
        void onToggleAvailability(FoodItem food);
        void onClick(FoodItem food);
        void onAddToCart(FoodItem food);
        void onToggleFavorite(FoodItem food);
    }

    public FoodAdapter(List<FoodItem> foods, boolean isSellerMode, OnFoodActionListener listener) {
        this.foods = foods;
        this.isSellerMode = isSellerMode;
        this.listener = listener;
        this.favoriteFoodIds = new HashSet<>();
        this.favoriteManager = FavoriteManager.getInstance();
        
        // Load favorite IDs for buyer mode
        if (!isSellerMode) {
            loadFavoriteIds();
        }
    }
    
    private void loadFavoriteIds() {
        favoriteManager.loadFavoriteIds(favoriteIds -> {
            favoriteFoodIds.clear();
            favoriteFoodIds.addAll(favoriteIds);
            notifyDataSetChanged();
        });
    }
    
    public void updateFavoriteStatus(String foodId, boolean isFavorite) {
        if (isFavorite) {
            favoriteFoodIds.add(foodId);
        } else {
            favoriteFoodIds.remove(foodId);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFoodBinding binding = ItemFoodBinding.inflate(
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
        private ItemFoodBinding binding;

        ViewHolder(ItemFoodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FoodItem food) {
            binding.tvFoodName.setText(food.getName());
            binding.tvPrice.setText(CurrencyFormatter.format(food.getPrice()));
            binding.tvCategory.setText(CategoryUtils.getDisplayName(food.getCategory()));

            // Load image
            Glide.with(binding.getRoot())
                    .load(food.getImageUrl())
                    .placeholder(R.drawable.placeholder_food)
                    .error(R.drawable.placeholder_food)
                    .centerCrop()
                    .into(binding.ivFoodImage);

            // Seller mode controls
            if (isSellerMode) {
                binding.layoutActions.setVisibility(View.VISIBLE);
                binding.tvAvailability.setVisibility(View.VISIBLE);
                binding.tvApprovalStatus.setVisibility(View.VISIBLE);

                binding.tvAvailability.setText(food.isAvailable() ? "Đang bán" : "Tạm ngừng");
                binding.tvAvailability.setTextColor(binding.getRoot().getContext().getColor(
                        food.isAvailable() ? R.color.status_success : R.color.status_error));

                // Approval status
                binding.tvApprovalStatus.setText(food.isApproved() ? "Đã duyệt" : "Chờ duyệt");
                binding.tvApprovalStatus.setTextColor(binding.getRoot().getContext().getColor(
                        food.isApproved() ? R.color.status_success : R.color.status_warning));

                binding.btnEdit.setOnClickListener(v -> listener.onEdit(food));
                binding.btnDelete.setOnClickListener(v -> listener.onDelete(food));
                binding.btnToggleAvailability.setOnClickListener(v -> listener.onToggleAvailability(food));
            } else {
                binding.layoutActions.setVisibility(View.GONE);
                binding.tvAvailability.setVisibility(View.GONE);
                binding.tvApprovalStatus.setVisibility(View.GONE);
            }

            // Buyer mode buttons
            if (!isSellerMode) {
                binding.btnAddToCart.setVisibility(View.VISIBLE);
                binding.btnFavorite.setVisibility(View.VISIBLE);
                
                // Update favorite button icon based on status
                boolean isFavorite = favoriteFoodIds.contains(food.getFoodId());
                updateFavoriteButton(binding, isFavorite);
                
                // Check favorite status if not in cache
                if (!favoriteFoodIds.contains(food.getFoodId()) && !favoriteFoodIds.contains("checking_" + food.getFoodId())) {
                    favoriteFoodIds.add("checking_" + food.getFoodId()); // Mark as checking
                    favoriteManager.isFavorite(food.getFoodId(), isFav -> {
                        favoriteFoodIds.remove("checking_" + food.getFoodId());
                        if (isFav) {
                            favoriteFoodIds.add(food.getFoodId());
                        } else {
                            favoriteFoodIds.remove(food.getFoodId());
                        }
                        // Update only this item
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && position < foods.size() && foods.get(position).getFoodId().equals(food.getFoodId())) {
                            updateFavoriteButton(binding, isFav);
                        }
                    });
                }
                
                // Set click listeners - buttons will consume events and prevent card click
                binding.btnAddToCart.setOnClickListener(v -> {
                    if (listener != null) {
                        v.setEnabled(false);
                        listener.onAddToCart(food);
                        // Re-enable after a short delay to prevent double clicks
                        v.postDelayed(() -> v.setEnabled(true), 500);
                    }
                });
                
                binding.btnFavorite.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onToggleFavorite(food);
                    }
                });
            } else {
                binding.btnAddToCart.setVisibility(View.GONE);
                binding.btnFavorite.setVisibility(View.GONE);
            }

            // Click to view details - this will be triggered only if buttons don't consume the event
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(food);
                }
            });
        }
        
        private void updateFavoriteButton(ItemFoodBinding binding, boolean isFavorite) {
            // Find ImageView by ID
            View view = binding.btnFavorite.findViewById(R.id.ivFavoriteIcon);
            if (view instanceof android.widget.ImageView) {
                android.widget.ImageView favoriteIcon = (android.widget.ImageView) view;
                if (isFavorite) {
                    favoriteIcon.setImageResource(R.drawable.ic_favorite);
                    favoriteIcon.setColorFilter(binding.getRoot().getContext().getColor(R.color.primary_orange));
                } else {
                    favoriteIcon.setImageResource(R.drawable.ic_favorite_border);
                    favoriteIcon.setColorFilter(binding.getRoot().getContext().getColor(R.color.text_secondary));
                }
            }
        }
    }
}