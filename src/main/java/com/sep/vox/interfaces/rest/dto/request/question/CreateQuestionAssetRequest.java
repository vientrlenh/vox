package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.CreateQuestionAssetMutationCommand;
import com.sep.vox.domain.model.question.QuestionAssetType;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionAssetRequest(
    @Size(max = 255, message = "Tiêu đề tài nguyên không được vượt quá 255 ký tự")
    String title,

    @Min(value = 0, message = "Thời lượng tài nguyên phải lớn hơn hoặc bằng 0")
    Integer durationSeconds,

    @Size(max = 255, message = "Văn bản thay thế không được vượt quá 255 ký tự")
    String altText,

    @NotNull(message = "Loại tài nguyên không được để trống")
    QuestionAssetType type,

    @Size(max = 4096, message = "URL tài nguyên không được vượt quá 4096 ký tự")
    String url,

    String transcript,

    // No length limit, same reason as the transcript right above: description is the ONLY THING the AI
    // knows about the asset (it cannot see the image or hear the file), so truncating it cuts exactly
    // the information used to ask and grade. The column has been TEXT since V42.
    String description,

    @NotNull(message = "Thứ tự tài nguyên không được để trống")
    @Min(value = 0, message = "Thứ tự tài nguyên không được nhỏ hơn 0")
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
