package com.sep.vox.domain.valueobject.scoringruleaction;

public record RequireRetakeParams(
    String reasonCode,
    String message
) implements ScoringRuleActionParams {
    public RequireRetakeParams {
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("The reason code must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("The message must not be blank");
        }
    }
}
