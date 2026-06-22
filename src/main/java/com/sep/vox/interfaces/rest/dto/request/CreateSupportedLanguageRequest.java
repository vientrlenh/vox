package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportedLanguageRequest(

    @NotBlank(message = "Mã ngôn ngữ không được để trống")
    @Size(max = 10, message = "Mã ngôn ngữ không được vượt quá 10 ký tự")
    String code,

    @NotBlank(message = "Tên ngôn ngữ không được để trống")
    @Size(max = 100, message = "Tên ngôn ngữ không được vượt quá 100 ký tự")
    String name,

    @Size(max = 2048, message = "Mô tả ngôn ngữ không được vượt quá 2048 ký tự")
    String description
) {
}
