package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateQuestionRequest(
    @NotNull(message = "ID ngân hàng không được để trống")
    UUID questionBankId,

    @NotNull(message = "ID chủ đề không được để trống")
    UUID questionTopicId,

    String instructionText,

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    String questionText,

    String promptText,

    String preparationText,

    @NotBlank(message = "Loại câu hỏi không được để trống")
    @Pattern(
        regexp = "READ_ALOUD|SHORT_ANSWER|LONG_ANSWER|OPINION|DESCRIPTION",
        message = "Loại câu hỏi không hợp lệ"
    )
    String type,

    @NotNull(message = "Thời gian chuẩn bị không được để trống")
    @Min(value = 0, message = "Thời gian chuẩn bị không được dưới 0")
    Integer preparationTimeSeconds,

    @NotNull(message = "Thời gian trả lời tối thiểu không được để trống")
    @Min(value = 0, message = "Thời gian trả lời tối thiểu không được nhỏ hơn 0")
    Integer minResponseSeconds,

    @NotNull(message = "Thời gian trả lời tối đa không được để trống")
    @Min(value = 0, message = "Thời gian trả lời tối đa phải lớn hơn hoặc bằng 0")
    Integer maxResponseSeconds,

    @Pattern(
        regexp = "PRIVATE|SCHOOL_SHARED",
        message = "Chế độ chia sẻ không hợp lệ"
    )
    String sharing,

    @Valid
    List<CreateQuestionAssetRequest> assets,

    @Valid
    QuestionEvaluationGuideRequest evaluationGuide
) {

    @AssertTrue(message = "Thời gian trả lời tối thiểu không được lớn hơn thời gian trả lời tối đa")
    public boolean isResponseDurationRangeValid() {
        return minResponseSeconds == null
            || maxResponseSeconds == null
            || minResponseSeconds <= maxResponseSeconds;
    }
}
