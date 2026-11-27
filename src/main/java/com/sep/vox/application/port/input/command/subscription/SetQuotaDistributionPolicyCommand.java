package com.sep.vox.application.port.input.command.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/** @param distributableRatio 0..1 -- the part of the school's quota wallet that may be divided among individuals. */
public record SetQuotaDistributionPolicyCommand(
    UUID schoolId,
    QuotaType quotaType,
    BigDecimal distributableRatio
) {
}
