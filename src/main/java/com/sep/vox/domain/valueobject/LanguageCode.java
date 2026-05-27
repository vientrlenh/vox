package com.sep.vox.domain.valueobject;

public record LanguageCode(
    String value
) {
    private static final String UPPERCASE_CHARACTER_PATTERN = "^[A-Z]+$";

    public LanguageCode {
        if (value != null && !value.matches(UPPERCASE_CHARACTER_PATTERN)) {
            throw new IllegalArgumentException("Mã ngôn ngữ không hợp lệ");
        }
    }
}
