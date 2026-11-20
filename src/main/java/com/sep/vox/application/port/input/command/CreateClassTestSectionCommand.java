package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @param paperId mã đề nhận section mới. Bỏ trống chỉ hợp lệ khi bài có đúng một mã đề.
 */
public record CreateClassTestSectionCommand(
    UUID examId,
    UUID paperId,
    String title,
    String instruction,
    BigDecimal weight,
    List<ClassTestQuestionCommand> questions
) {
}
