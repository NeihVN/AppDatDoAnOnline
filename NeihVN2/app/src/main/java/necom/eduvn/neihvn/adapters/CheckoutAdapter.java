package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemCheckoutBinding;
import necom.eduvn.neihvn.models.OrderItem;
import necom.eduvn.neihvn.utils.CurrencyFormatter;

import java.util.List;

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.ViewHolder> {
    private List<OrderItem> items;

    public CheckoutAdapter(List<OrderItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCheckoutBinding binding = ItemCheckoutBinding.inflate(
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemCheckoutBinding binding;

        ViewHolder(ItemCheckoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderItem item) {
            binding.tvFoodName.setText(item.getFoodName());
            binding.tvQuantity.setText("x" + item.getQuantity());
            binding.tvSubtotal.setText(CurrencyFormatter.format(item.getSubtotal()));

            if (item.getFoodImageUrl() != null) {
                Glide.with(binding.getRoot())
                        .load(item.getFoodImageUrl())
                        .placeholder(R.drawable.placeholder_food)
                        .centerCrop()
                        .into(binding.ivFoodImage);
            }
        }
    }
}