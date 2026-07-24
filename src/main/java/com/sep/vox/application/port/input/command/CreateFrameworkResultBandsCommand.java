package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record CreateFrameworkResultBandsCommand(
        UUID frameworkId,
        UUID versionId,
        List<ResultBandItemCommand> bands
) {
    public record ResultBandItemCommand(
            String code,
            String label,
            String description,
            int order
    ) {}
}
