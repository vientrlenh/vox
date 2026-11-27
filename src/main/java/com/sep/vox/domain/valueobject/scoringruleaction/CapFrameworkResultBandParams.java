package com.sep.vox.domain.valueobject.scoringruleaction;

import java.util.UUID;

public record CapFrameworkResultBandParams(
    UUID maxResultBandId
) implements ScoringRuleActionParams {
    public CapFrameworkResultBandParams {
        if (maxResultBandId == null) {
            throw new IllegalArgumentException("The id of the maximum framework result band must not be blank");
        }
    }
}
