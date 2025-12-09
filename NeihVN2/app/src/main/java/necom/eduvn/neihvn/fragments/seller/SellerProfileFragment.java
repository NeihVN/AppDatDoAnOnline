package necom.eduvn.neihvn.fragments.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.activities.EditProfileActivity;
import necom.eduvn.neihvn.activities.LoginActivity;
import necom.eduvn.neihvn.activities.RestaurantFormActivity;
import necom.eduvn.neihvn.databinding.FragmentProfileBinding;
import necom.eduvn.neihvn.models.User;
import necom.eduvn.neihvn.utils.FirebaseUtil;

public class SellerProfileFragment extends Fragment {
    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserProfile();

        binding.btnEditProfile.setOnClickListener(v -> editProfile());
        binding.btnManageRestaurant.setVisibility(View.VISIBLE);
        binding.btnManageRestaurant.setOnClickListener(v -> manageRestaurant());
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String userId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            displayUserInfo(user);
                        }
                    }
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    private void displayUserInfo(User user) {
        binding.tvUserName.setText(user.getName());
        binding.tvUserEmail.setText(user.getEmail());
        binding.tvUserPhone.setText(user.getPhone() != null ? user.getPhone() : "Chưa cập nhật");
        binding.tvUserRole.setText("NGƯỜI BÁN");
        binding.tvUserRole.setBackgroundResource(R.color.accent_teal);

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_user_placeholder)
                    .circleCrop()
                    .into(binding.ivUserAvatar);
        }
    }

    private void editProfile() {
        startActivity(new Intent(getContext(), EditProfileActivity.class));
    }
    private void manageRestaurant() {
        startActivity(new Intent(getContext(), RestaurantFormActivity.class));
    }

    private void logout() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    FirebaseUtil.getAuth().signOut();
                    startActivity(new Intent(getContext(), LoginActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}