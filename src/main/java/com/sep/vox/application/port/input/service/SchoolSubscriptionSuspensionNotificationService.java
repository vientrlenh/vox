package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
 * Phát 2 event thông báo đình chỉ/gỡ đình chỉ gói -- mirror đúng cách {@code SchoolDebtNotificationService}
 * đang ghi outbox: CHỐT danh sách người nhận NGAY LÚC publish, nhúng thẳng vào payload, không để
 * consumer truy vấn lại. Mỗi method PHẢI được gọi trong cùng transaction với nơi đổi status (nơi gọi
 * tự chịu trách nhiệm @Transactional).
 *
 * <p>Không có bảng audit riêng như SchoolDebtEvent -- lịch sử "ai đình chỉ/gỡ lúc nào/vì sao" đã có
 * FinancialEvent(SUB_SUSPENDED/SUB_UNSUSPENDED) làm sổ bền vững, xem ForceSuspendSubscriptionUseCase.
 */
@Service
public class SchoolSubscriptionSuspensionNotificationService {

    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolUserRepository schoolUserRepository;

    public SchoolSubscriptionSuspensionNotificationService(
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort,
            SchoolUserRepository schoolUserRepository) {
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    public void publishSuspended(UUID subscriptionId, UUID schoolId, String reason, Instant now) {
        var schoolAdminIds = schoolAdminIdsOf(schoolId);

        var payload = jsonSerializationPort.toJson(new SchoolSubscriptionSuspendedPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, reason, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED, payload, now
        ));
    }

    public void publishUnsuspended(UUID subscriptionId, UUID schoolId, Instant now) {
        var schoolAdminIds = schoolAdminIdsOf(schoolId);

        var payload = jsonSerializationPort.toJson(new SchoolSubscriptionUnsuspendedPayloadV1(
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
