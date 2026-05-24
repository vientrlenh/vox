package com.sep.vox.domain.valueobject;

public record SchoolCode(String value) {

    private static final String UPPERCASE_CODE_PATTERN = "^[A-Z0-9_-]+$";

    public SchoolCode {
        if (value != null && !value.matches(UPPERCASE_CODE_PATTERN)) {
            throw new IllegalArgumentException("Định dạng mã trường không hợp lệ");
        }
    }
}
