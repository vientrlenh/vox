package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record ClassTestSectionCommand(
    String title,
    List<UUID> questionIds
) {
}
