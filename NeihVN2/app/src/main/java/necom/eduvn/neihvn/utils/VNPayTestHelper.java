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
 * Helper class to test VNPay integration and debug hash calculation
 */
public class VNPayTestHelper {
    private static final String TAG = "VNPayTestHelper";
    
    /**
     * Test method to verify hash calculation
     * Compare with expected values from VNPay documentation
     */
    public static void testHashCalculation() {
        try {
            String secretKey = VNPayUtil.VNPAY_HASH_SECRET;
            String tmnCode = VNPayUtil.VNPAY_TMN_CODE;
            
            // Test data
            Map<String, String> testParams = new HashMap<>();
            testParams.put("vnp_Version", "2.1.0");
            testParams.put("vnp_Command", "pay");
            testParams.put("vnp_TmnCode", tmnCode);
            testParams.put("vnp_Amount", "100000");
            testParams.put("vnp_CurrCode", "VND");
            testParams.put("vnp_TxnRef", "TEST_" + System.currentTimeMillis());
            testParams.put("vnp_OrderInfo", "Đơn hàng thử nghiệm");
            testParams.put("vnp_OrderType", "other");
            testParams.put("vnp_Locale", "vn");
            testParams.put("vnp_ReturnUrl", "http://localhost:2409/api/payments/vnpay/return");
            
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            String createDate = formatter.format(new Date());
            testParams.put("vnp_CreateDate", createDate);
            testParams.put("vnp_IpAddr", "127.0.0.1");
            
            // Sort and build hash data
            List<String> fieldNames = new ArrayList<>(testParams.keySet());
            Collections.sort(fieldNames);
            
            StringBuilder hashData = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = testParams.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    if (hashData.length() > 0) {
                        hashData.append("&");
                    }
                    hashData.append(fieldName);
                    hashData.append("=");
                    hashData.append(fieldValue);
                }
            }
            
            String hashDataStr = hashData.toString();
            Log.d(TAG, "=== VNPay Hash Test ===");
            Log.d(TAG, "Hash Data String: " + hashDataStr);
            
            // Calculate hash using reflection to access private method
            // For now, just log the hash data string for manual verification
            
        } catch (Exception e) {
            Log.e(TAG, "Error in test: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate VNPay configuration
     */
    public static boolean validateConfig() {
        boolean isValid = true;
        
        if (VNPayUtil.VNPAY_TMN_CODE == null || VNPayUtil.VNPAY_TMN_CODE.isEmpty()) {
            Log.e(TAG, "VNPAY_TMN_CODE is missing");
            isValid = false;
        }
        
        if (VNPayUtil.VNPAY_HASH_SECRET == null || VNPayUtil.VNPAY_HASH_SECRET.isEmpty()) {
            Log.e(TAG, "VNPAY_HASH_SECRET is missing");
            isValid = false;
        }
        
        if (VNPayUtil.VNPAY_RETURN_URL == null || VNPayUtil.VNPAY_RETURN_URL.isEmpty()) {
            Log.e(TAG, "VNPAY_RETURN_URL is missing");
            isValid = false;
        }
        
        // Check if return URL is localhost (may cause issues)
        if (VNPayUtil.VNPAY_RETURN_URL.contains("localhost")) {
            Log.w(TAG, "WARNING: Return URL contains localhost. VNPay server cannot access localhost!");
            Log.w(TAG, "Consider using ngrok or a public URL for testing.");
        }
        
        if (isValid) {
            Log.d(TAG, "VNPay configuration is valid");
        }
        
        return isValid;
    }
}

