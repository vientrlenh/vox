package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

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
    String openAt,
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
    String deliveryMode
) {
}
