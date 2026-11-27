package com.sep.vox.application.port.input.command.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;

public record UpdateAssessmentCommand(
    UUID assessmentId,
    String name,
    String description,
    Instant openAt,
    Instant closeAt,
    UUID assessmentPolicyId,
    Integer maxAttempt,
    Integer assessmentTimeDurationSecond,
    ResultDecisionMethod resultDecisionMethod,
    Boolean requiresOtp,
    /** null = keep the proctoring configuration; an empty list = turn proctoring off. */
    List<String> requiredStreamTypes,
    AssessmentStreamTypePermission streamTypePermission,

    /**
     * The AI confidence threshold as a PERCENTAGE (0-100). null = keep it, exactly the patch semantics of every other
     * field in this command.
     */
    BigDecimal aiConfidenceThresholdPercent
) {
}
