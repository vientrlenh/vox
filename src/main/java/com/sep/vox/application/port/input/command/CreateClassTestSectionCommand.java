package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateClassTestSectionCommand(
    UUID examId,
    String title,
    String instruction,
    BigDecimal weight,
    List<ClassTestQuestionCommand> questions
) {
}
