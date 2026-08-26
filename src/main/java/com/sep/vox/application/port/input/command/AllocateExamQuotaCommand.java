package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.common.DistributionMode;

public record AllocateExamQuotaCommand(
    UUID schoolId,
    DistributionMode mode,
    List<UserQuotaAmount> allocations
) {
}
