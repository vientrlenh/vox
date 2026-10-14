package com.sep.vox.domain.valueobject;

public record PositiveLong(
    Long value
) {
    public PositiveLong {
        if (value != null && value < 0L) {
            throw new IllegalArgumentException("Long value must not be lower than 0");
        }
    }

    public static PositiveLong from(Long value) {
        return value == null ? null : new PositiveLong(value);
    }

    public static Long valueOf(PositiveLong pLong) {
        return pLong == null ? null : pLong.value;
    }
}
