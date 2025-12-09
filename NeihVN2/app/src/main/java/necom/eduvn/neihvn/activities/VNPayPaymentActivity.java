package necom.eduvn.neihvn.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import necom.eduvn.neihvn.databinding.ActivityVnpayPaymentBinding;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.VNPayUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VNPayPaymentActivity extends AppCompatActivity {
    private static final String TAG = "VNPayPaymentActivity";
    private ActivityVnpayPaymentBinding binding;
    private String orderId;
    private String paymentUrl;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVnpayPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Handle deep link (VNPay return URL) - if opened from deep link
        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data != null) {
            String scheme = data.getScheme();
            String host = data.getHost();
            
            // Handle deep link or return URL with VNPay response
            if (data.toString().contains("vnp_ResponseCode")) {
                // Handle return from VNPay
                handlePaymentResponse(data.toString());
                return;
            }
        }
        
        // Get order info from intent
        orderId = intent.getStringExtra("orderId");
        paymentUrl = intent.getStringExtra("paymentUrl");
        
        if (paymentUrl == null || paymentUrl.isEmpty()) {
            Toast.makeText(this, "URL thanh toán không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupToolbar();
        setupWebView();
        loadPaymentPage();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        // Handle deep link
        Uri data = intent.getData();
        if (data != null && "neihvn".equals(data.getScheme()) && "vnpay".equals(data.getHost())) {
            handlePaymentResponse(data.toString());
        }
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            
            // Show payment amount info if available
            Intent intent = getIntent();
            double amountUSD = intent.getDoubleExtra("amountUSD", 0);
            long amountVND = intent.getLongExtra("amountVND", 0);
            
            if (amountUSD > 0 && amountVND > 0) {
                String title = String.format(Locale.getDefault(),
                    "Thanh toán VNPay\n%s (≈ %.2f USD)",
                    VNPayUtil.formatVND(amountVND), amountUSD);
                getSupportActionBar().setTitle(title);
            } else {
                getSupportActionBar().setTitle("Thanh toán VNPay");
            }
        }
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Show confirmation dialog before canceling
            showCancelDialog();
        });
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setDomStorageEnabled(true);
        binding.webView.getSettings().setLoadWithOverviewMode(true);
        binding.webView.getSettings().setUseWideViewPort(true);
        
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                }
                
                // Check if this is a return URL with VNPay response
                // Intercept when VNPay redirects back with response parameters
                if (url != null && url.contains("vnp_ResponseCode")) {
                    Log.d(TAG, "VNPay callback detected in onPageFinished, intercepting...");
                    handlePaymentResponse(url);
                }
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "Loading URL: " + url);
                
                // Intercept any URL containing VNPay response parameters
                // This works regardless of the return URL domain
                if (url != null && url.contains("vnp_ResponseCode")) {
                    Log.d(TAG, "VNPay callback detected, intercepting...");
                    handlePaymentResponse(url);
                    return true;
                }
                
                return false;
            }
            
            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "Loading URL (deprecated): " + url);
                
                // Intercept any URL containing VNPay response parameters
                if (url != null && url.contains("vnp_ResponseCode")) {
                    Log.d(TAG, "VNPay callback detected, intercepting...");
                    handlePaymentResponse(url);
                    return true;
                }
                
                return false;
            }
        });
    }
    
    private void loadPaymentPage() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.webView.loadUrl(paymentUrl);
        }
    }
    
    private void handlePaymentResponse(String url) {
        Log.d(TAG, "=== Handling VNPay Response ===");
        
        if (binding == null) {
            // Activity not fully initialized, create new binding
            binding = ActivityVnpayPaymentBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        Map<String, String> params = VNPayUtil.parseResponseUrl(url);
        Log.d(TAG, "Parsed params count: " + params.size());
        
        if (orderId == null) {
            String vnpTxnRef = params.get("vnp_TxnRef");
            if (vnpTxnRef != null && vnpTxnRef.contains("_")) {
                orderId = vnpTxnRef.substring(0, vnpTxnRef.lastIndexOf("_"));
                Log.d(TAG, "Extracted orderId from vnp_TxnRef: " + orderId);
            }
        } else {
            Log.d(TAG, "Using existing orderId: " + orderId);
        }
        
        Map<String, String> paramsCopy = new HashMap<>(params);
        boolean isValid = VNPayUtil.verifyResponse(paramsCopy);
        
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        boolean isSuccess = isValid && "00".equals(responseCode);
        String message = getResponseMessage(responseCode);
        
        Log.d(TAG, "Hash valid: " + isValid + ", Response code: " + responseCode + ", Success: " + isSuccess);
        Log.d(TAG, "Transaction No: " + transactionNo);
        
        updateOrderRecord(orderId, isSuccess, transactionNo, () -> {
            if (binding != null) {
                binding.progressBar.setVisibility(View.GONE);
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("orderId", orderId);
            resultIntent.putExtra("success", isSuccess);
            resultIntent.putExtra("responseCode", responseCode);
            resultIntent.putExtra("transactionNo", transactionNo);
            resultIntent.putExtra("message", message);

            Log.d(TAG, "Setting result and finishing. Success: " + isSuccess);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void updateOrderRecord(String orderId, boolean isSuccess, String transactionNo, Runnable onComplete) {
        if (orderId == null || orderId.isEmpty()) {
            Log.w(TAG, "Order ID is null, skip updating record");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        if (isSuccess) {
            FirebaseUtil.getFirestore().collection("orders")
                    .document(orderId)
                    .update("status", "Processing",
                            "paymentMethod", "VNPay",
                            "transactionNo", transactionNo != null ? transactionNo : "",
                            "updatedAt", System.currentTimeMillis())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Order " + orderId + " updated to Processing");
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update order status: " + e.getMessage(), e);
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
        } else {
            FirebaseUtil.getFirestore().collection("orders")
                    .document(orderId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Pending order " + orderId + " deleted after failed payment");
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to remove pending order: " + e.getMessage(), e);
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
        }
    }
    
    private String getResponseMessage(String responseCode) {
        if (responseCode == null) {
            return "Lỗi không xác định";
        }
        
        switch (responseCode) {
            case "00":
                return "Giao dịch thành công";
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09":
                return "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
            case "10":
                return "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11":
                return "Đã hết hạn chờ thanh toán. Xin vui lòng thực hiện lại giao dịch.";
            case "12":
                return "Thẻ/Tài khoản bị khóa.";
            case "13":
                return "Nhập sai mật khẩu xác thực giao dịch (OTP). Xin vui lòng thực hiện lại giao dịch.";
            case "24":
                return "Khách hàng hủy giao dịch";
            case "51":
                return "Tài khoản không đủ số dư để thực hiện giao dịch.";
            case "65":
                return "Tài khoản đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì.";
            case "79":
                return "Nhập sai mật khẩu thanh toán quá số lần quy định. Xin vui lòng thực hiện lại giao dịch.";
            default:
                return "Có lỗi xảy ra trong quá trình thanh toán. Mã lỗi: " + responseCode;
        }
    }
    
    private void showCancelDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hủy thanh toán")
                .setMessage("Bạn có chắc chắn muốn hủy thanh toán không?")
                .setPositiveButton("Hủy thanh toán", (dialog, which) -> {
                    // Xóa đơn treo khi người dùng hủy
                    updateOrderRecord(orderId, false, null, () -> {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("orderId", orderId);
                        resultIntent.putExtra("success", false);
                        resultIntent.putExtra("message", "Thanh toán bị hủy bởi người dùng");
                        setResult(RESULT_CANCELED, resultIntent);
                        finish();
                    });
                })
                .setNegativeButton("Tiếp tục", null)
                .show();
    }
    
    @Override
    public void onBackPressed() {
        showCancelDialog();
    }
}

