package com.sep.vox.domain.common;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Bỏ dấu tiếng Việt cho từ khoá tìm kiếm, phía Java.
 *
 * <p>Phải cho ra ĐÚNG kết quả như hàm SQL {@code vn_search_key} và như
 * {@code toVnSearchKey} phía frontend: từ khoá được chuẩn hoá ở đây rồi so với cột đã chuẩn hoá
 * bằng hàm SQL kia, lệch thuật toán là tìm không ra.
 *
 * <p>NFD tách nguyên âm có dấu thành "chữ gốc + dấu tổ hợp" (U+0300–U+036F) để bỏ dấu đi, NHƯNG
 * chữ "đ" là một CHỮ CÁI riêng (U+0111) chứ không phải "d" cộng dấu nên NFD không đụng tới.
 */
public final class VnSearchKey {

    private VnSearchKey() {
    }

    public static String of(String value) {
        if (value == null) {
            return null;
        }
        var decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return decomposed
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toLowerCase(Locale.ROOT)
            .strip();
    }
}
