package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateFrameworkCriterionBandsRequest(
        @NotEmpty(message = "Phải gửi lên ít nhất 1 mức đánh giá")
        @Valid
        List<CriterionBandItemRequest> bands
) {
    public record CriterionBandItemRequest(
            @NotBlank(message = "Mã kết quả (resultBandCode) không được để trống")
            String resultBandCode,

            String descriptor,

            @Valid
            List<SignalRequest> positiveSignals,

            @Valid
            List<SignalRequest> negativeSignals
    ) {}

    public record SignalRequest(
            @NotBlank(message = "Mã dấu hiệu không được để trống")
            String code,

            @NotBlank(message = "Mô tả dấu hiệu không được để trống")
            String description,

            @NotBlank(message = "Độ quan trọng không được để trống")
            String importance,

            String evidenceHint
    ) {}
}
