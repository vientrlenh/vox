package com.sep.vox.domain.valueobject;

public record StudentCount(int value) {

    public StudentCount {
        if (value <= 0) {
            throw new IllegalArgumentException("Số lượng học sinh phải lớn hơn 0");
        }
    }
}
