package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.DistributionMode;

public record AllocateClassTestQuotaCommand(
    UUID schoolId,
    DistributionMode mode,
    List<UserQuotaAmount> allocations
) {
}
