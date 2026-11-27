package com.sep.vox.interfaces.rest.dto.request.question;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSystemQuestionBankQuestionRequest(
    @NotNull(message = "Bank ID must not be blank")
    UUID questionBankId,

    @NotNull(message = "Topic ID must not be blank")
    UUID questionTopicId,

    @NotBlank(message = "Question code must not be blank")
    @Size(max = 100, message = "Question code must not exceed 100 characters")
    String code,

    String instructionText,

    @NotBlank(message = "Question content must not be blank")
    String questionText,

    String promptText,

    String preparationText,

    String expectedContent,

    String keyPoints,

    String acceptableResponses,

    String offTopicExamples,

    String scoringHints,

    String commonMistakes,

    @NotNull(message = "Question type must not be blank")
    QuestionType type,

    @NotNull(message = "Preparation time must not be blank")
    @Min(value = 0, message = "Preparation time must not be below 0")
    Integer preparationTimeSeconds,

    @NotNull(message = "Minimum answer time must not be blank")
    @Min(value = 0, message = "Minimum answer time must not be less than 0")
    Integer minResponseSeconds,

    @NotNull(message = "Maximum answer time must not be blank")
    @Min(value = 0, message = "Maximum answer time must be greater than or equal to 0")
    Integer maxResponseSeconds,

    QuestionSharing sharing,

    @Valid
    List<CreateQuestionAssetRequest> assets
) {

    @AssertTrue(message = "Minimum answer time must not be greater than maximum answer time")
    public boolean isResponseDurationRangeValid() {
        return minResponseSeconds == null
            || maxResponseSeconds == null
            || minResponseSeconds <= maxResponseSeconds;
    }
}
