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
        String text = trimAndCollapseSpaces(input);
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
     * Builds the "%keyword%" pattern (already lowercased) in Java instead of using JPQL CONCAT(),
     * because Hibernate translates CONCAT() to PostgreSQL's `||` operator and lets JDBC infer the
     * parameter type - when the value/context is ambiguous, PostgreSQL may wrongly infer the
     * parameter as bytea, making LOWER(...) fail with "function lower(bytea) does not exist".
     * Building the string beforehand and binding it straight into LIKE :pattern avoids that type
     * inference error entirely.
     */
    public static String toLikePattern(String keyword) {
        String trimmed = trimAndCollapseSpaces(keyword);
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        return "%" + trimmed.toLowerCase(Locale.ROOT) + "%";
    }

}
