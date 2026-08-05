package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

/**
 * @param source   {@code blueprint} (mặc định) | {@code copy} | {@code questions}.
 * @param sections chỉ dùng khi {@code source = questions} — soạn câu hỏi trực tiếp, riêng bài kiểm
 *                 tra trên lớp.
 */
public record CreateExamPaperRequest(
    String source,
    UUID copyFromPaperId,

    @Valid
    List<ClassTestSectionRequest> sections
) {
}
