package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record SchoolDomain(String value) {

    private static final Pattern EDUCATION_DOMAIN_PATTERN = Pattern.compile("^([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+edu\\.vn$", Pattern.CASE_INSENSITIVE);

    public SchoolDomain {
        if (value != null && !EDUCATION_DOMAIN_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Định dạng tên miền trường không hợp lệ");
        }
    }
}
