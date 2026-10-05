package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record Code(
    String value
) {

    private static final Pattern UPPERCASE_CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]+$");

    public Code {
        if (value != null && !UPPERCASE_CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Mã lớp học không hợp lệ");
        }
    }

    public static Code from(String code) {
        return code == null ? null : new Code(code);
    }

    public static String valueOf(Code code) {
        return code == null ? null : code.value;
    }
}
