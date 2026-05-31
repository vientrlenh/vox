package com.sep.vox.domain.valueobject.scoringruleaction;

public record FeedbackTagParams(
    String tagCode,
    String message
) implements ScoringRuleActionParams {
    public FeedbackTagParams {
        if (tagCode == null || tagCode.isBlank()) {
            throw new IllegalArgumentException("Tag code không được để trống");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Thông điệp không được để trống");
        }
    }
}
