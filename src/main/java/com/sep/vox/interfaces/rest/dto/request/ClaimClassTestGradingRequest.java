package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ClaimClassTestGradingRequest(
    @NotBlank(message = "Vòng chấm là bắt buộc")
    String roundType,

    @NotEmpty(message = "Phải chọn ít nhất một bài để nhận chấm")
    List<UUID> candidateResultIds
) {
}
