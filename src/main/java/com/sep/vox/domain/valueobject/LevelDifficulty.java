package com.sep.vox.domain.valueobject;

import java.math.BigDecimal;

public record LevelDifficulty(BigDecimal value) {
    public LevelDifficulty {
        if (value.doubleValue() < 0) {
            throw new IllegalArgumentException("Độ khó của cấp độ không được nhỏ hơn 0");
        }
    }
}
