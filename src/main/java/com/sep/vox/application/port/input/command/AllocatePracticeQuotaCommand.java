package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.DistributionMode;

public record AllocatePracticeQuotaCommand(
    UUID schoolId,
    DistributionMode mode,
    List<UserQuotaAmount> allocations
) {
}
