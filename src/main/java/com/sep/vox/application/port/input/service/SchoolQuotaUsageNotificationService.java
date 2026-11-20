package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.event.SchoolQuotaUsageWarningPayloadV1;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Phát event cảnh báo SỚM khi ví hạn mức AI (EXAM/PRACTICE) của 1 trường vừa vượt ngưỡng cảnh báo --
 * mirror đúng cách {@link SchoolDebtNotificationService#publishSchoolLockedDueToDebt} ghi outbox:
 * CHỐT danh sách người nhận NGAY LÚC publish, nhúng thẳng vào payload, không để consumer truy vấn lại.
 *
 * <p>Phải được gọi trong cùng transaction với nơi phát hiện transition
 * ({@link ConsumeQuotaService#checkUsageWarningTransition}), giống hệt cách
 * {@code SchoolDebtNotificationService} ghi outbox ngay trong transaction xử lý nghiệp vụ chính.
 */
@Service
public class SchoolQuotaUsageNotificationService {

    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolUserRepository schoolUserRepository;

    public SchoolQuotaUsageNotificationService(
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort,
            SchoolUserRepository schoolUserRepository) {
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    public void publishQuotaUsageWarning(
            UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd, Instant now) {
        var schoolAdminIds = schoolAdminIdsOf(schoolId);

        var payload = jsonSerializationPort.toJson(new SchoolQuotaUsageWarningPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, quotaType, totalAllocatedVnd, usedAmountVnd, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING, payload, now
        ));
    }

    private List<UUID> schoolAdminIdsOf(UUID schoolId) {
        return schoolUserRepository.findBySchoolIdWithRole(schoolId, RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream().map(su -> su.getUserId()).toList();
    }
}
