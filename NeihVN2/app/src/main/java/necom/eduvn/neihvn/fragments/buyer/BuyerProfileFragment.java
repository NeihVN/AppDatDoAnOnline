package necom.eduvn.neihvn.fragments.buyer;

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
import necom.eduvn.neihvn.databinding.FragmentProfileBinding;
import necom.eduvn.neihvn.models.User;
import necom.eduvn.neihvn.utils.FirebaseUtil;

public class BuyerProfileFragment extends Fragment {
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
        binding.tvUserAddress.setText(user.getAddress() != null ? user.getAddress() : "Chưa cập nhật");
        binding.tvUserRole.setText("KHÁCH HÀNG");
        binding.tvUserRole.setBackgroundResource(R.color.primary_orange);

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

    private void logout() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
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