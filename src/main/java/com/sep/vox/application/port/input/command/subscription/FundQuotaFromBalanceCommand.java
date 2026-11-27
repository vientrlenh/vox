package com.sep.vox.application.port.input.command.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Transfers {@code amountVnd} from the school's top-up wallet to the quota wallet of {@code quotaType}.
 *
 * <p>{@code reason} is optional -- the administrator's note (e.g. "at the request of class 12A"), stored directly
 * in the ledger entry so it can be traced back.
 */
public record FundQuotaFromBalanceCommand(
    UUID schoolId,
    QuotaType quotaType,
    BigDecimal amountVnd,
    String reason
) {
}
