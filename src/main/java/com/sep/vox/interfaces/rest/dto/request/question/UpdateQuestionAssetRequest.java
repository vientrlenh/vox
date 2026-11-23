package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.UpdateQuestionAssetCommand;
import com.sep.vox.domain.model.question.QuestionAssetType;
import java.util.UUID;

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

    // No length limit -- see CreateQuestionAssetRequest. The column has been TEXT since V42.
    String description,

    @Min(value = 0, message = "Thứ tự tài nguyên không được nhỏ hơn 0")
    Integer order
) {

    public static UpdateQuestionAssetCommand toCommand(
            UUID questionId,
            UUID assetId,
            UpdateQuestionAssetRequest request) {
        return new UpdateQuestionAssetCommand(
            questionId,
            assetId,
            request.title(),
            request.durationSeconds(),
            request.altText(),
            request.type(),
            request.url(),
            request.transcript(),
            request.description(),
            request.order()
        );
    }
}
