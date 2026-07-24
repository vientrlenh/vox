package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

public record ClassTestQuestionCommand(
    UUID questionId,
    BigDecimal weight
) {
}
