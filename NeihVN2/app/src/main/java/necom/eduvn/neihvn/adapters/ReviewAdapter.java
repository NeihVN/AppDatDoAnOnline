package necom.eduvn.neihvn.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ItemReviewBinding;
import necom.eduvn.neihvn.models.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private List<Review> reviews;
    private boolean isSellerMode;
    private OnReviewActionListener listener;

    public interface OnReviewActionListener {
        void onReply(Review review);
    }

    public ReviewAdapter(List<Review> reviews, boolean isSellerMode, OnReviewActionListener listener) {
        this.reviews = reviews;
        this.isSellerMode = isSellerMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewBinding binding = ItemReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemReviewBinding binding;

        ViewHolder(ItemReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Review review) {
            binding.tvBuyerName.setText(review.getBuyerName());
            binding.ratingBar.setRating(review.getRating());
            binding.tvComment.setText(review.getComment());

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("vi", "VN"));
            String date = sdf.format(new Date(review.getCreatedAt()));
            binding.tvReviewDate.setText(date);

            // Load avatar
            if (review.getBuyerAvatar() != null && !review.getBuyerAvatar().isEmpty()) {
                Glide.with(binding.getRoot())
                        .load(review.getBuyerAvatar())
                        .placeholder(R.drawable.ic_user_placeholder)
                        .circleCrop()
                        .into(binding.ivBuyerAvatar);
            }

            // Seller reply
            if (review.getSellerReply() != null && !review.getSellerReply().isEmpty()) {
                binding.layoutReply.setVisibility(View.VISIBLE);
                binding.tvReply.setText(review.getSellerReply());
            } else {
                binding.layoutReply.setVisibility(View.GONE);
            }

            // Reply button for sellers
            if (isSellerMode && (review.getSellerReply() == null || review.getSellerReply().isEmpty())) {
                binding.btnReply.setVisibility(View.VISIBLE);
                binding.btnReply.setOnClickListener(v -> listener.onReply(review));
            } else {
                binding.btnReply.setVisibility(View.GONE);
            }
        }
    }
}