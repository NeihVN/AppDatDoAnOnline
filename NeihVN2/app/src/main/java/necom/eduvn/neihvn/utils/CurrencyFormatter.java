package necom.eduvn.neihvn.utils;

import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyFormatter {

    private static final Locale VIETNAM_LOCALE = new Locale("vi", "VN");

    private CurrencyFormatter() {
        // Utility class
    }

    public static String format(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(VIETNAM_LOCALE);
        return formatter.format(amount);
    }
}

