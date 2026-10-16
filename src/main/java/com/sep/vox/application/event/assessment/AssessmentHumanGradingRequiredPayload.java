package com.sep.vox.application.event.assessment;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

public record AssessmentHumanGradingRequiredPayload(
    List<UUID> recipientIds,
    UUID assessmentId,
    String assessmentName,
    AssessmentKind assessmentKind,
    int pendingCount
) {

}
