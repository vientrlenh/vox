package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateExamRequest(
    @NotBlank(message = "Mã bài kiểm tra là bắt buộc")
    String code,

    @NotBlank(message = "Tên bài kiểm tra là bắt buộc")
    String name,

    String description,

    @NotNull(message = "Ngôn ngữ là bắt buộc")
    UUID languageId,

    UUID blueprintId,

    @NotBlank(message = "Thời gian mở bài là bắt buộc")
    String openAt,

    @NotBlank(message = "Thời gian đóng bài là bắt buộc")
    String closeAt,

    UUID assessmentPolicyId,

    @Min(value = 1, message = "Số lượt thi tối đa phải lớn hơn 0")
    Integer maxAttempt,

    @Min(value = 1, message = "Thời lượng bài thi phải lớn hơn 0 giây")
    Integer examTimeDurationSecond,

    ResultDecisionMethod resultDecisionMethod,

    Boolean requiresOtp, 

    List<String> requiredStreamTypes,

    String streamTypePermission,

    /** Thiết bị làm bài: LAB = thiết bị nhà trường, STUDENT_DEVICE = thiết bị thí sinh. Bỏ trống = LAB. */
    @Pattern(regexp = "STUDENT_DEVICE|LAB", message = "Hình thức làm bài không hợp lệ")
    String deliveryMode,
    /**
     * Ngưỡng tin cậy AI theo PHẦN TRĂM (0-100), nhà trường tự đặt. Bỏ trống = không đặt, hệ thống dùng luật
     * ngưỡng cứng như trước.
     *
     * <p>Đặt rồi thì bản chấm nào có overall_confidence thấp hơn ngưỡng sẽ sang PENDING_REVIEW,
     * và các luật cứng nội bộ bị bỏ qua -- xem RecordExamAttemptEvaluationUseCase.
     */
    @DecimalMin(value = "0.0", message = "Ngưỡng tin cậy phải từ 0 đến 100")
    @DecimalMax(value = "100.0", message = "Ngưỡng tin cậy phải từ 0 đến 100")
    @Digits(integer = 3, fraction = 2, message = "Ngưỡng tin cậy tối đa 2 chữ số thập phân")
    BigDecimal aiConfidenceThresholdPercent
) {
}
