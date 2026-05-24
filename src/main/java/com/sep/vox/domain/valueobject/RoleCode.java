package com.sep.vox.domain.valueobject;

public record RoleCode(String value) {

    private static final String UPPERCASE_SNAKE_CASE_PATTERN = "^[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*$";

    public RoleCode {
        if (value != null && !value.matches(UPPERCASE_SNAKE_CASE_PATTERN)) {
            throw new IllegalArgumentException("Định dạng mã vai trò không hợp lệ");
        }
    }
}
