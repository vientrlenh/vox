package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.sep.vox.domain.model.assessment.GradingRoundType;

public record ClaimClassTestGradingRequest(
    @NotNull(message = "Vòng chấm là bắt buộc")
    GradingRoundType roundType,

    @NotEmpty(message = "Phải chọn ít nhất một bài để nhận chấm")
    List<UUID> candidateResultIds
) {
}
