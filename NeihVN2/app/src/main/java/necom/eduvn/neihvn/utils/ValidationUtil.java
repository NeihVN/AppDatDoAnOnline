package necom.eduvn.neihvn.utils;

import android.util.Patterns;

public class ValidationUtil {

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^[0-9]{10,11}$");
    }

    public static boolean isValidPrice(String price) {
        try {
            double value = Double.parseDouble(price);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidUrl(String url) {
        return url != null && Patterns.WEB_URL.matcher(url).matches();
    }
}