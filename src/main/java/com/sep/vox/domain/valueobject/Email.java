package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record Email(
    String value
) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");
    
    public Email {
        if (value != null && !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address");
        }
    }

    public static Email from(String email) {
        return email == null ? null : new Email(email);
    }

    public static String valueOf(Email email) {
        return email == null ? null : email.value;
    }
}
