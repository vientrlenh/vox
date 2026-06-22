package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record IdentityNumber(String value) {

    private static final Pattern VIETNAMESE_IDENTITY_NUMBER_PATTERN = Pattern.compile("^(\\d{9}|\\d{12})$");

    public IdentityNumber {
        if (value != null && !VIETNAMESE_IDENTITY_NUMBER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Định dạng mã định danh không hợp lệ");
        }
    }
}
