package com.sep.vox.domain.valueobject;

import java.util.regex.Pattern;

public record Phone(String value) {

    private static final Pattern VIETNAM_PHONE_PATTERN = Pattern.compile("((^(\\+84|84|0|0084){1})(3|5|7|8|9))+([0-9]{8})$");
    
    public Phone {
        if (value != null && !VIETNAM_PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Định dạng số điện thoại không hợp lệ");
        }
    }
}
