package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateGradeLevelRequest(
        @NotBlank(message = "Mã khối không được để trống")
        String code,

        @NotBlank(message = "Tên khối không được để trống")
        String name,

        String description,

        @NotNull(message = "Thứ tự khối không được để trống")
        @Positive(message = "Thứ tự khối (order) phải lớn hơn 0")
        Integer order
) {}
