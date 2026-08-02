package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClassTestRequest(
    @NotNull(message = "Lớp học là bắt buộc")
    UUID schoolClassId,

    @NotBlank(message = "Tên bài kiểm tra là bắt buộc")
    String name,

    String description,

    @NotBlank(message = "Thời gian mở bài là bắt buộc")
    String openAt,

    @NotBlank(message = "Thời gian đóng bài là bắt buộc")
    String closeAt,

    @NotNull(message = "Bộ tiêu chí đánh giá là bắt buộc")
    UUID assessmentPolicyId,

    @Valid
    List<ClassTestSectionRequest> sections,

    UUID existingBlueprintId,
    UUID existingBlueprintVersionId,

    @Min(value = 1, message = "Số lượt thi tối đa phải lớn hơn 0")
    Integer maxAttempt,

    @Min(value = 1, message = "Thời lượng bài thi phải lớn hơn 0 giây")
    Integer examTimeDurationSecond,

    ResultDecisionMethod resultDecisionMethod
) {
}
