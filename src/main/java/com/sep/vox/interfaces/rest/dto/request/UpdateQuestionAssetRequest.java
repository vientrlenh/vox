package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.question.QuestionAssetType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateQuestionAssetRequest(
    @Size(max = 255, message = "Tiêu đề tài nguyên không được vượt quá 255 ký tự")
    String title,

    @Min(value = 0, message = "Thời lượng tài nguyên phải lớn hơn hoặc bằng 0")
    Integer durationSeconds,

    @Size(max = 255, message = "Văn bản thay thế không được vượt quá 255 ký tự")
    String altText,

    QuestionAssetType type,

    @Size(max = 4096, message = "URL tài nguyên không được vượt quá 4096 ký tự")
    String url,

    String transcript,

    // Không giới hạn độ dài -- xem CreateQuestionAssetRequest. Cột đã là TEXT từ V42.
    String description,

    @Min(value = 0, message = "Thứ tự tài nguyên không được nhỏ hơn 0")
    Integer order
) {
}
