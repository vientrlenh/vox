package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A person and the amount of AI money they spent for the school within the window.
 *
 * <p>{@code allocatedAmountVnd} is the personal SPENDING CAP that the school divides up itself, not a wallet: it
 * holds no money and exceeding the cap produces no ledger entry. null means this person has no cap --
 * they can still spend, there is just no level to compare against.
 */
public record UserAiSpendDto(
    UUID userId,
    String fullName,
    String quotaType,
    BigDecimal spentVnd,
    BigDecimal allocatedAmountVnd
) {
}
