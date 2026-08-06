package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateClassTestSectionRequest(
    /** Mã đề nhận section mới. Bỏ trống chỉ hợp lệ khi bài có đúng một mã đề. */
    UUID paperId,

    @NotBlank(message = "Tiêu đề section là bắt buộc")
    String title,

    String instruction,

    BigDecimal weight,

    @Valid
    @NotEmpty(message = "Section phải có ít nhất một câu hỏi")
    List<ClassTestQuestionRequest> questions
) {
}
