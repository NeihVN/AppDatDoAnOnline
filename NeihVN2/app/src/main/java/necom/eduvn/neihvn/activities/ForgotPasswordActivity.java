package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import necom.eduvn.neihvn.databinding.ActivityForgotPasswordBinding;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.ValidationUtil;

public class ForgotPasswordActivity extends AppCompatActivity {
    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();

        binding.btnResetPassword.setOnClickListener(v -> resetPassword());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Đặt lại mật khẩu");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void resetPassword() {
        String email = binding.etEmail.getText().toString().trim();

        if (!ValidationUtil.isValidEmail(email)) {
            binding.etEmail.setError("Email không hợp lệ");
            return;
        }

        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.btnResetPassword.setEnabled(false);

        FirebaseUtil.getAuth().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnResetPassword.setEnabled(true);

                    if (task.isSuccessful()) {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Thành công")
                                .setMessage("Email đặt lại mật khẩu đã được gửi đến " + email +
                                        "\n\nVui lòng kiểm tra hộp thư và làm theo hướng dẫn.")
                                .setPositiveButton("Đồng ý", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Gửi email đặt lại thất bại";
                        Toast.makeText(this, "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}