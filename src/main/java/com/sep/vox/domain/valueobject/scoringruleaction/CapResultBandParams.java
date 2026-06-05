package com.sep.vox.domain.valueobject.scoringruleaction;

import java.util.UUID;

public record CapResultBandParams(
    UUID maxResultBandId
) implements ScoringRuleActionParams {
    public CapResultBandParams {
        if (maxResultBandId == null) {
            throw new IllegalArgumentException("ID của khung chuẩn kết quả band tối đa không được để trống");
        }
    }
}
