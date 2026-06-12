package com.sep.vox.domain.valueobject.scoringruleaction;

import java.util.UUID;

// tác động lên band khung đạt được, và được phép ghi đè band đã tính (tương tự với CapFrameworkResultBandParams)
public record SetFrameworkResultBandParams(
    UUID frameworkResultBandId
) implements ScoringRuleActionParams {
    public SetFrameworkResultBandParams {
        if (frameworkResultBandId == null) {
            throw new IllegalArgumentException("ID của chuẩn khung kết quả band không được để trống");
        }
    }
}
