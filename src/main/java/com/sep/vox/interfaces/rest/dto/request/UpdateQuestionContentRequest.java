package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateQuestionContentRequest(
    String instructionText,
    @NotBlank(message = "Noi dung cau hoi khong duoc de trong") String questionText,
    String promptText,
    String preparationText,
    @NotBlank(message = "Loai cau hoi khong duoc de trong") String type,
    @NotBlank(message = "Pham vi cau hoi khong duoc de trong")
    @Pattern(
        regexp = "QUESTION_BANK|CLASSROOM_ASSESSMENT|CENTRAL_EXAM_DRAFT|CENTRAL_EXAM_PAPER",
        message = "Pham vi cau hoi khong hop le"
    ) String scope,
    @NotBlank(message = "Che do hien thi khong duoc de trong")
    @Pattern(
        regexp = "BANK_VISIBLE|AUTHOR_ONLY|REVIEWER_ONLY|ASSESSMENT_ONLY|EXAM_PAPER_ONLY",
        message = "Che do hien thi khong hop le"
    ) String visibility,
    @Min(value = 0, message = "Thoi gian chuan bi phai >= 0") int preparationTimeSeconds,
    @Min(value = 0, message = "Thoi gian tra loi toi thieu phai >= 0") int minResponseSeconds,
    @Min(value = 0, message = "Thoi gian tra loi toi da phai >= 0") int maxResponseSeconds
) {
}
