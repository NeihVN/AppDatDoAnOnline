package necom.eduvn.neihvn.utils;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;

import necom.eduvn.neihvn.models.Voucher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VoucherManager {
    private static final String TAG = "VoucherManager";
    private static VoucherManager instance;

    private VoucherManager() {}

    public static synchronized VoucherManager getInstance() {
        if (instance == null) {
            instance = new VoucherManager();
        }
        return instance;
    }

    public interface OnVoucherValidationListener {
        void onValid(Voucher voucher, double discountAmount);
        void onInvalid(String errorMessage);
    }

    public interface OnVouchersLoadListener {
        void onLoaded(List<Voucher> vouchers);
        void onError(String errorMessage);
    }

    /**
     * Validate voucher code for a specific restaurant and order amount
     */
    public void validateVoucher(String restaurantId, String code, double orderAmount, OnVoucherValidationListener listener) {
        if (code == null || code.trim().isEmpty()) {
            listener.onInvalid("Vui lòng nhập mã voucher");
            return;
        }

        String trimmedCode = code.trim().toUpperCase();
        
        Log.d(TAG, "=== Validating Voucher ===");
        Log.d(TAG, "Code: " + trimmedCode);
        Log.d(TAG, "Restaurant ID: " + restaurantId);
        Log.d(TAG, "Order Amount: " + CurrencyFormatter.format(orderAmount));

        FirebaseUtil.getFirestore().collection("vouchers")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("code", trimmedCode)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Query returned " + queryDocumentSnapshots.size() + " results");
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onInvalid("Mã voucher không tồn tại hoặc không áp dụng cho nhà hàng này");
                        return;
                    }

                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                    Voucher voucher = doc.toObject(Voucher.class);
                    
                    if (voucher == null) {
                        Log.e(TAG, "Failed to parse voucher from Firestore");
                        listener.onInvalid("Lỗi tải thông tin voucher");
                        return;
                    }

                    // Set voucherId from document ID
                    voucher.setVoucherId(doc.getId());
                    
                    Log.d(TAG, "Voucher found:");
                    Log.d(TAG, "  Type: " + voucher.getDiscountType());
                    Log.d(TAG, "  Value: " + voucher.getDiscountValue());
                    Log.d(TAG, "  Min Order: " + CurrencyFormatter.format(voucher.getMinOrderAmount()));
                    Log.d(TAG, "  Max Discount: " + CurrencyFormatter.format(voucher.getMaxDiscount()));
                    Log.d(TAG, "  Usage: " + voucher.getUsedCount() + "/" + voucher.getUsageLimit());

                    // Validate voucher
                    String validationError = validateVoucherRules(voucher, orderAmount);
                    if (validationError != null) {
                        Log.w(TAG, "Validation failed: " + validationError);
                        listener.onInvalid(validationError);
                        return;
                    }

                    // Calculate discount
                    double discountAmount = voucher.calculateDiscount(orderAmount);
                    Log.d(TAG, "Discount calculated: " + CurrencyFormatter.format(discountAmount));
                    Log.d(TAG, "Voucher validated successfully!");
                    
                    listener.onValid(voucher, discountAmount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error validating voucher: " + e.getMessage(), e);
                    listener.onInvalid("Lỗi kiểm tra voucher: " + e.getMessage());
                });
    }

    /**
     * Load available vouchers for a restaurant
     */
    public void loadAvailableVouchers(String restaurantId, double orderAmount, OnVouchersLoadListener listener) {
        long now = System.currentTimeMillis();

        FirebaseUtil.getFirestore().collection("vouchers")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("active", true)
                .whereLessThanOrEqualTo("startDate", now)
                .whereGreaterThanOrEqualTo("endDate", now)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Voucher> vouchers = new ArrayList<>();
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Voucher voucher = doc.toObject(Voucher.class);
                        if (voucher != null) {
                            voucher.setVoucherId(doc.getId());
                            
                            // Only add vouchers that are valid and meet requirements
                            if (voucher.isValid() && orderAmount >= voucher.getMinOrderAmount()) {
                                // Check usage limit
                                if (voucher.getUsageLimit() == 0 || voucher.getUsedCount() < voucher.getUsageLimit()) {
                                    vouchers.add(voucher);
                                }
                            }
                        }
                    }
                    
                    listener.onLoaded(vouchers);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading vouchers: " + e.getMessage(), e);
                    listener.onError("Lỗi tải danh sách voucher: " + e.getMessage());
                });
    }

    /**
     * Increment voucher usage count
     */
    public void incrementVoucherUsage(String voucherId, Runnable onSuccess, Runnable onFailure) {
        if (voucherId == null || voucherId.isEmpty()) {
            if (onFailure != null) onFailure.run();
            return;
        }

        FirebaseUtil.getFirestore().collection("vouchers")
                .document(voucherId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Voucher voucher = documentSnapshot.toObject(Voucher.class);
                        if (voucher != null) {
                            int newUsedCount = voucher.getUsedCount() + 1;
                            
                            documentSnapshot.getReference()
                                    .update("usedCount", newUsedCount)
                                    .addOnSuccessListener(aVoid -> {
                                        if (onSuccess != null) onSuccess.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error incrementing usage: " + e.getMessage(), e);
                                        if (onFailure != null) onFailure.run();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting voucher: " + e.getMessage(), e);
                    if (onFailure != null) onFailure.run();
                });
    }

    /**
     * Validate voucher rules
     */
    private String validateVoucherRules(Voucher voucher, double orderAmount) {
        long now = System.currentTimeMillis();

        if (!voucher.isActive()) {
            return "Voucher đã bị vô hiệu hóa bởi nhà hàng.";
        }

        if (now < voucher.getStartDate()) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String startDateStr = sdf.format(new Date(voucher.getStartDate()));
            return "Voucher chưa có hiệu lực.\nCó thể sử dụng từ: " + startDateStr;
        }

        if (now > voucher.getEndDate()) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String endDateStr = sdf.format(new Date(voucher.getEndDate()));
            return "Voucher đã hết hạn vào: " + endDateStr;
        }

        if (orderAmount < voucher.getMinOrderAmount()) {
            return String.format("Đơn hàng tối thiểu: %s\nĐơn hàng hiện tại: %s\nThiếu: %s",
                    CurrencyFormatter.format(voucher.getMinOrderAmount()),
                    CurrencyFormatter.format(orderAmount),
                    CurrencyFormatter.format(voucher.getMinOrderAmount() - orderAmount));
        }

        if (voucher.getUsageLimit() > 0 && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return String.format("Voucher đã hết lượt sử dụng.\nĐã dùng: %d/%d lượt", 
                    voucher.getUsedCount(), 
                    voucher.getUsageLimit());
        }

        return null; // Valid
    }

    /**
     * Get discount display text
     */
    public static String getDiscountDisplayText(Voucher voucher) {
        if (voucher == null) return "";

        if ("percentage".equals(voucher.getDiscountType())) {
            String text = String.format("Giảm %.0f%%", voucher.getDiscountValue());
            if (voucher.getMaxDiscount() > 0) {
                text += String.format(" (tối đa %s)", CurrencyFormatter.format(voucher.getMaxDiscount()));
            }
            return text;
        } else {
            return String.format("Giảm %s", CurrencyFormatter.format(voucher.getDiscountValue()));
        }
    }
}

