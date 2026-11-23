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
     * @param debtVnd số nợ THẬT lúc bị khóa, tức phần số dư đã âm (luôn {@code >= 0}).
     *
     * <p>Nhận thẳng số nợ chứ KHÔNG tự suy ra từ {@code usedAmountVnd - totalAllocatedVnd} như bản
     * trước: từ khi ConsumeQuotaService kẹp {@code used} tại {@code total} rồi đẩy phần vượt sang ví
     * tự nạp, hiệu đó không bao giờ dương nữa -- mọi dòng school_debt_events kiểu LOCKED vì thế ghi
     * overage_vnd = 0 (hoặc số âm) và dấu vết đối soát trở nên vô dụng đúng ở ca nghiêm trọng nhất.
     *
     * <p>{@code used_amount_vnd} ghi xuống là con số DỰNG LẠI {@code total + debt}, giống hệt cách
     * {@code ConsumeQuotaService.checkDebtCapTransition} dựng cho dòng CAP_EXCEEDED -- hai loại sự
     * kiện phải nói cùng một thứ tiếng thì mới xếp chung một bảng để đọc được.
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
     * Trường vừa thoát nợ: số dư ví đã về không âm nên khoá tự mở, không ai phải duyệt.
     *
     * <p>KHÔNG nhận quotaType/totalAllocated/usedAmount nữa. Hết nợ là sự kiện cấp TRƯỜNG: số dư là
     * MỘT con số dùng chung cho cả hai ví hạn mức, nên "ví nào vừa hết nợ" không có câu trả lời, và
     * bịa ra một quotaType để lấp chỗ trống là làm bẩn chính quyển sổ dùng để đối soát. Ba cột đó
     * bỏ trống ở dòng CLEARED, có chk_school_debt_events_shape_matches_event_type canh (V4).
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

        // overage = 0 CỐ ĐỊNH, không phải usedAmountVnd - totalAllocatedVnd: hết nợ nghĩa là số dư đã
        // về không âm, tức phần vượt bằng 0 theo đúng định nghĩa. Hiệu kia giờ luôn <= 0 (used bị kẹp
        // tại total) nên chỉ ghi được số 0 hoặc một số ÂM vô nghĩa trên sổ đối soát.
        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.CLEARED, null, null, null, null,
            null, null, BigDecimal.ZERO, now);
    }

    private void logDebtEvent(
            UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType, QuotaType quotaType,
            UUID triggerAssessmentSessionId, UUID triggerPracticeSessionId, BigDecimal triggerAmountVnd,
            BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd, BigDecimal overageVnd, Instant now) {
        schoolDebtEventRepository.save(SchoolDebtEvent.builder()
            .schoolId(schoolId)
            .subscriptionId(subscriptionId)
            .eventType(eventType)
            .quotaType(quotaType)
            .triggerAssessmentSessionId(triggerAssessmentSessionId)
            .triggerPracticeSessionId(triggerPracticeSessionId)
            .triggerAmountVnd(triggerAmountVnd)
            .totalAllocatedVnd(totalAllocatedVnd)
            .usedAmountVnd(usedAmountVnd)
            .overageVnd(overageVnd)
            .occurredAt(now)
            .build());
    }

    private List<UUID> schoolAdminIdsOf(UUID schoolId) {
        return schoolUserRepository.findBySchoolIdWithRole(schoolId, RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream().map(su -> su.getUserId()).toList();
    }
}
