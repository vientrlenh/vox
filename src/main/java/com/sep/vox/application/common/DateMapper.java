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

        // 1. Date-only format FE đã chốt: dd.MM.yyyy
        // Ví dụ: 07.04.2026 -> 2026-04-07T00:00:00+07:00
        try {
            DateTimeFormatter dotDateFormatter =
                    DateTimeFormatter.ofPattern("dd.MM.yyyy");

            LocalDate localDate = LocalDate.parse(fixedString, dotDateFormatter);

            return localDate.atStartOfDay().atOffset(DEFAULT_ZONE_OFFSET);
        } catch (DateTimeParseException ignored) {
            // Không phải dd.MM.yyyy thì xử lý tiếp các format cũ.
        }

        // 2. Trường hợp có kèm giờ phút theo ISO
        // Ví dụ: 2026-06-09T09:30, 2026-06-09T09:30:00,
        //        2026-06-09T09:30:00+07:00, 2026-06-09T02:30:00Z
        if (fixedString.contains("T")) {
            try {
                if (fixedString.length() == 13) {
                    fixedString += ":00:00+07:00";
                } else if (fixedString.length() == 16) {
                    fixedString += ":00+07:00";
                } else if (fixedString.length() == 19) {
                    fixedString += "+07:00";
                }

                return OffsetDateTime.parse(fixedString);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Định dạng thời gian không hợp lệ. "
                                + "Dùng dd.MM.yyyy hoặc ISO datetime."
                );
            }
        }

        // 3. Hỗ trợ các format cũ đang có trong toLocalDate()
        try {
            LocalDate localDate = toLocalDate(fixedString);

            return localDate.atStartOfDay().atOffset(DEFAULT_ZONE_OFFSET);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Định dạng ngày không hợp lệ. "
                            + "Khuyên dùng: dd.MM.yyyy, ví dụ 07.04.2026."
            );
        }
    }
}
