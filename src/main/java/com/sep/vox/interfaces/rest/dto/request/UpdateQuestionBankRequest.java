package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateQuestionBankRequest(
    @NotBlank(message = "Tên ngân hàng câu hỏi không được để trống")
    @Size(max = 255, message = "Tên ngân hàng câu hỏi không được vượt quá 255 ký tự")
    String bankName,

    @Size(max = 2048, message = "Mô tả không được vượt quá 2048 ký tự")
    String description,

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    Boolean isActive
) {
}
