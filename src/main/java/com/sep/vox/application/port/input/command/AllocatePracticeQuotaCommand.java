package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.common.DistributionMode;
import java.util.List;
import java.util.UUID;


public record AllocatePracticeQuotaCommand(
    UUID schoolId,
    DistributionMode mode,
    List<AllocateUserQuotaAmountCommand> allocations,
    boolean confirmWalletDraw
) {
}
