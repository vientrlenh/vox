package com.sep.vox.domain.valueobject.scoringruleaction;

public record FeedbackTagParams(
    String tagCode,
    String message
) implements ScoringRuleActionParams {
    public FeedbackTagParams {
        if (tagCode == null || tagCode.isBlank()) {
            throw new IllegalArgumentException("The tag code must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("The message must not be blank");
        }
    }
}
