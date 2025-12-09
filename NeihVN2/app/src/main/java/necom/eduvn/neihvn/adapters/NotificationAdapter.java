package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import necom.eduvn.neihvn.databinding.ItemNotificationBinding;
import necom.eduvn.neihvn.models.Notification;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notifications;
    private OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onNotificationClick(Notification notification);
        void onMarkAsRead(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationActionListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemNotificationBinding binding;

        ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Notification notification) {
            binding.tvNotificationTitle.setText(notification.getTitle());
            binding.tvNotificationMessage.setText(notification.getMessage());
            
            // Format time
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", new Locale("vi", "VN"));
            String timeStr = sdf.format(new Date(notification.getCreatedAt()));
            binding.tvNotificationTime.setText(timeStr);
            
            // Show/hide unread indicator
            binding.indicatorUnread.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            
            // Change background if unread
            if (!notification.isRead()) {
                binding.getRoot().setAlpha(1.0f);
            } else {
                binding.getRoot().setAlpha(0.7f);
            }
            
            // Click listeners
            binding.getRoot().setOnClickListener(v -> {
                listener.onNotificationClick(notification);
                if (!notification.isRead()) {
                    listener.onMarkAsRead(notification);
                }
            });
        }
    }
}
