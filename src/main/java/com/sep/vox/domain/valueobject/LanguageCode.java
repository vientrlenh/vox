package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record LanguageCode(
    String value
) {
    private static final Pattern  UPPERCASE_CHARACTER_PATTERN = Pattern.compile("^[A-Z]+$");

    public LanguageCode {
        if (value != null && !UPPERCASE_CHARACTER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Mã ngôn ngữ không hợp lệ");
        }
    }
}
