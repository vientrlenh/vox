package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record AddRubricVersionsRequest(
        @NotEmpty(message = "Danh sách phiên bản không được để trống")
        @Valid
        List<RubricVersionItemRequest> versions
) {
    public record RubricVersionItemRequest(
            @NotNull(message = "Số Version không được để trống")
            @Positive(message = "Số Version phải lớn hơn 0")
            Integer version,

            @NotBlank(message = "Tên phiên bản không được để trống")
            String name,

            @NotNull(message = "Điểm sàn không được để trống")
            @DecimalMin(value = "0.0", message = "Điểm sàn phải lớn hơn hoặc bằng 0")
            BigDecimal scoringScaleMin,

            @NotNull(message = "Điểm trần không được để trống")
            @DecimalMin(value = "0.0", inclusive = false, message = "Điểm trần phải lớn hơn 0")
            BigDecimal scoringScaleMax,

            @NotBlank(message = "Phương pháp tính điểm tổng không được để trống (SUM/WEIGHTED_AVERAGE)")
            String totalScoreMethod,

            String effectiveFrom,

            String effectiveTo
    ) {}
}