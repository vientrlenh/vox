package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record UpdateClassTestQuestionsRequest(
    /** Mã đề được thay toàn bộ nội dung. Bỏ trống chỉ hợp lệ khi bài có đúng một mã đề. */
    UUID paperId,

    @Valid
    @NotEmpty(message = "Danh sách section không được để trống")
    List<ClassTestSectionRequest> sections
) {
}
