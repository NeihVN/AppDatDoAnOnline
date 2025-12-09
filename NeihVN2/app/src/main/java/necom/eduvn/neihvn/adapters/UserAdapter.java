package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemUserBinding;
import necom.eduvn.neihvn.models.User;

import java.util.List;
import java.util.Locale;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private static final Locale VI_LOCALE = new Locale("vi", "VN");

    private List<User> users;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onEdit(User user);
        void onDelete(User user);
        void onToggleActive(User user);
    }

    public UserAdapter(List<User> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemUserBinding binding;

        ViewHolder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            binding.tvUserName.setText(user.getName());
            binding.tvUserEmail.setText(user.getEmail());

            String roleKey = user.getRole() != null ? user.getRole() : "";
            String roleLabel;
            switch (roleKey) {
                case "admin":
                    roleLabel = "Quản trị";
                    break;
                case "seller":
                    roleLabel = "Người bán";
                    break;
                default:
                    roleLabel = "Người mua";
                    break;
            }
            binding.tvUserRole.setText(roleLabel.toUpperCase(VI_LOCALE));

            // Role badge color
            int roleColor = "admin".equals(roleKey) ? R.color.status_error :
                    "seller".equals(roleKey) ? R.color.accent_teal :
                            R.color.primary_orange;
            binding.tvUserRole.setBackgroundResource(roleColor);

            // Status
            boolean isActive = user.isActive();
            binding.tvStatus.setText(isActive ? "Đang hoạt động" : "Đã vô hiệu hóa");
            binding.tvStatus.setTextColor(binding.getRoot().getContext().getColor(
                    isActive ? R.color.status_success : R.color.status_error));

            // Avatar
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(binding.getRoot())
                        .load(user.getAvatarUrl())
                        .placeholder(R.drawable.ic_user_placeholder)
                        .circleCrop()
                        .into(binding.ivAvatar);
            }

            // Actions
            binding.btnEdit.setOnClickListener(v -> listener.onEdit(user));
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(user));
            binding.btnToggleActive.setOnClickListener(v -> listener.onToggleActive(user));

            binding.btnToggleActive.setText(isActive ? "Vô hiệu hóa" : "Kích hoạt");
        }
    }
}