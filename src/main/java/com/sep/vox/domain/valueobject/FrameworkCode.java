package com.sep.vox.domain.valueobject;

public record FrameworkCode(String value) {
    private static final String UPPERCASE_CODE_PATTERN = "^[A-Z0-9_-]+$";
    public FrameworkCode {
        if (value != null && !value.matches(UPPERCASE_CODE_PATTERN)) {
            throw new IllegalArgumentException("Framework của cấp độ không hợp lệ");
        }
    }
}
