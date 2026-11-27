package com.sep.vox.domain.valueobject.scoringruleaction;

import java.util.UUID;

// affects the achieved framework band, and may override the computed band (same as CapFrameworkResultBandParams)
public record SetFrameworkResultBandParams(
    UUID frameworkResultBandId
) implements ScoringRuleActionParams {
    public SetFrameworkResultBandParams {
        if (frameworkResultBandId == null) {
            throw new IllegalArgumentException("The id of the framework result band must not be blank");
        }
    }
}
