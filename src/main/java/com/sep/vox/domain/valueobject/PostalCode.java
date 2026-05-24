package com.sep.vox.domain.valueobject;

public record PostalCode(
    String value
) {

    private static final String POSTAL_CODE_PATTERN = "^[A-Za-z0-9][A-Za-z0-9 -]{1,18}[A-Za-z0-9]$";

    public PostalCode {
        if (value != null && !value.matches(POSTAL_CODE_PATTERN)) {
            throw new IllegalArgumentException("Định dạng mã bưu chính không hợp lệ");
        }
    }
}
