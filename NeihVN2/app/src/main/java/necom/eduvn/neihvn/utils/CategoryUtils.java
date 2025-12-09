package necom.eduvn.neihvn.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CategoryUtils {

    private static final LinkedHashMap<String, String> CATEGORY_MAP = new LinkedHashMap<>();

    static {
        CATEGORY_MAP.put("Main", "Món chính");
        CATEGORY_MAP.put("Drink", "Đồ uống");
        CATEGORY_MAP.put("Dessert", "Tráng miệng");
    }

    private CategoryUtils() {
        // Utility class
    }

    public static String getDisplayName(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "";
        }
        for (Map.Entry<String, String> entry : CATEGORY_MAP.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(category)) {
                return entry.getValue();
            }
        }
        return category;
    }

    public static String getCanonicalCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        for (Map.Entry<String, String> entry : CATEGORY_MAP.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(value)) {
                return entry.getKey();
            }
            if (entry.getValue().equalsIgnoreCase(value)) {
                return entry.getKey();
            }
        }
        return value.trim();
    }

    public static List<String> getCanonicalCodes() {
        return new ArrayList<>(CATEGORY_MAP.keySet());
    }

    public static List<String> getDisplayNames(List<String> categories) {
        List<String> displayNames = new ArrayList<>();
        if (categories == null) {
            return displayNames;
        }
        for (String category : categories) {
            displayNames.add(getDisplayName(category));
        }
        return displayNames;
    }

}

