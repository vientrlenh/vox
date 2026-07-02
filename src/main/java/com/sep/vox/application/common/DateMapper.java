package com.sep.vox.application.common;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class DateMapper {
    
    private static final List<String> INPUT_LOCALDATE_FORMAT = List.of(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "MM-dd-yyyy"
    );

    private static final String OUTPUT_LOCALDATE_FORMAT = "dd-MM-yyyy";
    private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.ofHours(7);

    public static LocalDate toLocalDate(String localDateString) {
        for (var pattern : INPUT_LOCALDATE_FORMAT) {
            try {
                var formatter = DateTimeFormatter.ofPattern(pattern);
                var date = LocalDate.parse(localDateString, formatter);
                return date;
            } catch (DateTimeParseException e) {  
            }
        }
        throw new IllegalArgumentException("Định dạng ngày yêu cầu không hợp lệ");
    }

    public static String localDateToString(LocalDate date) {
        var formatter = DateTimeFormatter.ofPattern(OUTPUT_LOCALDATE_FORMAT);
        return date.format(formatter);
    }


    public static OffsetDateTime toOffsetDateTime(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }

        String fixedString = dateString.trim();

        // 1. Trường hợp có kèm theo Giờ Phút (Chuẩn ISO, có chứa ký tự 'T')
        // Ví dụ: 2026-06-09T09:30
        if (fixedString.contains("T")) {
            try {
                if (fixedString.length() == 13) fixedString += ":00:00+07:00";
                else if (fixedString.length() == 16) fixedString += ":00+07:00";
                else if (fixedString.length() == 19) fixedString += "+07:00";

                return OffsetDateTime.parse(fixedString);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Định dạng thời gian (có giờ phút) không hợp lệ.");
            }
        }

        // 2. Trường hợp gửi kiểu tự do chỉ có Ngày (Ví dụ: 10/06/2026, 10-06-2026, 2026-06-10)
        try {
            // Tận dụng hàm toLocalDate ở trên để parse ra ngày
            LocalDate localDate = toLocalDate(fixedString);

            // Ép thành giờ bắt đầu của ngày (00:00:00) và cộng thêm múi giờ VN (+07:00)
            return localDate.atStartOfDay().atOffset(DEFAULT_ZONE_OFFSET);
        } catch (IllegalArgumentException ex) {
            // Bắn lại lỗi từ toLocalDate ra cho Frontend
            throw new IllegalArgumentException("Định dạng thời gian không hợp lệ. Khuyên dùng: dd/MM/yyyy hoặc YYYY-MM-DDTHH:mm:ss");
        }
    }
}
