package com.sep.vox.domain.valueobject.scoringruleaction;

import java.math.BigDecimal;

public record FinalScoreDeltaParams(
    BigDecimal delta
) implements ScoringRuleActionParams {
    public FinalScoreDeltaParams {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Delta không được để trống hoặc bằng 0");
        }
    }
}
