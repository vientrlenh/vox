package com.sep.vox.domain.valueobject;

public record IdentityNumber(String value) {

    private static final String VIETNAMESE_IDENTITY_NUMBER_PATTERN = "^(\\d{9}|\\d{12})$";

    public IdentityNumber {
        if (value != null && !value.matches(VIETNAMESE_IDENTITY_NUMBER_PATTERN)) {
            throw new IllegalArgumentException("Định dạng mã định danh không hợp lệ");
        }
    }
}
