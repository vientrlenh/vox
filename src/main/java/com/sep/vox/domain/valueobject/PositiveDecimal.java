package com.sep.vox.domain.valueobject;

import java.math.BigDecimal;

public record PositiveDecimal(
    BigDecimal value
) {
    public PositiveDecimal {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Decimal value cannot be lower than 0.0");
        }
    }

    public static PositiveDecimal from(BigDecimal value) {
        return value == null ? null : new PositiveDecimal(value);
    }

    public static BigDecimal valueOf(PositiveDecimal decimal) {
        return decimal == null ? null : decimal.value;
    }
}
