package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

/**
 * @param source     {@code blueprint} | {@code copy} | {@code questions}.
 * @param sections   chỉ dùng khi {@code source = questions} (soạn câu hỏi trực tiếp — riêng bài
 *                   kiểm tra trên lớp).
 */
public record CreateExamPaperCommand(
    UUID examId,
    String source,
    UUID copyFromPaperId,
    List<ClassTestSectionCommand> sections
) {
}
