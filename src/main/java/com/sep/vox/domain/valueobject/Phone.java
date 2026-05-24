package com.sep.vox.domain.valueobject;

public record Phone(String value) {

    private static final String VIETNAM_PHONE_PATTERN = "((^(\\+84|84|0|0084){1})(3|5|7|8|9))+([0-9]{8})$";
    
    public Phone {
        if (value != null && !value.matches(VIETNAM_PHONE_PATTERN)) {
            throw new IllegalArgumentException("Định dạng số điện thoại không hợp lệ");
        }
    }
}
