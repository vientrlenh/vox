package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentCommand;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record UpdateAssessmentRequest(
    String name,
    String description,
    Instant openAt,
    Instant closeAt,
    UUID assessmentPolicyId,

    @Min(value = 1, message = "Maximum number of attempts must be greater than 0")
    Integer maxAttempt,

    @Min(value = 1, message = "Assessment duration must be greater than 0 seconds")
    Integer assessmentTimeDurationSecond,

    ResultDecisionMethod resultDecisionMethod,

    Boolean requiresOtp,

    /**
     * Monitoring configuration, in the same raw form as when creating ({@code ["CAMERA","SCREEN"]}).
     *
     * <p>Empty (null) = keep the current configuration, like every other field of this API. An EMPTY
     * list is what means "turn monitoring off" -- when creating, null means that, but here null already means
     * "do not touch", so the two had to be separated.
     */
    List<String> requiredStreamTypes,

    AssessmentStreamTypePermission streamTypePermission,

    /**
     * AI confidence threshold as a PERCENTAGE (0-100). Empty = KEEP the current value, like every
     * other field of the update command -- so there is currently no way to clear a threshold back to "not set".
     */
    @DecimalMin(value = "0.0", message = "Confidence threshold must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Confidence threshold must be between 0 and 100")
    @Digits(integer = 3, fraction = 2, message = "Confidence threshold must have at most 2 decimal places")
    BigDecimal aiConfidenceThresholdPercent
) {

    public static UpdateAssessmentCommand toCommand(UUID assessmentId, UpdateAssessmentRequest request) {
        return new UpdateAssessmentCommand(
            assessmentId,
            request.name(),
            request.description(),
            request.openAt(),
            request.closeAt(),
            request.assessmentPolicyId(),
            request.maxAttempt(),
            request.assessmentTimeDurationSecond(),
            request.resultDecisionMethod(),
            request.requiresOtp(),
            request.requiredStreamTypes(),
            request.streamTypePermission()
,
            request.aiConfidenceThresholdPercent()
        );
    }
}
