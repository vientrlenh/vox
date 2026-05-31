package com.sep.vox.domain.valueobject;

public record LevelOrder(int value) {
    public LevelOrder {
        if (value <= 0) {
            throw new IllegalArgumentException("Thứ tự của cấp độ không được nhỏ hơn hoặc bằng 0");
        }
    }
}
