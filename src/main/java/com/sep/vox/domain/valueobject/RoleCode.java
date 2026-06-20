package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record RoleCode(String value) {

    private static final Pattern UPPERCASE_SNAKE_CASE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*$");

    public RoleCode {
        if (value != null && !UPPERCASE_SNAKE_CASE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Định dạng mã vai trò không hợp lệ");
        }
    }
}
