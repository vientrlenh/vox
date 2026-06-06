package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSystemQuestionBankQuestionRequest(
    @NotNull(message = "ID chủ đề không được để trống")
    UUID questionTopicId,

    @NotBlank(message = "Mã câu hỏi không được để trống")
    @Size(max = 100, message = "Mã câu hỏi không được vượt quá 100 ký tự")
    String code,

    String instructionText,

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    String questionText,

    String promptText,

    String preparationText,

    @NotNull(message = "ID của phiên bản cấp độ tiêu chuẩn không được để trống")
    UUID standardLevelVersionId,

    String expectedContent,

    String keyPoints,

    String acceptableResponses,

    String offTopicExamples,

    String scoringHints,

    String commonMistakes,

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

    @NotNull(message = "Thoi gian tra loi toi da khong duoc de trong")
    @Min(value = 0, message = "Thoi gian tra loi toi da phai lon hon hoac bang 0")
    Integer maxResponseSeconds,

    @Valid
    List<CreateQuestionAssetRequest> assets
) {

    @AssertTrue(message = "Thời gian trả lời tối thiểu không được lớn hơn thời gian trả lời tối đa")
    public boolean isResponseDurationRangeValid() {
        return minResponseSeconds == null
            || maxResponseSeconds == null
            || minResponseSeconds <= maxResponseSeconds;
    }
}
