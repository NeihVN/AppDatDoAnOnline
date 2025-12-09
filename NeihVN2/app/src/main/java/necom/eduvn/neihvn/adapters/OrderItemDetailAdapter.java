package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemOrderDetailFoodBinding;
import necom.eduvn.neihvn.models.OrderItem;
import necom.eduvn.neihvn.utils.CurrencyFormatter;

import java.util.List;
import java.util.Locale;

public class OrderItemDetailAdapter extends RecyclerView.Adapter<OrderItemDetailAdapter.ViewHolder> {
    private List<OrderItem> orderItems;

    public OrderItemDetailAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderDetailFoodBinding binding = ItemOrderDetailFoodBinding.inflate(
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
        private ItemOrderDetailFoodBinding binding;

        ViewHolder(ItemOrderDetailFoodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderItem item) {
            binding.tvFoodName.setText(item.getFoodName());
            binding.tvQuantity.setText("Số lượng: " + item.getQuantity());
            binding.tvItemPrice.setText(String.format(Locale.getDefault(), "%s / món", CurrencyFormatter.format(item.getPrice())));
            binding.tvItemTotal.setText(CurrencyFormatter.format(item.getSubtotal()));

            if (item.getFoodImageUrl() != null && !item.getFoodImageUrl().isEmpty()) {
                Glide.with(binding.getRoot())
                        .load(item.getFoodImageUrl())
                        .placeholder(R.drawable.placeholder_food)
                        .centerCrop()
                        .into(binding.ivFoodImage);
            }
        }
    }
}
