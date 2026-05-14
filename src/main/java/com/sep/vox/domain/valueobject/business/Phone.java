package com.sep.vox.domain.valueobject.business;

public record Phone(String value) {
    
    public Phone {
        if (value != null && !value.matches("((^(\\+84|84|0|0084){1})(3|5|7|8|9))+([0-9]{8})$")) {
            throw new IllegalArgumentException("Định dạng số điện thoại không hợp lệ");
        }
    }
}
