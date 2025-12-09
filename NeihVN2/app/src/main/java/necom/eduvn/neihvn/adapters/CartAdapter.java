package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemCartBinding;
import necom.eduvn.neihvn.models.OrderItem;
import necom.eduvn.neihvn.utils.CurrencyFormatter;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
    private List<OrderItem> items;
    private OnCartActionListener listener;

    public interface OnCartActionListener {
        void onQuantityChanged(OrderItem item, int newQuantity);
        void onRemove(OrderItem item);
    }

    public CartAdapter(List<OrderItem> items, OnCartActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateItems(List<OrderItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemCartBinding binding;

        ViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderItem item) {
            binding.tvFoodName.setText(item.getFoodName());
            binding.tvPrice.setText(CurrencyFormatter.format(item.getPrice()));
            binding.tvQuantity.setText(String.valueOf(item.getQuantity()));
            binding.tvSubtotal.setText(CurrencyFormatter.format(item.getSubtotal()));

            // Load image
            if (item.getFoodImageUrl() != null) {
                Glide.with(binding.getRoot())
                        .load(item.getFoodImageUrl())
                        .placeholder(R.drawable.placeholder_food)
                        .centerCrop()
                        .into(binding.ivFoodImage);
            }

            // Quantity controls
            binding.btnMinus.setOnClickListener(v -> {
                int newQty = item.getQuantity() - 1;
                if (newQty >= 0) {
                    listener.onQuantityChanged(item, newQty);
                }
            });

            binding.btnPlus.setOnClickListener(v -> {
                int newQty = item.getQuantity() + 1;
                listener.onQuantityChanged(item, newQty);
            });

            // Remove
            binding.btnRemove.setOnClickListener(v -> listener.onRemove(item));
        }
    }
}