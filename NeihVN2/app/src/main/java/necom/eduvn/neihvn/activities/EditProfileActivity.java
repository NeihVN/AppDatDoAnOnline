package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ActivityEditProfileBinding;
import necom.eduvn.neihvn.models.User;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.ValidationUtil;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        loadUserProfile();

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Profile");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserProfile() {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        String userId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            displayUserInfo();
                        }
                    }
                    binding.progressBar.setVisibility(android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                });
    }

    private void displayUserInfo() {
        binding.etName.setText(currentUser.getName());
        binding.etPhone.setText(currentUser.getPhone());
        binding.etAddress.setText(currentUser.getAddress());
        binding.etAvatarUrl.setText(currentUser.getAvatarUrl());

        if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentUser.getAvatarUrl())
                    .placeholder(R.drawable.ic_user_placeholder)
                    .circleCrop()
                    .into(binding.ivPreviewAvatar);
        }

        // Preview avatar on text change
        binding.etAvatarUrl.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String url = s.toString().trim();
                if (ValidationUtil.isValidUrl(url)) {
                    Glide.with(EditProfileActivity.this)
                            .load(url)
                            .placeholder(R.drawable.ic_user_placeholder)
                            .circleCrop()
                            .into(binding.ivPreviewAvatar);
                }
            }
        });
    }

    private void saveProfile() {
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String avatarUrl = binding.etAvatarUrl.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etName.setError("Vui lòng nhập họ tên");
            return;
        }

        if (!phone.isEmpty() && !ValidationUtil.isValidPhone(phone)) {
            binding.etPhone.setError("Số điện thoại không hợp lệ");
            return;
        }

        if (!avatarUrl.isEmpty() && !ValidationUtil.isValidUrl(avatarUrl)) {
            binding.etAvatarUrl.setError("URL không hợp lệ");
            return;
        }

        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.btnSave.setEnabled(false);

        String userId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("users")
                .document(userId)
                .update("name", name,
                        "phone", phone,
                        "address", address,
                        "avatarUrl", avatarUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnSave.setEnabled(true);
                });
    }
}