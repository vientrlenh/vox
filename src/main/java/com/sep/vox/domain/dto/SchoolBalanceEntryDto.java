package com.sep.vox.domain.dto;

import java.util.UUID;

import com.sep.vox.domain.common.DecimalText;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;

/**
 * One entry of the wallet ledger, in read form. Maps 1-1 to {@link SchoolBalanceEntry} -- no joins,
 * no data enrichment.
 *
 * <p>Returns the raw {@code actorId} rather than the name of the person who acted: the same way {@code SchoolDebtEvent}
 * returns {@code triggerAssessmentSessionId}. Showing the name needs a LEFT JOIN to users, and then the DTO can
 * no longer be built from the domain model alone -- that is a separate decision, worth making when a screen
 * really needs it, not something to prepare here.
 *
 * <p>The four money columns are String -- see {@link DecimalText} and the comment block at the top of school-balance.graphqls.
 */
public record SchoolBalanceEntryDto(
    UUID id,
    UUID schoolId,
    String entryType,
    String amountVnd,
    String balanceAfterVnd,
    String occurredAt,
    UUID subscriptionId,
    UUID orderId,
    UUID assessmentSessionId,
    UUID practiceSessionId,
    String quotaType,
    String costUsd,
    String fxRateUsed,
    String reason,
    UUID actorId
) {

    public static SchoolBalanceEntryDto toDto(SchoolBalanceEntry entry) {
        return new SchoolBalanceEntryDto(
            entry.getId(),
            entry.getSchoolId(),
            entry.getEntryType() == null ? null : entry.getEntryType().name(),
            DecimalText.of(entry.getAmountVnd()),
            DecimalText.of(entry.getBalanceAfterVnd()),
            entry.getOccurredAt() == null ? null : entry.getOccurredAt().toString(),
            entry.getSubscriptionId(),
            entry.getOrderId(),
            entry.getAssessmentSessionId(),
            entry.getPracticeSessionId(),
            // Nullable ở tầng DB: chỉ OVERAGE_CHARGE mới bắt buộc có quota_type.
            entry.getQuotaType() == null ? null : entry.getQuotaType().name(),
            DecimalText.of(entry.getCostUsd()),
            DecimalText.of(entry.getFxRateUsed()),
            entry.getReason(),
            entry.getActorId()
        );
    }
}
