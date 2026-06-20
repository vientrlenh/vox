package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record LevelCode(String value) {
    private static final Pattern UPPERCASE_CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]+$");
    public LevelCode {
        if (value != null && !UPPERCASE_CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Mã cấp độ không hợp lệ");
        }
    }
}
