package com.sep.vox.application.port.input.command.assessment;

import java.math.BigDecimal;
import java.util.UUID;

public record ClassTestQuestionCommand(
    UUID questionId,
    BigDecimal weight
) {
}
