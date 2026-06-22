package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record PostalCode(
    String value
) {

    private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 -]{1,18}[A-Za-z0-9]$");

    public PostalCode {
        if (value != null && !POSTAL_CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Định dạng mã bưu chính không hợp lệ");
        }
    }
}
