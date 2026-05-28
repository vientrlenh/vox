package com.sep.vox.domain.valueobject;

public record ClassCode(String value) {
    private static final String UPPERCASE_CODE_PATTERN = "^[A-Z0-9_-]+$";
    public ClassCode {
        if (value != null && !value.matches(UPPERCASE_CODE_PATTERN)) {
            throw new IllegalArgumentException("Mã lớp học không hợp lệ");
        }
    }
}
