package com.sep.vox.domain.valueobject;

public record SchoolDomain(String value) {

    private static final String EDUCATION_DOMAIN_PATTERN = "^[a-zA-Z0-9.-]+\\.edu\\.vn$";

    public SchoolDomain {
        if (value != null && !value.matches(EDUCATION_DOMAIN_PATTERN)) {
            throw new IllegalArgumentException("Định dạng tên miền trường không hợp lệ");
        }
    }
}
