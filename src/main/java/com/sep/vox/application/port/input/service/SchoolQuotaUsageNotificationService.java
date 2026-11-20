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
 * Publishes the EARLY warning event when the AI quota wallet (ASSESSMENT/PRACTICE) of a school has just crossed the warning threshold --
 * mirrors exactly how {@link SchoolDebtNotificationService#publishSchoolLockedDueToDebt} writes the outbox:
 * FIXES the recipient list RIGHT AT publish time, embedded directly in the payload, not left for the consumer to query again.
 *
 * <p>Must be called in the same transaction as the place that detects the transition
 * ({@link ConsumeQuotaService#checkUsageWarningTransition}), exactly like how
 * {@code SchoolDebtNotificationService} writes the outbox inside the main business transaction.
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
        List<UUID> schoolAdminIds = schoolAdminIdsOf(schoolId);

        String payload = jsonSerializationPort.toJson(new SchoolQuotaUsageWarningPayloadV1(
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
