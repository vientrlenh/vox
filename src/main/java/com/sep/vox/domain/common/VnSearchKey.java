package com.sep.vox.domain.common;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Strips Vietnamese diacritics from a search keyword, on the Java side.
 *
 * <p>Must produce EXACTLY the same result as the SQL function {@code vn_search_key} and the frontend
 * {@code toVnSearchKey}: the keyword is normalised here and then compared with the column normalised by that SQL
 * function, so a mismatch in the algorithm means nothing is found.
 *
 * <p>NFD splits an accented vowel into "base letter + combining mark" (U+0300–U+036F) so the mark can be removed,
 * BUT the letter "đ" is a separate LETTER (U+0111), not "d" plus a mark, so NFD does not touch it.
 */
public final class VnSearchKey {

    private VnSearchKey() {
    }

    public static String of(String value) {
        if (value == null) {
            return null;
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return decomposed
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toLowerCase(Locale.ROOT)
            .strip();
    }
}
