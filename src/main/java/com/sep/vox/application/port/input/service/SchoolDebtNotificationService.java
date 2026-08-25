package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
 * Phát 3 event thông báo nợ hạn mức AI, mirror đúng cách {@code InvoiceSettlementService
 * .publishInvoicePaid} đang ghi outbox -- CHỐT danh sách người nhận NGAY LÚC publish, nhúng thẳng vào
 * payload, không để consumer truy vấn lại (xem javadoc {@link SchoolLockedDueToDebtPayloadV1}).
 *
 * <p>Mỗi method PHẢI được gọi trong cùng transaction với nơi phát hiện transition (nơi gọi tự chịu
 * trách nhiệm @Transactional), giống hệt cách InvoiceSettlementService ghi outbox ngay trong transaction
 * xử lý nghiệp vụ chính.
 *
 * <p>Song song với outbox (tạm thời, phục vụ push/in-app), mỗi lần phát còn ghi 1 dòng
 * {@link SchoolDebtEvent} -- sổ audit "nguyên nhân nợ" bền vững, để system admin tra lại lịch sử
 * (session nào, trừ bao nhiêu, snapshot hạn mức) bất kỳ lúc nào, không phụ thuộc vòng đời outbox/notification.
 */
@Service
public class SchoolDebtNotificationService {

    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolDebtEventRepository schoolDebtEventRepository;

    public SchoolDebtNotificationService(
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleRepository userRoleRepository,
            SchoolDebtEventRepository schoolDebtEventRepository) {
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolDebtEventRepository = schoolDebtEventRepository;
    }

    public void publishDebtCapExceeded(
            UUID subscriptionId, UUID schoolId, QuotaType quotaType, UUID triggerExamSessionId,
            BigDecimal triggerAmountUsd, BigDecimal totalAllocatedUsd, BigDecimal usedQuantityUsd,
            BigDecimal overageUsd, BigDecimal capUsd, Instant now) {
        var systemAdminIds = userRoleRepository.findActiveUserIdsByRoleCode(RoleConstant.SYSTEM_ADMIN_ROLE);

        var payload = jsonSerializationPort.toJson(new SchoolDebtCapExceededPayloadV1(
            systemAdminIds, schoolId, subscriptionId, quotaType, overageUsd, capUsd, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, payload, now
        ));

        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.CAP_EXCEEDED, quotaType,
            triggerExamSessionId, triggerAmountUsd, totalAllocatedUsd, usedQuantityUsd, overageUsd, now);
    }

    public void publishSchoolLockedDueToDebt(
            UUID subscriptionId, UUID schoolId, QuotaType quotaType, UUID triggerExamSessionId,
            BigDecimal triggerAmountUsd, BigDecimal totalAllocatedUsd, BigDecimal usedQuantityUsd, Instant now) {
        var schoolAdminIds = schoolAdminIdsOf(schoolId);

        var payload = jsonSerializationPort.toJson(new SchoolLockedDueToDebtPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT, payload, now
        ));

        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.LOCKED, quotaType, triggerExamSessionId,
            triggerAmountUsd, totalAllocatedUsd, usedQuantityUsd, usedQuantityUsd.subtract(totalAllocatedUsd), now);
    }

    public void publishSchoolDebtCleared(
            UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            BigDecimal totalAllocatedUsd, BigDecimal usedQuantityUsd, Instant now) {
        var schoolAdminIds = schoolAdminIdsOf(schoolId);

        var payload = jsonSerializationPort.toJson(new SchoolDebtClearedPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_DEBT_CLEARED, payload, now
        ));

        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.CLEARED, quotaType, null, null,
            totalAllocatedUsd, usedQuantityUsd, usedQuantityUsd.subtract(totalAllocatedUsd), now);
    }

    private void logDebtEvent(
            UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType, QuotaType quotaType,
            UUID triggerExamSessionId, BigDecimal triggerAmountUsd, BigDecimal totalAllocatedUsd,
            BigDecimal usedQuantityUsd, BigDecimal overageUsd, Instant now) {
        schoolDebtEventRepository.save(new SchoolDebtEvent(
            schoolId, subscriptionId, eventType, quotaType, triggerExamSessionId, triggerAmountUsd,
            totalAllocatedUsd, usedQuantityUsd, overageUsd, now
        ));
    }

    private List<UUID> schoolAdminIdsOf(UUID schoolId) {
        return schoolUserRepository.findBySchoolIdWithRole(schoolId, RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream().map(su -> su.getUserId()).toList();
    }
}
