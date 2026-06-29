package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record CreateSystemRubricCriterionBandsRequest(
        @NotEmpty(message = "Phải gửi lên ít nhất 1 cấu hình mức điểm")
        @Valid
        List<CriterionBandItemRequest> bands
) {
    public record CriterionBandItemRequest(
            @NotBlank(message = "Mã mức đánh giá (Code) không được để trống")
            String code,

            @NotNull(message = "Điểm tối thiểu không được để trống")
            BigDecimal scoreMin,

            @NotNull(message = "Điểm tối đa không được để trống")
            BigDecimal scoreMax
    ) {}
}