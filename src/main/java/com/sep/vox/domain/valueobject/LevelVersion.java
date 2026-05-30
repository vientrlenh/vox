package com.sep.vox.domain.valueobject;

public record LevelVersion(int value) {
    public LevelVersion {
        if (value <= 0) {
            throw new IllegalArgumentException("Phiên bản của cấp độ không được phép nhỏ hơn hoặc bằng 0");
        }
    }
}
