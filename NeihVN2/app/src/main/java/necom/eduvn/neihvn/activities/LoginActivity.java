package necom.eduvn.neihvn.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import necom.eduvn.neihvn.databinding.ActivityLoginBinding;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.ValidationUtil;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseUtil.getAuth();

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        // Add this in onCreate after setting up views
        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (!ValidationUtil.isValidEmail(email)) {
            binding.etEmail.setError("Email không hợp lệ");
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            binding.etPassword.setError("Mật khẩu cần tối thiểu 6 ký tự");
            return;
        }

        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        checkUserRoleAndNavigate();
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRoleAndNavigate() {
        String userId = FirebaseUtil.getCurrentUserId();
        FirebaseUtil.getFirestore().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Intent intent;

                        switch (role) {
                            case "admin":
                                intent = new Intent(this, AdminMainActivity.class);
                                break;
                            case "seller":
                                intent = new Intent(this, SellerMainActivity.class);
                                break;
                            default:
                                intent = new Intent(this, BuyerMainActivity.class);
                                break;
                        }
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}