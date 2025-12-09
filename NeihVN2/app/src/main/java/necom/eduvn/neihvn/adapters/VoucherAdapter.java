package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.models.Voucher;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.VoucherManager;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {
    
    private List<Voucher> voucherList;
    private OnVoucherActionListener listener;
    
    public interface OnVoucherActionListener {
        void onEditVoucher(Voucher voucher);
        void onDeleteVoucher(Voucher voucher);
        void onToggleActive(Voucher voucher);
    }
    
    public VoucherAdapter(List<Voucher> voucherList, OnVoucherActionListener listener) {
        this.voucherList = voucherList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = voucherList.get(position);
        holder.bind(voucher);
    }
    
    @Override
    public int getItemCount() {
        return voucherList.size();
    }
    
    public void updateList(List<Voucher> newList) {
        this.voucherList = newList;
        notifyDataSetChanged();
    }
    
    class VoucherViewHolder extends RecyclerView.ViewHolder {
        private TextView tvVoucherCode, tvVoucherDiscount, tvVoucherDescription;
        private TextView tvMinOrder, tvUsage, tvExpiry, tvStatus;
        private ImageView btnMenu;
        
        VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVoucherCode = itemView.findViewById(R.id.tvVoucherCode);
            tvVoucherDiscount = itemView.findViewById(R.id.tvVoucherDiscount);
            tvVoucherDescription = itemView.findViewById(R.id.tvVoucherDescription);
            tvMinOrder = itemView.findViewById(R.id.tvMinOrder);
            tvUsage = itemView.findViewById(R.id.tvUsage);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
        
        void bind(Voucher voucher) {
            // Set code and discount
            tvVoucherCode.setText(voucher.getCode());
            tvVoucherDiscount.setText(VoucherManager.getDiscountDisplayText(voucher));
            
            // Set description
            tvVoucherDescription.setText(voucher.getDescription());
            
            // Set minimum order
            tvMinOrder.setText(CurrencyFormatter.format(voucher.getMinOrderAmount()));
            
            // Set usage
            String usageText;
            if (voucher.getUsageLimit() == 0) {
                usageText = String.valueOf(voucher.getUsedCount()) + "/∞";
            } else {
                usageText = voucher.getUsedCount() + "/" + voucher.getUsageLimit();
            }
            tvUsage.setText(usageText);
            
            // Set expiry date
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvExpiry.setText(sdf.format(new Date(voucher.getEndDate())));
            
            // Set status
            long now = System.currentTimeMillis();
            if (!voucher.isActive()) {
                tvStatus.setText("ĐÃ VÔ HIỆU HÓA");
                tvStatus.setBackgroundResource(R.drawable.badge_inactive);
            } else if (now > voucher.getEndDate()) {
                tvStatus.setText("ĐÃ HẾT HẠN");
                tvStatus.setBackgroundResource(R.drawable.badge_cancelled);
            } else if (voucher.getUsageLimit() > 0 && voucher.getUsedCount() >= voucher.getUsageLimit()) {
                tvStatus.setText("HẾT LƯỢT");
                tvStatus.setBackgroundResource(R.drawable.badge_cancelled);
            } else if (now < voucher.getStartDate()) {
                tvStatus.setText("CHƯA HIỆU LỰC");
                tvStatus.setBackgroundResource(R.drawable.badge_pending);
            } else {
                tvStatus.setText("ĐANG HOẠT ĐỘNG");
                tvStatus.setBackgroundResource(R.drawable.badge_active);
            }
            
            // Set menu click listener
            btnMenu.setOnClickListener(v -> showPopupMenu(v, voucher));
        }
        
        private void showPopupMenu(View view, Voucher voucher) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.menu_voucher_actions, popup.getMenu());
            
            // Update toggle text based on current status
            if (voucher.isActive()) {
                popup.getMenu().findItem(R.id.action_toggle_active).setTitle("Vô hiệu hóa");
            } else {
                popup.getMenu().findItem(R.id.action_toggle_active).setTitle("Kích hoạt");
            }
            
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_edit) {
                    if (listener != null) listener.onEditVoucher(voucher);
                    return true;
                } else if (id == R.id.action_toggle_active) {
                    if (listener != null) listener.onToggleActive(voucher);
                    return true;
                } else if (id == R.id.action_delete) {
                    if (listener != null) listener.onDeleteVoucher(voucher);
                    return true;
                }
                return false;
            });
            
            popup.show();
        }
    }
}

