package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateQuestionAssetRequest(
    @Size(max = 255, message = "Tiêu đề tài nguyên không được vượt quá 255 ký tự")
    String title,

    @Min(value = 0, message = "Thời lượng tài nguyên phải lớn hơn hoặc bằng 0")
    Integer durationSeconds,

    @Size(max = 255, message = "Văn bản thay thế không được vượt quá 255 ký tự")
    String altText,

    @NotBlank(message = "Loại tài nguyên không được để trống")
    @Pattern(
        regexp = "AUDIO|IMAGE|VIDEO|TEXT_PASSAGE",
        message = "Loại tài nguyên không hợp lệ"
    )
    String type,

    @NotBlank(message = "URL tài nguyên không được để trống")
    @Size(max = 4096, message = "URL tài nguyên không được vượt quá 4096 ký tự")
    String url,

    String transcript,

    @Size(max = 2048, message = "Mô tả tài nguyên không được vượt quá 2048 ký tự")
    String description,

    @NotNull(message = "Thứ tự tài nguyên không được để trống")
    @Min(value = 0, message = "Thứ tự tài nguyên không được nhỏ hơn 0")
    Integer order
) {
}
