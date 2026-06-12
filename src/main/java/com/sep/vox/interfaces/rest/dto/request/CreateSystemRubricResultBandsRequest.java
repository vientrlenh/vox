package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSystemRubricResultBandsRequest(
        @NotEmpty(message = "Phải có ít nhất 1 thang điểm được gửi lên")
        @Valid
        List<ResultBandItemRequest> resultBands
) {
    public record ResultBandItemRequest(
            @NotNull(message = "Framework Result Band ID không được để trống")
            UUID frameworkResultBandId,

            @NotBlank(message = "Mã thang điểm không được để trống")
            String code,

            @NotBlank(message = "Tên thang điểm (VD: Giỏi, Khá, Pass) không được để trống")
            String name,

            String description,

            @NotNull(message = "Điểm quy đổi tối thiểu không được để trống")
            BigDecimal mappedScoreMin,

            @NotNull(message = "Điểm quy đổi tối đa không được để trống")
            BigDecimal mappedScoreMax,

            @NotNull(message = "Thứ tự không được để trống")
            @Min(value = 1, message = "Thứ tự (Order) phải lớn hơn 0")
            Integer order,

            @NotNull(message = "Cờ đậu/rớt (Is Passing) không được để trống")
            Boolean isPassing
    ) {}
}