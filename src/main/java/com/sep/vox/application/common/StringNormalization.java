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

    public static String normalizeSchoolCode(String input) {
        if (input == null) {
            return null;
        }
        return input.strip().toUpperCase(Locale.ROOT);
    }
}
