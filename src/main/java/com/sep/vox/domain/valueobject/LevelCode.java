package com.sep.vox.domain.valueobject;

public record LevelCode(String value) {
    private static final String UPPERCASE_CODE_PATTERN = "^[A-Z0-9_-]+$";
    public LevelCode {
        if (value != null && !value.matches(UPPERCASE_CODE_PATTERN)) {
            throw new IllegalArgumentException("Mã cấp độ không hợp lệ");
        }
    }
}
