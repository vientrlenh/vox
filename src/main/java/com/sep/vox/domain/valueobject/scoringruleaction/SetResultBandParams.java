package com.sep.vox.domain.valueobject.scoringruleaction;

import java.util.UUID;

public record SetResultBandParams(
    UUID frameworkResultBandId
) implements ScoringRuleActionParams {
    public SetResultBandParams {
        if (frameworkResultBandId == null) {
            throw new IllegalArgumentException("ID của chuẩn khung kết quả band không được để trống");
        }
    }
}
