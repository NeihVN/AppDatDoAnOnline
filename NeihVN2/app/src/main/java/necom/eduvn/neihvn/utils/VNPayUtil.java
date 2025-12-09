package necom.eduvn.neihvn.utils;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayUtil {
    private static final String TAG = "VNPayUtil";
    
    // VNPay Configuration - From https://sandbox.vnpayment.vn/apis/vnpay-demo/
    public static final String VNPAY_TMN_CODE = "TGETSDDK";
    public static final String VNPAY_HASH_SECRET = "RS5GOI7PNDSTL9TLJ1OBRR4RMFA3B23J";
    public static final String VNPAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    
    // Return URL - For mobile app, WebView will intercept any URL containing vnp_ResponseCode
    // You can use any valid URL pattern - WebView will catch the callback
    // Recommended: Use your actual server URL or a public test URL
    // The URL doesn't need to be accessible, as WebView intercepts before navigation
    public static final String VNPAY_RETURN_URL = "https://sandbox.vnpayment.vn/returnv2.html";
    
    /**
     * Create VNPay payment URL
     * @param orderId Order ID
     * @param amount Amount in VND
     * @param orderDescription Order description
     * @return Payment URL
     */
    public static String createPaymentUrl(String orderId, long amount, String orderDescription) {
        try {
            // Validate configuration first
            if (VNPAY_TMN_CODE == null || VNPAY_TMN_CODE.isEmpty() ||
                VNPAY_HASH_SECRET == null || VNPAY_HASH_SECRET.isEmpty()) {
                Log.e(TAG, "VNPay configuration is invalid");
                return null;
            }
            
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", VNPAY_TMN_CODE);
            
            // VNPay requires amount multiplied by 100
            // Example: 250,000 VND should be sent as 25000000
            if (amount <= 0) {
                Log.e(TAG, "Invalid amount: " + amount);
                return null;
            }
            
            // Multiply by 100 for VNPay format
            long vnpayAmount = amount * 100;
            String amountStr = String.valueOf(vnpayAmount);
            vnp_Params.put("vnp_Amount", amountStr);
            Log.d(TAG, "Amount (VND): " + amount + " -> VNPay format: " + amountStr);
            vnp_Params.put("vnp_CurrCode", "VND");
            
            // Create transaction reference - must be unique (max 100 chars)
            String vnp_TxnRef = orderId + "_" + System.currentTimeMillis();
            // Ensure it's not too long
            if (vnp_TxnRef.length() > 100) {
                vnp_TxnRef = vnp_TxnRef.substring(0, 100);
            }
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            
            // Order info - must not be empty (max 255 chars)
            String orderInfo = orderDescription != null ? orderDescription : "Thanh toán đơn hàng";
            if (orderInfo.length() > 255) {
                orderInfo = orderInfo.substring(0, 255);
            }
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", "billpayment");
            
            // Location - "vn" for Vietnamese, "en" for English
            vnp_Params.put("vnp_Locale", "vn");
            
            // Return URL - must be a valid HTTP/HTTPS URL
            // WARNING: localhost will NOT work - VNPay server cannot access it
            vnp_Params.put("vnp_ReturnUrl", VNPAY_RETURN_URL);
            
            // Create date - format: yyyyMMddHHmmss (14 digits)
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            String vnp_CreateDate = formatter.format(new Date());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            
            // IP Address - client IP (required)
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");
            
            // Match Node.js implementation:
            // 1. Encode keys and values (sortObject function)
            // 2. Sort by encoded keys
            // 3. Build query string with encoded values (querystring.stringify with encode: false)
            
            Map<String, String> encodedParams = new HashMap<>();
            for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value != null && !value.isEmpty()) {
                    // URL encode both key and value
                    String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.toString());
                    String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
                    // Replace %20 with + (space encoding) - matching Node.js behavior
                    encodedValue = encodedValue.replace("%20", "+");
                    encodedParams.put(encodedKey, encodedValue);
                }
            }
            
            // Sort by encoded keys alphabetically
            List<String> sortedKeys = new ArrayList<>(encodedParams.keySet());
            Collections.sort(sortedKeys);
            
            // Build hash data string (query string with encoded values, NO additional encoding)
            // This matches: querystring.stringify(vnp_Params, { encode: false })
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            
            for (int i = 0; i < sortedKeys.size(); i++) {
                String encodedKey = sortedKeys.get(i);
                String encodedValue = encodedParams.get(encodedKey);
                
                if (i > 0) {
                    hashData.append("&");
                    query.append("&");
                }
                hashData.append(encodedKey);
                hashData.append("=");
                hashData.append(encodedValue);
                
                query.append(encodedKey);
                query.append("=");
                query.append(encodedValue);
            }
            
            // Create secure hash using HMAC SHA512
            String hashDataStr = hashData.toString();
            Log.d(TAG, "=== VNPay Hash Calculation ===");
            Log.d(TAG, "Hash Data String (URL encoded, sorted): " + hashDataStr);
            Log.d(TAG, "Secret Key: " + VNPAY_HASH_SECRET);
            
            String vnp_SecureHash = hmacSHA512(VNPAY_HASH_SECRET, hashDataStr);
            Log.d(TAG, "Generated Hash: " + vnp_SecureHash);
            Log.d(TAG, "Hash Length: " + vnp_SecureHash.length());
            
            // Append hash to query (hash is NOT URL encoded when added)
            query.append("&vnp_SecureHash=").append(vnp_SecureHash);
            
            String paymentUrl = VNPAY_URL + "?" + query.toString();
            
            Log.d(TAG, "VNPay Payment URL created");
            Log.d(TAG, "Amount: " + amountStr + " VND");
            Log.d(TAG, "Order Info: " + orderDescription);
            
            return paymentUrl;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating VNPay URL: " + e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Verify VNPay response hash
     * @param params Response parameters from VNPay (will be modified - hash removed)
     * @return true if hash is valid
     */
    public static boolean verifyResponse(Map<String, String> params) {
        try {
            Log.d(TAG, "=== Verifying VNPay Response Hash ===");
            
            // Create a copy to avoid modifying original
            Map<String, String> paramsCopy = new HashMap<>(params);
            String vnp_SecureHash = paramsCopy.remove("vnp_SecureHash");
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                Log.w(TAG, "No secure hash found in response");
                return false;
            }
            
            Log.d(TAG, "Received Hash: " + vnp_SecureHash);
            
            // Sort params
            List<String> fieldNames = new ArrayList<>(paramsCopy.keySet());
            Collections.sort(fieldNames);
            
            Log.d(TAG, "Sorted fields: " + fieldNames);
            
            // Build hash data
            StringBuilder hashData = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = paramsCopy.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    hashData.append((hashData.length() == 0 ? "" : "&"));
                    hashData.append(fieldName);
                    hashData.append("=");
                    hashData.append(fieldValue);
                }
            }
            
            String hashDataStr = hashData.toString();
            Log.d(TAG, "Hash Data String: " + hashDataStr);
            Log.d(TAG, "Hash Data Length: " + hashDataStr.length());
            
            // Verify hash
            String calculatedHash = hmacSHA512(VNPAY_HASH_SECRET, hashDataStr);
            boolean isValid = calculatedHash.equalsIgnoreCase(vnp_SecureHash);
            
            if (!isValid) {
                Log.w(TAG, "Hash verification FAILED");
                Log.w(TAG, "Calculated: " + calculatedHash);
                Log.w(TAG, "Received:   " + vnp_SecureHash);
            } else {
                Log.d(TAG, "Hash verification SUCCESS");
            }
            
            return isValid;
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying VNPay response: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * HMAC SHA512 encryption
     * Important: Data must NOT be URL encoded when calculating hash
     * Result should be lowercase hexadecimal string
     */
    private static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                Log.e(TAG, "Key or data is null");
                return "";
            }
            
            if (key.isEmpty() || data.isEmpty()) {
                Log.e(TAG, "Key or data is empty");
                return "";
            }
            
            Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSHA512.init(secretKey);
            
            // Use UTF-8 encoding for data bytes - CRITICAL: Raw bytes, no encoding
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = hmacSHA512.doFinal(dataBytes);
            
            // Convert to hexadecimal string (lowercase, exactly as VNPay expects)
            StringBuilder hexString = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                // Convert byte to hex (0-255) and pad with leading zero if needed
                int value = b & 0xFF; // Ensure unsigned
                String hex = Integer.toHexString(value);
                if (hex.length() == 1) {
                    hexString.append('0'); // Pad with leading zero
                }
                hexString.append(hex);
            }
            
            String result = hexString.toString();
            Log.d(TAG, "HMAC SHA512 - Input length: " + data.length() + ", Output length: " + result.length());
            Log.d(TAG, "HMAC SHA512 - First 32 chars: " + (result.length() > 32 ? result.substring(0, 32) + "..." : result));
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error creating HMAC SHA512: " + e.getMessage(), e);
            e.printStackTrace();
            return "";
        }
    }
    
    // Exchange rate: 1 USD = ? VND
    // You can update this or fetch from API/database
    private static final double USD_TO_VND_RATE = 25000.0;
    
    /**
     * Convert USD to VND
     * @param usdAmount Amount in USD
     * @return Amount in VND (rounded to nearest integer)
     * Ví dụ: 10.50 USD = 262.500 VND (tỷ giá 25.000)
     */
    public static long convertToVND(double usdAmount) {
        return Math.round(usdAmount * USD_TO_VND_RATE);
    }
    
    /**
     * Format VND amount for display
     * @param vndAmount Amount in VND
     * @return Formatted string like "250,000 VND"
     */
    public static String formatVND(long vndAmount) {
        return String.format(Locale.getDefault(), "%,d VND", vndAmount);
    }
    
    /**
     * Get exchange rate
     * @return Current USD to VND exchange rate
     */
    public static double getExchangeRate() {
        return USD_TO_VND_RATE;
    }
    
    /**
     * Parse response URL from VNPay
     * IMPORTANT: VNPay calculates hash from URL-encoded values,
     * so we must NOT decode them for hash verification
     */
    public static Map<String, String> parseResponseUrl(String url) {
        Map<String, String> params = new HashMap<>();
        
        try {
            if (url == null || url.isEmpty()) {
                return params;
            }
            
            // Extract query string
            String query = url;
            
            // Handle different URL formats
            if (url.contains("?")) {
                query = url.substring(url.indexOf("?") + 1);
            } else if (url.contains("#")) {
                // Handle fragment
                String fragment = url.substring(url.indexOf("#") + 1);
                if (fragment.contains("?")) {
                    query = fragment.substring(fragment.indexOf("?") + 1);
                } else {
                    query = fragment;
                }
            }
            
            // Parse parameters - KEEP URL-encoded values for hash verification
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                if (pair.isEmpty()) continue;
                
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    // DO NOT decode - keep original URL-encoded values
                    String key = keyValue[0];
                    String value = keyValue[1];
                    params.put(key, value);
                } else if (keyValue.length == 1 && !keyValue[0].isEmpty()) {
                    // Parameter without value
                    String key = keyValue[0];
                    params.put(key, "");
                }
            }
            
            Log.d(TAG, "Parsed " + params.size() + " parameters from response URL");
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response URL: " + e.getMessage(), e);
        }
        
        return params;
    }
}

