package com.sep.vox.domain.valueobject.scoringruleaction;

import java.util.UUID;

public record SetStandardLevelParams(
    UUID standardLevelVersionId
) implements ScoringRuleActionParams {
    public SetStandardLevelParams {
        if (standardLevelVersionId == null) {
            throw new IllegalArgumentException("ID của phiên bản tiêu chuẩn cấp độ không được để trống");
        }
    }
}
