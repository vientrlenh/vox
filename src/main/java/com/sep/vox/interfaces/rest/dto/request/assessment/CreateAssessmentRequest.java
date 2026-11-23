package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.assessment.CreateAssessmentCommand;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAssessmentRequest(
    @NotBlank(message = "Mã bài kiểm tra là bắt buộc")
    String code,

    @NotBlank(message = "Tên bài kiểm tra là bắt buộc")
    String name,

    String description,

    @NotNull(message = "Ngôn ngữ là bắt buộc")
    UUID languageId,

    UUID blueprintId,

    @NotNull(message = "Thời gian mở bài là bắt buộc")
    Instant openAt,

    @NotNull(message = "Thời gian đóng bài là bắt buộc")
    Instant closeAt,

    UUID assessmentPolicyId,

    @Min(value = 1, message = "Số lượt thi tối đa phải lớn hơn 0")
    Integer maxAttempt,

    @Min(value = 1, message = "Thời lượng bài thi phải lớn hơn 0 giây")
    Integer assessmentTimeDurationSecond,

    ResultDecisionMethod resultDecisionMethod,

    Boolean requiresOtp, 

    List<String> requiredStreamTypes,

    AssessmentStreamTypePermission streamTypePermission,

    /** Test-taking device: LAB = school device, STUDENT_DEVICE = examinee device. Empty = LAB. */
    AssessmentDeliveryMode deliveryMode,
    /**
     * The AI confidence threshold as a PERCENTAGE (0-100), set by the school itself. Empty = not set, the system uses the hard
     * threshold rules as before.
     *
     * <p>Once set, any grading result whose overall_confidence is below the threshold goes to PENDING_REVIEW,
     * and the internal hard rules are ignored -- see RecordAssessmentAttemptEvaluationUseCase.
     */
    @DecimalMin(value = "0.0", message = "Ngưỡng tin cậy phải từ 0 đến 100")
    @DecimalMax(value = "100.0", message = "Ngưỡng tin cậy phải từ 0 đến 100")
    @Digits(integer = 3, fraction = 2, message = "Ngưỡng tin cậy tối đa 2 chữ số thập phân")
    BigDecimal aiConfidenceThresholdPercent
) {

    public static CreateAssessmentCommand toCommand(CreateAssessmentRequest request) {
        return new CreateAssessmentCommand(
            request.code(),
            request.name(),
            request.description(),
            request.languageId(),
            request.blueprintId(),
            request.openAt(),
            request.closeAt(),
            request.assessmentPolicyId(),
            request.maxAttempt(),
            request.assessmentTimeDurationSecond(),
            request.resultDecisionMethod(),
            request.requiresOtp(), 
            request.requiredStreamTypes(),
            request.streamTypePermission(),
            request.deliveryMode(),
            request.aiConfidenceThresholdPercent()
        );
    }
}
