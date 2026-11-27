package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.UpdateQuestionAssetCommand;
import com.sep.vox.domain.model.question.QuestionAssetType;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateQuestionAssetRequest(
    @Size(max = 255, message = "Asset title must not exceed 255 characters")
    String title,

    @Min(value = 0, message = "Asset duration must be greater than or equal to 0")
    Integer durationSeconds,

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    String altText,

    QuestionAssetType type,

    @Size(max = 4096, message = "Asset URL must not exceed 4096 characters")
    String url,

    String transcript,

    // No length limit -- see CreateQuestionAssetRequest. The column has been TEXT since V42.
    String description,

    @Min(value = 0, message = "Asset order must not be less than 0")
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
