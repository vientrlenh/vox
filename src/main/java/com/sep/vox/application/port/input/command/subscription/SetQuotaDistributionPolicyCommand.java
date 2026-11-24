package com.sep.vox.application.port.input.command.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/** @param distributableRatio 0..1 -- phần ví hạn mức trường được phép chia ra cho từng người. */
public record SetQuotaDistributionPolicyCommand(
    UUID schoolId,
    QuotaType quotaType,
    BigDecimal distributableRatio
) {
}
