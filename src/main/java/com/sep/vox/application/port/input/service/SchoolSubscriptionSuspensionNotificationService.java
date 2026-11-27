package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.event.SchoolSubscriptionSuspendedPayloadV1;
import com.sep.vox.application.event.SchoolSubscriptionUnsuspendedPayloadV1;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Publishes 2 events notifying a plan suspension/unsuspension -- mirrors exactly how {@code SchoolDebtNotificationService}
 * writes to the outbox: the recipient list is FIXED AT PUBLISH TIME and embedded straight in the payload, with no
 * re-query by the consumer. Each method MUST be called in the same transaction as the place that changes the status (the caller
 * is responsible for @Transactional).
 *
 * <p>There is no separate audit table like SchoolDebtEvent -- the history of "who suspended/unsuspended when and why" already lives in
 * SchoolSubscriptionEvent(SUSPENDED/UNSUSPENDED) as the durable ledger, see ForceSuspendSubscriptionUseCase.
 */
@Service
@RequiredArgsConstructor
public class SchoolSubscriptionSuspensionNotificationService {

    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolUserRepository schoolUserRepository;

    public void publishSuspended(UUID subscriptionId, UUID schoolId, String reason, Instant now) {
        List<UUID> schoolAdminIds = schoolAdminIdsOf(schoolId);

        String payload = jsonSerializationPort.toJson(new SchoolSubscriptionSuspendedPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, reason, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED, payload, now
        ));
    }

    public void publishUnsuspended(UUID subscriptionId, UUID schoolId, Instant now) {
        List<UUID> schoolAdminIds = schoolAdminIdsOf(schoolId);

        String payload = jsonSerializationPort.toJson(new SchoolSubscriptionUnsuspendedPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED, payload, now
        ));
    }

    private List<UUID> schoolAdminIdsOf(UUID schoolId) {
        return schoolUserRepository.findBySchoolIdWithRole(schoolId, RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream().map(su -> su.getUserId()).toList();
    }
}
