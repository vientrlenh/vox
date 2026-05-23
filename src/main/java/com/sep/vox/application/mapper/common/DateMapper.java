package com.sep.vox.application.mapper.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DateMapper {
    
    private static final List<String> INPUT_LOCALDATE_FORMAT = List.of(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "MM-dd-yyyy"
    );

    private static final String OUTPUT_LOCALDATE_FORMAT = "dd-MM-yyyy";

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



}
