package com.sep.vox.domain.valueobject.scoringruleaction;

import java.util.UUID;

public record CapStandardLevelParams(
    UUID maxStandardLevelVersionId
) implements ScoringRuleActionParams {
    public CapStandardLevelParams {
        if (maxStandardLevelVersionId == null) {
            throw new IllegalArgumentException("ID của phiên bản tiêu chuẩn cấp độ tối đa không được để trống");
        }
    }
}
