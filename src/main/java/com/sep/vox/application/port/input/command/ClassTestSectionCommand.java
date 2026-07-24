package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;

public record ClassTestSectionCommand(
    String title,
    String instruction,
    BigDecimal weight,
    List<ClassTestQuestionCommand> questions
) {
}
