package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record ClassCode(String value) {
    private static final Pattern UPPERCASE_CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]+$");
    public ClassCode {
        if (value != null && !UPPERCASE_CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Mã lớp học không hợp lệ");
        }
    }
}
