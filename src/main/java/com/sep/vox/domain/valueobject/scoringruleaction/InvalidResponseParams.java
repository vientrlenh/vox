package com.sep.vox.domain.valueobject.scoringruleaction;

public record InvalidResponseParams(
    String reasonCode,
    String message
) implements ScoringRuleActionParams {
    public InvalidResponseParams {
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("Reason code không được để trống");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Thông điệp không được để trống");
        }
    }
}
