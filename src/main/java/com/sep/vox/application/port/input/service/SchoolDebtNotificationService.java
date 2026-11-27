package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.event.SchoolDebtCapExceededPayloadV1;
import com.sep.vox.application.event.SchoolDebtClearedPayloadV1;
import com.sep.vox.application.event.SchoolLockedDueToDebtPayloadV1;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.school.SchoolDebtEvent;
import com.sep.vox.domain.model.school.SchoolDebtEventType;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolDebtEventRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

/**
 * Publishes the 3 AI-quota debt notification events, mirroring the way {@code OrderSettlementService
 * .publishInvoicePaid} writes to the outbox -- the recipient list is FIXED AT THE MOMENT OF PUBLISHING and embedded
 * directly in the payload, so the consumer does not have to query again (see the javadoc of
 * {@link SchoolLockedDueToDebtPayloadV1}).
 *
 * <p>Every method MUST be called in the same transaction as the place that detects the transition (the caller is
 * responsible for @Transactional), exactly as OrderSettlementService writes the outbox inside the transaction that
 * processes the main business logic.
 *
 * <p>In parallel with the outbox (temporary, serving push/in-app), every publication also writes 1 {@link SchoolDebtEvent}
 * row -- a durable "cause of debt" audit ledger, so the system admin can look up the history (which session, how much
 * was charged, the quota snapshot) at any time, independent of the outbox/notification lifecycle.
 */
@Service
@RequiredArgsConstructor
public class SchoolDebtNotificationService {

    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolDebtEventRepository schoolDebtEventRepository;

    /**
     * @param triggerAssessmentSessionId the ASSESSMENT session that caused the deduction over the ceiling -- null when the source is speaking practice
     * @param triggerPracticeSessionId the SPEAKING PRACTICE session that caused the deduction -- null when the source is an assessment session
     *
     * <p>Must accept BOTH because the PRACTICE quota wallet can also exceed its ceiling, just like the ASSESSMENT wallet. The previous version only
     * accepted assessmentSessionId so every CAP_EXCEEDED row caused by speaking practice recorded trigger = NULL: the ledger
     * knew the school exceeded the ceiling but not because of which expense, exactly when it is needed most.
     * chk_school_debt_events_shape_matches_event_type now requires EXACTLY ONE of the two, so forgetting to pass
     * one makes the INSERT fail right away instead of silently writing a row that lost its trace.
     */
    public void publishDebtCapExceeded(
            UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            UUID triggerAssessmentSessionId, UUID triggerPracticeSessionId,
            BigDecimal triggerAmountVnd, BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd,
            BigDecimal overageVnd, BigDecimal capVnd, Instant now) {
        List<UUID> systemAdminIds = userRoleRepository.findActiveUserIdsByRoleCode(RoleConstant.SYSTEM_ADMIN_ROLE);

        String payload = jsonSerializationPort.toJson(new SchoolDebtCapExceededPayloadV1(
            systemAdminIds, schoolId, subscriptionId, quotaType, overageVnd, capVnd, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, payload, now
        ));

        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.CAP_EXCEEDED, quotaType,
            triggerAssessmentSessionId, triggerPracticeSessionId, triggerAmountVnd, totalAllocatedVnd,
            usedAmountVnd, overageVnd, now);
    }

    /**
     * @param debtVnd the REAL debt at the time of the lock, i.e. the part of the balance that is negative (always {@code >= 0}).
     *
     * <p>Takes the debt directly instead of deriving it from {@code usedAmountVnd - totalAllocatedVnd} as the
     * previous version did: since ConsumeQuotaService clamps {@code used} at {@code total} and pushes the excess to the
     * self-funded balance, that difference is never positive again -- every LOCKED school_debt_events row therefore recorded
     * overage_vnd = 0 (or a negative number) and the reconciliation trail became useless in exactly the most serious case.
     *
     * <p>The {@code used_amount_vnd} written is the REBUILT figure {@code total + debt}, exactly as
     * {@code ConsumeQuotaService.checkDebtCapTransition} builds it for the CAP_EXCEEDED row -- the two kinds of
     * event must speak the same language to be readable in one table.
     */
    public void publishSchoolLockedDueToDebt(
            UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            UUID triggerAssessmentSessionId, UUID triggerPracticeSessionId,
            BigDecimal triggerAmountVnd, BigDecimal totalAllocatedVnd, BigDecimal debtVnd, Instant now) {
        List<UUID> schoolAdminIds = schoolAdminIdsOf(schoolId);

        String payload = jsonSerializationPort.toJson(new SchoolLockedDueToDebtPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT, payload, now
        ));

        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.LOCKED, quotaType,
            triggerAssessmentSessionId, triggerPracticeSessionId,
            triggerAmountVnd, totalAllocatedVnd, totalAllocatedVnd.add(debtVnd), debtVnd, now);
    }

    /**
     * The school has just got out of debt: the balance is back to non-negative so the lock opens by itself, nobody has to approve it.
     *
     * <p>It NO LONGER takes quotaType/totalAllocated/usedAmount. Clearing debt is a SCHOOL-level event: the balance is
     * ONE number shared by both quota records, so "which quota record just cleared its debt" has no answer, and
     * inventing a quotaType to fill the gap pollutes the very ledger used for reconciliation. Those three columns
     * are left empty on the CLEARED row, guarded by chk_school_debt_events_shape_matches_event_type (V4).
     */
    public void publishSchoolDebtCleared(UUID subscriptionId, UUID schoolId, Instant now) {
        List<UUID> schoolAdminIds = schoolAdminIdsOf(schoolId);

        String payload = jsonSerializationPort.toJson(new SchoolDebtClearedPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_DEBT_CLEARED, payload, now
        ));

        // overage = 0 FIXED, not usedAmountVnd - totalAllocatedVnd: clearing debt means the balance
        // is back to non-negative, i.e. the excess is 0 by definition. That difference is now always <= 0 (used is clamped
        // at total), so it could only write 0 or a meaningless NEGATIVE number on the reconciliation ledger.
        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.CLEARED, null, null, null, null,
            null, null, BigDecimal.ZERO, now);
    }

    private void logDebtEvent(
            UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType, QuotaType quotaType,
            UUID triggerAssessmentSessionId, UUID triggerPracticeSessionId, BigDecimal triggerAmountVnd,
            BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd, BigDecimal overageVnd, Instant now) {
        schoolDebtEventRepository.save(SchoolDebtEvent.recorded(
            schoolId, subscriptionId, eventType, quotaType, triggerAssessmentSessionId, triggerPracticeSessionId,
            triggerAmountVnd, totalAllocatedVnd, usedAmountVnd, overageVnd, now));
    }

    private List<UUID> schoolAdminIdsOf(UUID schoolId) {
        return schoolUserRepository.findBySchoolIdWithRole(schoolId, RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream().map(su -> su.getUserId()).toList();
    }
}
