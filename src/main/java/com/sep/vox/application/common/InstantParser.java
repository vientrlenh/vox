package com.sep.vox.application.common;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Parse mốc thời gian ISO-8601 từ payload.
 *
 * <p>Gọi thẳng {@code Instant.parse} thì chuỗi sai định dạng ném {@link DateTimeParseException} —
 * lớp này kế thừa {@code DateTimeException} chứ KHÔNG phải {@code IllegalArgumentException}, nên
 * {@code GlobalExceptionHandler} không bắt được và client nhận 500 "Có lỗi xảy ra" thay vì 400 kèm
 * lý do. Đổi sang {@code IllegalArgumentException} với message tiếng Việt để đi đúng nhánh 400.
 *
 * <p>Chuỗi rỗng/toàn khoảng trắng coi như không gửi ({@code null}), giống {@code null}: FE bỏ trống ô
 * datetime gửi lên chuỗi rỗng chứ không bỏ hẳn field.
 */
public final class InstantParser {

    private InstantParser() {
    }

    /** {@code null} nếu không có giá trị; ném {@code IllegalArgumentException} nếu sai định dạng. */
    public static Instant parseOrNull(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                fieldLabel + " không đúng định dạng thời gian ISO-8601 (ví dụ: 2026-08-10T08:00:00Z)");
        }
    }
}
