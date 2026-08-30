package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

/** @param distributableRatio 0..1 -- phần ví hạn mức trường được phép chia ra cho từng người. */
public record SetQuotaDistributionPolicyCommand(
    UUID schoolId,
    String quotaType,
    BigDecimal distributableRatio
) {
}
