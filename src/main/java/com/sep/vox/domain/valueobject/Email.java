package com.sep.vox.domain.valueobject;

public record Email(String value) {
    
    public Email {
        if (value != null && !value.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ");
        }
    }
}
