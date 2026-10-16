package com.sep.vox.domain.valueobject;

public record PositiveInteger(
    Integer value
) {
    public PositiveInteger {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("Number value cannot be lower than 0");
        }
    }

    public static PositiveInteger from(Integer value) {
        return value == null ? null : new PositiveInteger(value);
    }

    public static Integer valueOf(PositiveInteger integer) {
        return integer == null ? null : integer.value;
    }
}
