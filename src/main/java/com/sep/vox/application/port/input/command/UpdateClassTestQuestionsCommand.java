package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

/**
 * @param paperId mã đề được thay toàn bộ nội dung. Bỏ trống chỉ hợp lệ khi bài có đúng một mã đề.
 */
public record UpdateClassTestQuestionsCommand(
    UUID examId,
    UUID paperId,
    List<ClassTestSectionCommand> sections
) {
}
