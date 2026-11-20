package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record AllocateAssessmentQuotaCommand(
    UUID schoolId,
    String mode,
    List<AllocateUserQuotaAmountCommand> allocations,
    boolean confirmWalletDraw
) {

}
