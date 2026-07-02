package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFrameworkRequest(
    @NotBlank(message = "Mã framework không được để trống")
    @Size(max = 100, message = "Mã framework không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã framework chỉ được chứa chữ hoa, số, gạch dưới và gạch ngang")
    String code,

    @NotBlank(message = "Tên framework không được để trống")
    @Size(max = 255, message = "Tên framework không được vượt quá 255 ký tự")
    String name,

    @Size(max = 2048, message = "Mô tả không được vượt quá 2048 ký tự")
    String description
) {
}
