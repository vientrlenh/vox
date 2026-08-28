package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record AllocateExamQuotaCommand(
    UUID schoolId,
    String mode,
    List<AllocateUserQuotaAmountCommand> allocations
) {

}
