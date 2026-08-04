package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ArchivePlanCommand(
    UUID planId,
    UUID replacedByPlanId
) {
}
