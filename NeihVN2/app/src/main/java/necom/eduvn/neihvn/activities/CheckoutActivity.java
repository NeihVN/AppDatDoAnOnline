package necom.eduvn.neihvn.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.adapters.CheckoutAdapter;
import necom.eduvn.neihvn.databinding.ActivityCheckoutBinding;
import necom.eduvn.neihvn.models.Order;
import necom.eduvn.neihvn.models.OrderItem;
import necom.eduvn.neihvn.models.Voucher;
import necom.eduvn.neihvn.utils.CartManager;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.VNPayUtil;
import necom.eduvn.neihvn.utils.VoucherManager;

import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {
    private ActivityCheckoutBinding binding;
    private CartManager cartManager;
    private CheckoutAdapter adapter;
    private String paymentMethod = "Cash";
    private String pendingOrderId = null;
    private Voucher selectedVoucher = null;
    private double discountAmount = 0;
    
    private ActivityResultLauncher<Intent> vnpayLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cartManager = CartManager.getInstance();

        setupToolbar();
        setupRecyclerView();
        loadUserAddress();
        setupPaymentMethod();
        setupVoucherSection();
        displayOrderSummary();
        setupVNPayLauncher();

        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }
    
    private void setupVoucherSection() {
        binding.cardVoucher.setOnClickListener(v -> showVoucherDialog());
    }
    
    private void showVoucherDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_voucher_input, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        EditText etVoucherCode = bottomSheetView.findViewById(R.id.etVoucherCode);
        View btnApply = bottomSheetView.findViewById(R.id.btnApplyVoucher);
        View btnRemove = bottomSheetView.findViewById(R.id.btnRemoveVoucher);
        
        // Show/hide remove button based on current voucher
        if (selectedVoucher != null) {
            btnRemove.setVisibility(View.VISIBLE);
            etVoucherCode.setText(selectedVoucher.getCode());
        } else {
            btnRemove.setVisibility(View.GONE);
        }
        
        // Apply voucher
        btnApply.setOnClickListener(v -> {
            String code = etVoucherCode.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String restaurantId = cartManager.getCurrentRestaurantId();
            double subtotal = cartManager.getTotalAmount();
            
            Log.d("CheckoutActivity", "Validating voucher: " + code);
            Log.d("CheckoutActivity", "Restaurant ID: " + restaurantId);
            Log.d("CheckoutActivity", "Order subtotal: " + CurrencyFormatter.format(subtotal));
            
            VoucherManager.getInstance().validateVoucher(restaurantId, code, subtotal, 
                new VoucherManager.OnVoucherValidationListener() {
                    @Override
                    public void onValid(Voucher voucher, double discount) {
                        Log.d("CheckoutActivity", "Voucher valid! Discount: " + CurrencyFormatter.format(discount));
                        Log.d("CheckoutActivity", "Voucher type: " + voucher.getDiscountType());
                        Log.d("CheckoutActivity", "Voucher value: " + voucher.getDiscountValue());
                        
                        selectedVoucher = voucher;
                        discountAmount = discount;
                        updateVoucherUI();
                        displayOrderSummary();
                        bottomSheetDialog.dismiss();
                        
                        String successMsg = String.format(Locale.getDefault(),
                                "✅ Áp dụng voucher thành công!\n\n%s\nTiết kiệm: %s",
                                VoucherManager.getDiscountDisplayText(voucher),
                                CurrencyFormatter.format(discount));
                        Toast.makeText(CheckoutActivity.this, successMsg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onInvalid(String errorMessage) {
                        Log.w("CheckoutActivity", "Voucher invalid: " + errorMessage);
                        showVoucherErrorDialog(errorMessage, code);
                    }
                });
        });
        
        // Remove voucher
        btnRemove.setOnClickListener(v -> {
            selectedVoucher = null;
            discountAmount = 0;
            updateVoucherUI();
            displayOrderSummary();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Đã xóa voucher", Toast.LENGTH_SHORT).show();
        });
        
        bottomSheetDialog.show();
    }
    
    private void updateVoucherUI() {
        if (selectedVoucher != null) {
            binding.tvVoucherStatus.setText(selectedVoucher.getCode());
            binding.tvVoucherDiscount.setText(VoucherManager.getDiscountDisplayText(selectedVoucher));
            binding.tvVoucherDiscount.setVisibility(View.VISIBLE);
            
            Log.d("CheckoutActivity", "Voucher UI updated:");
            Log.d("CheckoutActivity", "  Code displayed: " + selectedVoucher.getCode());
            Log.d("CheckoutActivity", "  Discount text: " + VoucherManager.getDiscountDisplayText(selectedVoucher));
        } else {
            binding.tvVoucherStatus.setText("Chọn hoặc nhập mã");
            binding.tvVoucherDiscount.setVisibility(View.GONE);
            
            Log.d("CheckoutActivity", "Voucher UI cleared");
        }
    }
    
    private void showVoucherErrorDialog(String errorMessage, String voucherCode) {
        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_voucher_error, null);
        
        // Set voucher code and error message
        android.widget.TextView tvVoucherCode = dialogView.findViewById(R.id.tvVoucherCode);
        android.widget.TextView tvErrorMessage = dialogView.findViewById(R.id.tvErrorMessage);
        com.google.android.material.button.MaterialButton btnOk = dialogView.findViewById(R.id.btnOk);
        
        tvVoucherCode.setText(voucherCode.toUpperCase());
        tvErrorMessage.setText(errorMessage);
        
        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // Set button click listener
        btnOk.setOnClickListener(v -> dialog.dismiss());
        
        // Show dialog
        dialog.show();
    }

    private void setupVNPayLauncher() {
        vnpayLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleVNPayResult
        );
    }

    private void handleVNPayResult(ActivityResult result) {
        Log.d("CheckoutActivity", "=== VNPay Result Received ===");
        Log.d("CheckoutActivity", "Result code: " + result.getResultCode());
        
        binding.progressBar.setVisibility(android.view.View.GONE);
        binding.btnPlaceOrder.setEnabled(true);
        
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Intent data = result.getData();
            boolean success = data.getBooleanExtra("success", false);
            String message = data.getStringExtra("message");
            String orderId = data.getStringExtra("orderId");
            String transactionNo = data.getStringExtra("transactionNo");
            
            Log.d("CheckoutActivity", "Payment success: " + success);
            Log.d("CheckoutActivity", "Order ID: " + orderId);
            Log.d("CheckoutActivity", "Transaction No: " + transactionNo);
            Log.d("CheckoutActivity", "Message: " + message);
            
            if (success) {
                // VNPayPaymentActivity đã cập nhật đơn hàng rồi, chỉ cần xóa cart và hiển thị thành công
                Log.d("CheckoutActivity", "Clearing cart after successful payment");
                cartManager.clearCart();
                pendingOrderId = null;
                showSuccessDialog(message != null ? message : "Thanh toán thành công! Đơn hàng đã được đặt.");
            } else {
                // Thanh toán thất bại - VNPayPaymentActivity đã xóa đơn treo rồi
                Log.d("CheckoutActivity", "Payment failed, pending order should be deleted");
                pendingOrderId = null;
                Toast.makeText(this, message != null ? message : "Thanh toán thất bại", Toast.LENGTH_LONG).show();
            }
        } else if (result.getResultCode() == RESULT_CANCELED) {
            // User cancelled payment - VNPayPaymentActivity sẽ xóa đơn treo
            Log.d("CheckoutActivity", "Payment cancelled by user");
            pendingOrderId = null;
            Toast.makeText(this, "Thanh toán bị hủy", Toast.LENGTH_SHORT).show();
        } else {
            Log.w("CheckoutActivity", "Unexpected result code or null data");
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Thanh toán");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new CheckoutAdapter(cartManager.getCartItems());
        binding.recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewItems.setAdapter(adapter);
    }

    private void loadUserAddress() {
        String userId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String address = documentSnapshot.getString("address");
                        if (address != null && !address.isEmpty()) {
                            binding.tvDeliveryAddress.setText(address);
                        } else {
                            binding.tvDeliveryAddress.setText("Vui lòng thêm địa chỉ giao hàng");
                        }
                    }
                });
    }

    private void setupPaymentMethod() {
        binding.radioGroupPayment.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioCash) {
                    paymentMethod = "Cash";
                } else if (checkedId == R.id.radioBankTransfer) {
                    paymentMethod = "Bank Transfer";
                } else if (checkedId == R.id.radioVNPay) {
                    paymentMethod = "VNPay";
                }
            }
        });
    }

    private void displayOrderSummary() {
        double subtotal = cartManager.getTotalAmount();
        double deliveryFee = 2.00;
        
        // Apply voucher discount if selected
        if (selectedVoucher != null) {
            discountAmount = selectedVoucher.calculateDiscount(subtotal);
            Log.d("CheckoutActivity", "Recalculating discount in displayOrderSummary:");
            Log.d("CheckoutActivity", "  Subtotal: " + CurrencyFormatter.format(subtotal));
            Log.d("CheckoutActivity", "  Voucher type: " + selectedVoucher.getDiscountType());
            Log.d("CheckoutActivity", "  Voucher value: " + selectedVoucher.getDiscountValue());
            Log.d("CheckoutActivity", "  Calculated discount: " + CurrencyFormatter.format(discountAmount));
        } else {
            discountAmount = 0;
        }
        
        double total = subtotal + deliveryFee - discountAmount;
        
        // Ensure total is not negative
        if (total < 0) {
            Log.w("CheckoutActivity", "Total was negative, setting to 0");
            total = 0;
        }

        binding.tvSubtotal.setText(CurrencyFormatter.format(subtotal));
        binding.tvDeliveryFee.setText(CurrencyFormatter.format(deliveryFee));
        
        // Show/hide discount row
        if (discountAmount > 0 && selectedVoucher != null) {
            binding.layoutDiscount.setVisibility(View.VISIBLE);
            binding.tvDiscount.setText(CurrencyFormatter.format(-discountAmount));
            binding.tvDiscountDetail.setText("Mã: " + selectedVoucher.getCode() + " • " + VoucherManager.getDiscountDisplayText(selectedVoucher));
            
            Log.d("CheckoutActivity", ">>> Discount UI updated <<<");
            Log.d("CheckoutActivity", "  layoutDiscount visibility: VISIBLE");
            Log.d("CheckoutActivity", "  tvDiscount text: " + binding.tvDiscount.getText());
            Log.d("CheckoutActivity", "  tvDiscountDetail text: " + binding.tvDiscountDetail.getText());
        } else {
            binding.layoutDiscount.setVisibility(View.GONE);
            
            Log.d("CheckoutActivity", ">>> Discount UI hidden <<<");
            Log.d("CheckoutActivity", "  layoutDiscount visibility: GONE");
            Log.d("CheckoutActivity", "  Reason: discountAmount=" + discountAmount + ", selectedVoucher=" + (selectedVoucher != null ? "exists" : "null"));
        }
        
        // Show original price (strikethrough) if discount applied
        if (discountAmount > 0) {
            double originalTotal = subtotal + deliveryFee;
            binding.tvOriginalTotal.setVisibility(View.VISIBLE);
            binding.tvOriginalTotal.setText(CurrencyFormatter.format(originalTotal));
            binding.tvOriginalTotal.setPaintFlags(binding.tvOriginalTotal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            
            Log.d("CheckoutActivity", "  Original total (strikethrough): " + binding.tvOriginalTotal.getText());
        } else {
            binding.tvOriginalTotal.setVisibility(View.GONE);
        }
        
        binding.tvTotal.setText(CurrencyFormatter.format(total));
        binding.btnPlaceOrder.setText(String.format(Locale.getDefault(), "Đặt hàng (%s)", CurrencyFormatter.format(total)));
        
        Log.d("CheckoutActivity", "Order Summary: Subtotal=" + CurrencyFormatter.format(subtotal) +
              ", Delivery=" + CurrencyFormatter.format(deliveryFee) +
              ", Discount=" + CurrencyFormatter.format(discountAmount) +
              ", Total=" + CurrencyFormatter.format(total));
    }

    private void placeOrder() {
        String address = binding.tvDeliveryAddress.getText().toString();

        if (address.equals("Vui lòng thêm địa chỉ giao hàng")) {
            Toast.makeText(this, "Vui lòng thêm địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // If VNPay, create order first then open payment
        if ("VNPay".equals(paymentMethod)) {
            processVNPayPayment(address);
        } else {
            processCashPayment(address);
        }
    }

    private void processVNPayPayment(String address) {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.btnPlaceOrder.setEnabled(false);

        String orderId = FirebaseUtil.getFirestore().collection("orders").document().getId();
        String buyerId = FirebaseUtil.getCurrentUserId();
        String restaurantId = cartManager.getCurrentRestaurantId();
        List<OrderItem> items = cartManager.getCartItems();
        double subtotal = cartManager.getTotalAmount();
        double deliveryFee = 2.00;
        double total = subtotal + deliveryFee - discountAmount;

        // Create order with "Pending Payment" status
        Order order = new Order(orderId, buyerId, restaurantId, items, total);
        order.setDeliveryAddress(address);
        order.setPaymentMethod("VNPay");
        order.setStatus("Pending Payment");
        
        // Set voucher info if used
        if (selectedVoucher != null && discountAmount > 0) {
            order.setVoucherId(selectedVoucher.getVoucherId());
            order.setVoucherCode(selectedVoucher.getCode());
            order.setDiscountAmount(discountAmount);
            order.setSubtotal(subtotal + deliveryFee);
        }

        FirebaseUtil.getFirestore().collection("orders")
                .document(orderId)
                .set(order)
                .addOnSuccessListener(aVoid -> {
                    pendingOrderId = orderId;
                    
                    // Create VNPay payment URL
                    // Convert USD to VND for VNPay (VNPay only supports VND)
                    long amountVND = VNPayUtil.convertToVND(total);
                    String orderDescription = "Order #" + orderId.substring(0, 8);
                    
                    // Log conversion for debugging
                    Log.d("CheckoutActivity", String.format(Locale.getDefault(),
                            "Chuyển đổi sang VND: %.2f USD = %s",
                            total, VNPayUtil.formatVND(amountVND)));
                    
                    String paymentUrl = VNPayUtil.createPaymentUrl(orderId, amountVND, orderDescription);
                    
                    if (paymentUrl != null) {
                        // Open VNPay payment activity
                        Intent intent = new Intent(this, VNPayPaymentActivity.class);
                        intent.putExtra("orderId", orderId);
                        intent.putExtra("paymentUrl", paymentUrl);
                        intent.putExtra("amountUSD", total);
                        intent.putExtra("amountVND", amountVND);
                        vnpayLauncher.launch(intent);
                    } else {
                        Toast.makeText(this, "Lỗi tạo URL thanh toán", Toast.LENGTH_SHORT).show();
                        // Delete the pending order
                        FirebaseUtil.getFirestore().collection("orders").document(orderId).delete();
                    }
                    
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnPlaceOrder.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnPlaceOrder.setEnabled(true);
                });
    }

    private void processCashPayment(String address) {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.btnPlaceOrder.setEnabled(false);

        String orderId = FirebaseUtil.getFirestore().collection("orders").document().getId();
        String buyerId = FirebaseUtil.getCurrentUserId();
        String restaurantId = cartManager.getCurrentRestaurantId();
        List<OrderItem> items = cartManager.getCartItems();
        double subtotal = cartManager.getTotalAmount();
        double deliveryFee = 2.00;
        double total = subtotal + deliveryFee - discountAmount;

        Order order = new Order(orderId, buyerId, restaurantId, items, total);
        order.setDeliveryAddress(address);
        order.setPaymentMethod(paymentMethod);
        
        // Set voucher info if used
        if (selectedVoucher != null && discountAmount > 0) {
            order.setVoucherId(selectedVoucher.getVoucherId());
            order.setVoucherCode(selectedVoucher.getCode());
            order.setDiscountAmount(discountAmount);
            order.setSubtotal(subtotal + deliveryFee);
        }

        FirebaseUtil.getFirestore().collection("orders")
                .document(orderId)
                .set(order)
                .addOnSuccessListener(aVoid -> {
                    // Increment voucher usage if used
                    if (selectedVoucher != null && selectedVoucher.getVoucherId() != null) {
                        VoucherManager.getInstance().incrementVoucherUsage(
                            selectedVoucher.getVoucherId(),
                            () -> Log.d("CheckoutActivity", "Voucher usage incremented"),
                            () -> Log.e("CheckoutActivity", "Failed to increment voucher usage")
                        );
                    }
                    
                    cartManager.clearCart();
                    showSuccessDialog("Đơn hàng đã được đặt. Theo dõi đơn hàng trong mục Đơn hàng.");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnPlaceOrder.setEnabled(true);
                });
    }

    private void showSuccessDialog(String message) {
        binding.progressBar.setVisibility(android.view.View.GONE);
        binding.btnPlaceOrder.setEnabled(true);
        
        Intent resultIntent = new Intent();
        resultIntent.putExtra("paymentSuccess", true);
        resultIntent.putExtra("paymentMessage", message);
        setResult(RESULT_OK, resultIntent);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đặt hàng thành công!")
                .setMessage(message)
                .setPositiveButton("Đóng", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}