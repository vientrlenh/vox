package com.sep.vox.domain.valueobject;

import java.math.BigDecimal;

public record LevelDifficulty(BigDecimal value) {
    public LevelDifficulty {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Độ khó của cấp độ không được để trống hoặc nhỏ hơn 0");
        }
    }
}
