package necom.eduvn.neihvn.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import necom.eduvn.neihvn.databinding.ActivitySplashBinding;
import necom.eduvn.neihvn.utils.FirebaseUtil;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Handler().postDelayed(() -> {
            if (FirebaseUtil.isUserLoggedIn()) {
                checkUserRole();
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, 2000);
    }

    private void checkUserRole() {
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
                    } else {
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }
}