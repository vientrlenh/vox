package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ClassTestSectionCommand(
    String title,
    String instruction,
    BigDecimal weight,
    List<UUID> questionIds
) {
}
