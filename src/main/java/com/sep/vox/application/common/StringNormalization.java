package com.sep.vox.application.common;

import java.text.Normalizer;
import java.util.Locale;

public final class StringNormalization {
    
    public static String trimAndCollapseSpaces(String input) {
        if (input == null) {
            return null;
        }
        return input.strip().replaceAll("\\s+", " ");
    }

    public static String normalizeEmail(String input) {
        if (input == null) {
            return null;
        }
        return input.strip().toLowerCase(Locale.ROOT);
    }

    public static String normalizeDomain(String input) {
        if (input == null) {
            return null;
        }
        return input.strip().toLowerCase(Locale.ROOT);
    }

    public static String normalizePhone(String input) {
        if (input == null) {
            return null;
        }
        return input.strip().replaceAll("[\\s.-]", "");
    }

    public static String normalizeIdentityNumber(String input) {
        if (input == null) {
            return null;
        }
        return input.strip().replaceAll("\\s+", "");
    }

    public static String normalizeSearchText(String input) {
        if (input == null) {
            return null;
        }
        var text = trimAndCollapseSpaces(input);
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        return text.replaceAll("\\p{M}", "");
    }

    public static String normalizeCode(String input) {
        if (input == null) {
            return null;
        }
        return input.strip().toUpperCase(Locale.ROOT);
    }

    /**
     * Tạo sẵn pattern "%keyword%" (đã lowercase) ở tầng Java thay vì dùng JPQL CONCAT(), vì Hibernate dịch
     * CONCAT() sang toán tử `||` của PostgreSQL và để JDBC tự suy luận kiểu tham số - khi giá trị/ngữ cảnh mơ hồ,
     * PostgreSQL có thể suy ra nhầm tham số là bytea, khiến LOWER(...) báo lỗi "function lower(bytea) does not
     * exist". Build sẵn chuỗi này rồi bind thẳng vào LIKE :pattern tránh hẳn lỗi suy luận kiểu đó.
     */
    public static String toLikePattern(String keyword) {
        var trimmed = trimAndCollapseSpaces(keyword);
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        return "%" + trimmed.toLowerCase(Locale.ROOT) + "%";
    }

}
