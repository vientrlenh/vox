package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.ResultDecisionMethod;

public record CreateClassTestCommand(
    UUID schoolClassId,
    String name,
    String description,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    Integer maxAttempt,
    Integer assessmentTimeDurationSecond,
    ResultDecisionMethod resultDecisionMethod,
    List<String> requiredStreamTypes,
    String streamTypePermission,
    String deliveryMode,
    Boolean requiresOtp,
    UUID schoolRoomId,

    /**
     * AI confidence threshold (0.00-1.00). NULL = not set, the system uses the hard rules as before.
     *
     * <p>When a threshold is set, a grading result with a lower overall_confidence goes to PENDING_REVIEW, and the whole
     * internal threshold rule set is ignored -- see RecordAssessmentAttemptEvaluationUseCase.
     */
    java.math.BigDecimal aiConfidenceThresholdPercent
) {
}
