package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ActivityRegisterBinding;
import necom.eduvn.neihvn.models.User;
import necom.eduvn.neihvn.utils.EmailSender;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.ValidationUtil;

public class RegisterActivity extends AppCompatActivity {
    private static final int OTP_LENGTH = 6;
    private static final long OTP_VALID_DURATION = TimeUnit.MINUTES.toMillis(5);
    private static final long RESEND_INTERVAL = TimeUnit.SECONDS.toMillis(60);

    private ActivityRegisterBinding binding;
    private ExecutorService mailExecutor;

    private AlertDialog otpDialog;
    private TextInputLayout layoutOtp;
    private EditText etOtp;
    private ProgressBar otpProgressBar;
    private Button btnConfirmOtp;
    private Button btnResendOtp;

    private String pendingEmail;
    private String pendingPassword;
    private String pendingName;
    private String pendingPhone;
    private String pendingRole;

    private String currentOtp;
    private long otpExpiryMillis;
    private long lastOtpSentMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mailExecutor = Executors.newSingleThreadExecutor();

        setupRoleSpinner();

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        if (otpDialog != null && otpDialog.isShowing()) {
            otpDialog.dismiss();
        }
        if (mailExecutor != null && !mailExecutor.isShutdown()) {
            mailExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    private void setupRoleSpinner() {
        String[] roles = {
                getString(R.string.role_buyer_display),
                getString(R.string.role_seller_display)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roles);
        binding.spinnerRole.setAdapter(adapter);
    }

    private void registerUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String roleDisplay = binding.spinnerRole.getSelectedItem().toString();
        String role;
        if (roleDisplay.equals(getString(R.string.role_buyer_display))) {
            role = "buyer";
        } else {
            role = "seller";
        }

        if (name.isEmpty()) {
            binding.etName.setError("Vui lòng nhập họ tên");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            binding.etEmail.setError("Địa chỉ email không hợp lệ");
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            binding.etPassword.setError("Mật khẩu cần tối thiểu 6 ký tự");
            return;
        }

        if (!ValidationUtil.isValidPhone(phone)) {
            binding.etPhone.setError("Số điện thoại không hợp lệ");
            return;
        }

        pendingEmail = email;
        pendingPassword = password;
        pendingName = name;
        pendingPhone = phone;
        pendingRole = role;

        currentOtp = generateOtp();
        otpExpiryMillis = System.currentTimeMillis() + OTP_VALID_DURATION;

        sendOtpEmail(false);
    }

    private void sendOtpEmail(boolean isResend) {
        if (!isResend) {
            showMainLoading(true);
        } else {
            toggleDialogLoading(true);
        }

        mailExecutor.execute(() -> {
            try {
                String subject = getString(R.string.otp_email_subject);
                String body = getString(R.string.otp_email_body, pendingName, currentOtp);
                EmailSender.sendEmail(pendingEmail, subject, body);

                runOnUiThread(() -> {
                    lastOtpSentMillis = System.currentTimeMillis();
                    if (isResend) {
                        toggleDialogLoading(false);
                        Toast.makeText(this,
                                getString(R.string.otp_sent_success, pendingEmail),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        showMainLoading(false);
                        Toast.makeText(this,
                                getString(R.string.otp_sent_success, pendingEmail),
                                Toast.LENGTH_SHORT).show();
                        showOtpDialog();
                    }
                });
            } catch (Exception e) {
                Log.e("RegisterActivity", "sendOtpEmail: ", e);
                runOnUiThread(() -> {
                    if (isResend) {
                        toggleDialogLoading(false);
                    } else {
                        showMainLoading(false);
                        binding.btnRegister.setEnabled(true);
                    }
                    Toast.makeText(this,
                            getString(R.string.otp_send_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showOtpDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_verify_otp, null);
        layoutOtp = view.findViewById(R.id.layoutOtp);
        etOtp = view.findViewById(R.id.etOtp);
        otpProgressBar = view.findViewById(R.id.progressOtp);
        btnConfirmOtp = view.findViewById(R.id.btnConfirmOtp);
        btnResendOtp = view.findViewById(R.id.btnResendOtp);
        Button btnCancelOtp = view.findViewById(R.id.btnCancelOtp);
        TextView tvDescription = view.findViewById(R.id.tvOtpDescription);

        tvDescription.setText(getString(R.string.otp_dialog_description, pendingEmail));

        btnConfirmOtp.setOnClickListener(v -> verifyOtp());
        btnResendOtp.setOnClickListener(v -> resendOtp());
        btnCancelOtp.setOnClickListener(v -> {
            if (otpDialog != null) {
                otpDialog.dismiss();
            }
            showMainLoading(false);
            binding.btnRegister.setEnabled(true);
        });

        otpDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();
        otpDialog.show();
    }

    private void verifyOtp() {
        if (layoutOtp != null) {
            layoutOtp.setError(null);
        }
        String input = etOtp != null ? etOtp.getText().toString().trim() : "";
        if (input.length() != OTP_LENGTH) {
            if (layoutOtp != null) {
                layoutOtp.setError("Mã gồm 6 chữ số");
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now > otpExpiryMillis) {
            Toast.makeText(this, R.string.otp_expired, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!input.equals(currentOtp)) {
            if (layoutOtp != null) {
                layoutOtp.setError(getString(R.string.otp_invalid));
            }
            return;
        }

        toggleDialogLoading(true);
        createAccountWithFirebase();
    }

    private void resendOtp() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastOtpSentMillis;
        if (elapsed < RESEND_INTERVAL) {
            long waitSeconds = TimeUnit.MILLISECONDS.toSeconds(RESEND_INTERVAL - elapsed);
            Toast.makeText(this,
                    getString(R.string.otp_resend_wait, waitSeconds),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        currentOtp = generateOtp();
        otpExpiryMillis = now + OTP_VALID_DURATION;
        sendOtpEmail(true);
    }

    private void createAccountWithFirebase() {
        FirebaseUtil.getAuth().createUserWithEmailAndPassword(pendingEmail, pendingPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = FirebaseUtil.getCurrentUserId();
                        User user = new User(userId, pendingEmail, pendingName, pendingRole);
                        user.setPhone(pendingPhone);

                        FirebaseUtil.getFirestore().collection("users")
                                .document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    if (otpDialog != null && otpDialog.isShowing()) {
                                        otpDialog.dismiss();
                                    }
                                    Toast.makeText(this,
                                            "Đăng ký thành công!",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("RegisterActivity", "Firestore error", e);
                                    toggleDialogLoading(false);
                                    Toast.makeText(this,
                                            "Không thể lưu thông tin người dùng: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        toggleDialogLoading(false);
                        Toast.makeText(this,
                                "Đăng ký thất bại: " + (task.getException() != null
                                        ? task.getException().getMessage()
                                        : ""),
                                Toast.LENGTH_LONG).show();
                        Log.e("RegisterActivity", "Auth error", task.getException());
                    }
                });
    }

    private void showMainLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!show);
    }

    private void toggleDialogLoading(boolean loading) {
        if (otpProgressBar != null) {
            otpProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnConfirmOtp != null) {
            btnConfirmOtp.setEnabled(!loading);
        }
        if (btnResendOtp != null) {
            btnResendOtp.setEnabled(!loading);
        }
    }

    private String generateOtp() {
        Random random = new Random();
        int number = random.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format(Locale.US, "%0" + OTP_LENGTH + "d", number);
    }
}