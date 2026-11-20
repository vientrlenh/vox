package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.common.DecimalText;
import com.sep.vox.domain.model.school.SchoolDebtEvent;

/**
 * One row of the quota debt log, in read form. Maps 1-1 to {@link SchoolDebtEvent}.
 *
 * <p>The four money columns are String rather than BigDecimal/Float -- see {@link DecimalText} and the
 * comment block at the top of {@code school-balance.graphqls}. They are {@code numeric(18,6)} like the
 * wallet ledger, so they must speak the same language as it: the same debt appears in both ledgers, and
 * two ledgers that reconcile against each other must not drift because of binary rounding.
 */
public record SchoolDebtEventDto(
    UUID id,
    UUID schoolId,
    UUID subscriptionId,
    String eventType,
    String quotaType,
    UUID triggerAssessmentSessionId,
    UUID triggerPracticeSessionId,
    String triggerAmountVnd,
    String totalAllocatedVnd,
    String usedAmountVnd,
    String overageVnd,
    Instant occurredAt
) {

    public static SchoolDebtEventDto toDto(SchoolDebtEvent event) {
        return new SchoolDebtEventDto(
            event.getId(),
            event.getSchoolId(),
            event.getSubscriptionId(),
            event.getEventType() == null ? null : event.getEventType().name(),
            event.getQuotaType() == null ? null : event.getQuotaType().name(),
            event.getTriggerAssessmentSessionId(),
            event.getTriggerPracticeSessionId(),
            // null for a CLEARED row: clearing the debt is not caused by any single deduction.
            DecimalText.of(event.getTriggerAmountVnd()),
            DecimalText.of(event.getTotalAllocatedVnd()),
            DecimalText.of(event.getUsedAmountVnd()),
            DecimalText.of(event.getOverageVnd()),
            event.getOccurredAt()
        );
    }
}
