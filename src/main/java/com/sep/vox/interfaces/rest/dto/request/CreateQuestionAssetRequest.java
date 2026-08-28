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

    @Size(max = 4096, message = "URL tài nguyên không được vượt quá 4096 ký tự")
    String url,

    String transcript,

    // Không giới hạn độ dài, cùng lý do với transcript ngay trên: description là THỨ DUY NHẤT AI
    // biết về tài nguyên (nó không nhìn được ảnh, không nghe được tệp), nên cắt ngắn là cắt đúng
    // phần thông tin dùng để hỏi và chấm. Cột đã là TEXT từ V42.
    String description,

    @NotNull(message = "Thứ tự tài nguyên không được để trống")
    @Min(value = 0, message = "Thứ tự tài nguyên không được nhỏ hơn 0")
    Integer order
) {
}
