package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSystemQuestionBankQuestionRequest(
    @NotNull(message = "ID chu de khong duoc de trong")
    UUID questionTopicId,

    @NotBlank(message = "Ma cau hoi khong duoc de trong")
    @Size(max = 100, message = "Ma cau hoi khong duoc vuot qua 100 ky tu")
    String code,

    String instructionText,

    @NotBlank(message = "Noi dung cau hoi khong duoc de trong")
    String questionText,

    String promptText,

    String preparationText,

    @NotBlank(message = "Loai cau hoi khong duoc de trong")
    @Pattern(
        regexp = "READ_ALOUD|SHORT_ANSWER|LONG_ANSWER|OPINION|DESCRIPTION",
        message = "Loai cau hoi khong hop le"
    )
    String type,

    @NotBlank(message = "Pham vi cau hoi khong duoc de trong")
    @Pattern(
        regexp = "QUESTION_BANK|CLASSROOM_ASSESSMENT|CENTRAL_EXAM_DRAFT|CENTRAL_EXAM_PAPER",
        message = "Pham vi cau hoi khong hop le"
    )
    String scope,

    @NotBlank(message = "Che do hien thi khong duoc de trong")
    @Pattern(
        regexp = "BANK_VISIBLE|AUTHOR_ONLY|REVIEWER_ONLY|ASSESSMENT_ONLY|EXAM_PAPER_ONLY",
        message = "Che do hien thi khong hop le"
    )
    String visibility,

    @NotNull(message = "Thoi gian chuan bi khong duoc de trong")
    @Min(value = 0, message = "Thoi gian chuan bi khong duoc duoi 0")
    Integer preparationTimeSeconds,

    @NotNull(message = "Thoi gian tra loi toi thieu khong duoc de trong")
    @Min(value = 0, message = "Thoi gian tra loi toi thieu khong duoc nho hon 0")
    Integer minResponseSeconds,

    @NotNull(message = "Thoi gian tra loi toi da khong duoc de trong")
    @Min(value = 0, message = "Thoi gian tra loi toi da phai lon hon hoac bang 0")
    Integer maxResponseSeconds
) {

    @AssertTrue(message = "Thoi gian tra loi toi thieu khong duoc lon hon thoi gian tra loi toi da")
    public boolean isResponseDurationRangeValid() {
        return minResponseSeconds == null
            || maxResponseSeconds == null
            || minResponseSeconds <= maxResponseSeconds;
    }
}
