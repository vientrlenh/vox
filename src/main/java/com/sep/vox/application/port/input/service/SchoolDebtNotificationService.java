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
            BigDecimal triggerAmountVnd, BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd,
            BigDecimal overageVnd, BigDecimal capVnd, Instant now) {
        var systemAdminIds = userRoleRepository.findActiveUserIdsByRoleCode(RoleConstant.SYSTEM_ADMIN_ROLE);

        var payload = jsonSerializationPort.toJson(new SchoolDebtCapExceededPayloadV1(
            systemAdminIds, schoolId, subscriptionId, quotaType, overageVnd, capVnd, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, payload, now
        ));

        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.CAP_EXCEEDED, quotaType,
            triggerExamSessionId, triggerAmountVnd, totalAllocatedVnd, usedAmountVnd, overageVnd, now);
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
            UUID subscriptionId, UUID schoolId, QuotaType quotaType, UUID triggerExamSessionId,
            BigDecimal triggerAmountVnd, BigDecimal totalAllocatedVnd, BigDecimal debtVnd, Instant now) {
        var schoolAdminIds = schoolAdminIdsOf(schoolId);

        var payload = jsonSerializationPort.toJson(new SchoolLockedDueToDebtPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT, payload, now
        ));

        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.LOCKED, quotaType, triggerExamSessionId,
            triggerAmountVnd, totalAllocatedVnd, totalAllocatedVnd.add(debtVnd), debtVnd, now);
    }

    public void publishSchoolDebtCleared(
            UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd, Instant now) {
        var schoolAdminIds = schoolAdminIdsOf(schoolId);

        var payload = jsonSerializationPort.toJson(new SchoolDebtClearedPayloadV1(
            schoolAdminIds, schoolId, subscriptionId, now
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.SCHOOL_SUBSCRIPTION, subscriptionId,
            EventTypeConstant.SCHOOL_DEBT_CLEARED, payload, now
        ));

        // overage = 0 CỐ ĐỊNH, không phải usedAmountVnd - totalAllocatedVnd: hết nợ nghĩa là số dư đã
        // về không âm, tức phần vượt bằng 0 theo đúng định nghĩa. Hiệu kia giờ luôn <= 0 (used bị kẹp
        // tại total) nên chỉ ghi được số 0 hoặc một số ÂM vô nghĩa trên sổ đối soát.
        logDebtEvent(schoolId, subscriptionId, SchoolDebtEventType.CLEARED, quotaType, null, null,
            totalAllocatedVnd, usedAmountVnd, BigDecimal.ZERO, now);
    }

    private void logDebtEvent(
            UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType, QuotaType quotaType,
            UUID triggerExamSessionId, BigDecimal triggerAmountVnd, BigDecimal totalAllocatedVnd,
            BigDecimal usedAmountVnd, BigDecimal overageVnd, Instant now) {
        schoolDebtEventRepository.save(new SchoolDebtEvent(
            schoolId, subscriptionId, eventType, quotaType, triggerExamSessionId, triggerAmountVnd,
            totalAllocatedVnd, usedAmountVnd, overageVnd, now
        ));
    }

    private List<UUID> schoolAdminIdsOf(UUID schoolId) {
        return schoolUserRepository.findBySchoolIdWithRole(schoolId, RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream().map(su -> su.getUserId()).toList();
    }
}
