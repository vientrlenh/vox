package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApproveRegisterFormRequest(
    @NotNull(message = "ID của đơn đăng ký không được để trống")
    UUID registerFormId,

    @NotBlank(message = "Mã trường không được để trống")
    @Size(max = 100, message = "Mã trường không được vượt quá 100 ký tự")
    String schoolCode,

    @Size(max = 2048, message = "Mô tả trường không được vượt quá 2048 ký tự")
    String description, 

    @NotBlank(message = "Mã tỉnh thành không được để trống")
    @Size(max = 100, message = "Mã tỉnh thành không được vượt quá 100 ký tự")
    String provinceCode
) {
    
}
