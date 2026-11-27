package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.assessment.CreateClassTestCommand;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClassTestRequest(
    @NotNull(message = "Class is required")
    UUID schoolClassId,

    @NotBlank(message = "Class test name is required")
    String name,

    String description,

    @NotNull(message = "Opening time is required")
    Instant openAt,

    @NotNull(message = "Closing time is required")
    Instant closeAt,

    @NotNull(message = "Evaluation rubric is required")
    UUID assessmentPolicyId,

    @Min(value = 1, message = "Maximum number of attempts must be greater than 0")
    Integer maxAttempt,

    @Min(value = 1, message = "Assessment duration must be greater than 0 seconds")
    Integer assessmentTimeDurationSecond,

    ResultDecisionMethod resultDecisionMethod,

    /** Monitoring stream types: CAMERA and/or SCREEN. Empty = no stream monitoring. */
    List<String> requiredStreamTypes,

    /** Stream choice rule, only applies when both types are required: ALL (both required) or ANY. */
    AssessmentStreamTypePermission streamTypePermission,

    /** Test-taking device: LAB = school device, STUDENT_DEVICE = student device. Empty = STUDENT_DEVICE. */
    AssessmentDeliveryMode deliveryMode,

    /** Makes the student enter the exam slot OTP when entering the test. Empty = not required. */
    Boolean requiresOtp,

    /** Exam room. May be left empty at creation and chosen later, but must be set before scheduling. */
    UUID schoolRoomId,
    /**
     * The AI confidence threshold as a PERCENTAGE (0-100), set by the school itself. Empty = not set, the system uses the hard
     * threshold rules as before.
     *
     * <p>Once set, any grading result whose overall_confidence is below the threshold goes to PENDING_REVIEW,
     * and the internal hard rules are ignored -- see RecordAssessmentAttemptEvaluationUseCase.
     */
    @DecimalMin(value = "0.0", message = "Confidence threshold must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Confidence threshold must be between 0 and 100")
    @Digits(integer = 3, fraction = 2, message = "Confidence threshold must have at most 2 decimal places")
    BigDecimal aiConfidenceThresholdPercent
) {

    public static CreateClassTestCommand toCommand(CreateClassTestRequest request) {
        return new CreateClassTestCommand(
            request.schoolClassId(),
            request.name(),
            request.description(),
            request.openAt(),
            request.closeAt(),
            request.assessmentPolicyId(),
            request.maxAttempt(),
            request.assessmentTimeDurationSecond(),
            request.resultDecisionMethod(),
            request.requiredStreamTypes(),
            request.streamTypePermission(),
            request.deliveryMode(),
            request.requiresOtp(),
            request.schoolRoomId()
,
            request.aiConfidenceThresholdPercent()
        );
    }
}
