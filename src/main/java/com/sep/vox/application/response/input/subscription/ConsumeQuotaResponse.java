package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;

/**
 * The result of ONE cost deduction (ConsumeQuotaService) -- not a snapshot of any row.
 *
 * <p>These numbers are not put into {@link SchoolSubscriptionQuotaRecordDto} for the same reason given in
 * {@link SchoolSubscriptionQuotaRecordResponse}: they are not columns of the quota wallet. More importantly,
 * one deduction now touches TWO different aggregates (the quota wallet + the school balance), so a DTO projecting
 * exactly one row cannot express it fully even with extra columns.
 *
 * <p>The derived numbers are computed RIGHT INSIDE the transaction that holds the balance row lock and returned here, instead of
 * letting the caller query again after commit: two reads outside the lock may not match what has just
 * happened inside the lock.
 *
 * @param quota               the quota wallet AFTER the deduction
 * @param chargedToQuotaVnd   the part that fell within the quota that came with the plan
 * @param chargedToBalanceVnd the part that EXCEEDED it, recorded as an OVERAGE_CHARGE entry (0 if nothing exceeded).
 *                            Always {@code chargedToQuotaVnd + chargedToBalanceVnd = the amount deducted}
 * @param balanceAfterVnd     the top-up wallet balance after the entry -- NEGATIVE means the school is in debt
 */
public record ConsumeQuotaResponse(
    SchoolSubscriptionQuotaRecordDto quota,
    BigDecimal chargedToQuotaVnd,
    BigDecimal chargedToBalanceVnd,
    BigDecimal balanceAfterVnd,

    /**
     * THIS very deduction pushed the school from no debt into debt -- a TRANSITION, true exactly once.
     * Used to send a notification. If it was already in debt beforehand, this flag is false.
     */
    boolean crossedIntoDebt,

    /**
     * Both the quota and the balance are fully used up -- a STATE, true on every later deduction and not only
     * the first. Used to close a speaking practice session properly (SubmitTurnResultDto.quotaExhausted).
     *
     * <p>Deliberately separated from {@link #crossedIntoDebt}: one says "it just happened", the other says "it is
     * currently so". Merged into one, either the notification repeats every turn, or the session never closes.
     */
    boolean fundsExhausted
) {
}
