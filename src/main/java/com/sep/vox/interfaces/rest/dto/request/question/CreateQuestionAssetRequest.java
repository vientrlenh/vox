package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.CreateQuestionAssetMutationCommand;
import com.sep.vox.domain.model.question.QuestionAssetType;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionAssetRequest(
    @Size(max = 255, message = "Asset title must not exceed 255 characters")
    String title,

    @Min(value = 0, message = "Asset duration must be greater than or equal to 0")
    Integer durationSeconds,

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    String altText,

    @NotNull(message = "Asset type must not be blank")
    QuestionAssetType type,

    @Size(max = 4096, message = "Asset URL must not exceed 4096 characters")
    String url,

    String transcript,

    // No length limit, same reason as the transcript right above: description is the ONLY THING the AI
    // knows about the asset (it cannot see the image or hear the file), so truncating it cuts exactly
    // the information used to ask and grade. The column has been TEXT since V42.
    String description,

    @NotNull(message = "Asset order must not be blank")
    @Min(value = 0, message = "Asset order must not be less than 0")
    Integer order
) {

    public static CreateQuestionAssetMutationCommand toCommand(UUID questionId, CreateQuestionAssetRequest request) {
        return new CreateQuestionAssetMutationCommand(
            questionId,
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
