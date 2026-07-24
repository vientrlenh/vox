package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

import jakarta.validation.constraints.Min;

public record UpdateExamRequest(
    String name,
    String description,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,

    @Min(value = 1, message = "Số lượt thi tối đa phải lớn hơn 0")
    Integer maxAttempt,

    @Min(value = 1, message = "Thời lượng bài thi phải lớn hơn 0 giây")
    Integer examTimeDurationSecond,

    ResultDecisionMethod resultDecisionMethod,

    Boolean requiresOtp
) {
}
