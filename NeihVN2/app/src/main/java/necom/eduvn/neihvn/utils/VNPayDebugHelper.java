package necom.eduvn.neihvn.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Debug helper to test VNPay hash calculation
 * Use this to compare your hash with VNPay's expected hash
 */
public class VNPayDebugHelper {
    private static final String TAG = "VNPayDebug";
    
    /**
     * Print all parameters that will be used for hash calculation
     */
    public static void debugPaymentParams(String orderId, long amount, String orderDescription) {
        Log.d(TAG, "=== VNPay Payment Parameters Debug ===");
        
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", VNPayUtil.VNPAY_TMN_CODE);
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        
        String vnp_TxnRef = orderId + "_" + System.currentTimeMillis();
        if (vnp_TxnRef.length() > 100) {
            vnp_TxnRef = vnp_TxnRef.substring(0, 100);
        }
        params.put("vnp_TxnRef", vnp_TxnRef);
        
        String orderInfo = orderDescription != null ? orderDescription : "Thanh toán đơn hàng";
        if (orderInfo.length() > 255) {
            orderInfo = orderInfo.substring(0, 255);
        }
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", VNPayUtil.VNPAY_RETURN_URL);
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String vnp_CreateDate = formatter.format(new Date());
        params.put("vnp_CreateDate", vnp_CreateDate);
        params.put("vnp_IpAddr", "127.0.0.1");
        
        // Sort and print
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        
        Log.d(TAG, "Sorted Parameters (for hash calculation):");
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                Log.d(TAG, "  " + fieldName + " = " + fieldValue);
                if (hashData.length() > 0) {
                    hashData.append("&");
                }
                hashData.append(fieldName).append("=").append(fieldValue);
            }
        }
        
        Log.d(TAG, "Hash Data String: " + hashData.toString());
        Log.d(TAG, "Secret Key: " + VNPayUtil.VNPAY_HASH_SECRET);
        Log.d(TAG, "================================");
    }
}

