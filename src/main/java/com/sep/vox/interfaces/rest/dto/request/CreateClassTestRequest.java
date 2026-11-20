package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    @NotNull(message = "Lớp học là bắt buộc")
    UUID schoolClassId,

    @NotBlank(message = "Tên bài kiểm tra là bắt buộc")
    String name,

    String description,

    @NotNull(message = "Thời gian mở bài là bắt buộc")
    Instant openAt,

    @NotNull(message = "Thời gian đóng bài là bắt buộc")
    Instant closeAt,

    @NotNull(message = "Bộ tiêu chí đánh giá là bắt buộc")
    UUID assessmentPolicyId,

    @Min(value = 1, message = "Số lượt thi tối đa phải lớn hơn 0")
    Integer maxAttempt,

    @Min(value = 1, message = "Thời lượng bài thi phải lớn hơn 0 giây")
    Integer assessmentTimeDurationSecond,

    ResultDecisionMethod resultDecisionMethod,

    /** Các loại stream giám sát: CAMERA và/hoặc SCREEN. Bỏ trống = không giám sát bằng stream. */
    List<String> requiredStreamTypes,

    /** Quyền chọn stream, chỉ áp dụng khi yêu cầu cả hai loại: ALL (bắt buộc cả hai) hoặc ANY. */
    AssessmentStreamTypePermission streamTypePermission,

    /** Thiết bị làm bài: LAB = thiết bị nhà trường, STUDENT_DEVICE = thiết bị học sinh. Bỏ trống = STUDENT_DEVICE. */
    AssessmentDeliveryMode deliveryMode,

    /** Bắt học sinh nhập OTP của ca thi khi vào bài. Bỏ trống = không yêu cầu. */
    Boolean requiresOtp,

    /** Phòng thi. Có thể bỏ trống lúc tạo và chọn sau, nhưng phải có trước khi lên lịch. */
    UUID schoolRoomId,
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
}
