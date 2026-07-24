package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateFrameworkResultBandsRequest(
        @NotEmpty(message = "Phải gửi lên ít nhất 1 mức kết quả")
        @Valid
        List<ResultBandItemRequest> bands
) {
    public record ResultBandItemRequest(
            @NotBlank(message = "Mã kết quả không được để trống")
            String code,

            @NotBlank(message = "Nhãn kết quả không được để trống")
            String label,

            String description,

            int order
    ) {}
}
