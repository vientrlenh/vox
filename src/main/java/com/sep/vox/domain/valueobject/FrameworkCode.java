package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record FrameworkCode(String value) {
    private static final Pattern UPPERCASE_CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]+$");
    public FrameworkCode {
        if (value != null && !UPPERCASE_CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("The framework of the level is invalid");
        }
    }
}
