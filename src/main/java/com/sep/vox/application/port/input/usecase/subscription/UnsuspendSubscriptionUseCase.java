package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UnsuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.mapper.SchoolSubscriptionDtoMapper;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

// Đưa gói đã bị ForceSuspendSubscriptionUseCase đình chỉ quay lại ACTIVE (vd xác minh oan). Nếu
// endDate đã qua lúc gỡ, không xử lý riêng ở đây -- SubscriptionExpiryJob (chỉ đụng status=ACTIVE) sẽ
// tự chuyển EXPIRED ở lần chạy kế tiếp, đúng hành vi tự nhiên.
@Service
public class UnsuspendSubscriptionUseCase implements IUseCase<UnsuspendSubscriptionCommand, SchoolSubscriptionDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final FinancialEventRepository financialEventRepository;
    private final SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private final UserContextPort userContextPort;

    public UnsuspendSubscriptionUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            FinancialEventRepository financialEventRepository,
            SchoolSubscriptionSuspensionNotificationService suspensionNotificationService,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.financialEventRepository = financialEventRepository;
        this.suspensionNotificationService = suspensionNotificationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SchoolSubscriptionDto execute(UnsuspendSubscriptionCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Chỉ System Admin được gỡ đình chỉ gói đăng ký.");
        }

        var subscription = schoolSubscriptionRepository.findById(input.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (!subscription.getSchoolId().equals(input.schoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (subscription.getStatus() != SubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("Gói đăng ký này hiện không bị đình chỉ");
        }

        var now = Instant.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var note = StringNormalization.trimAndCollapseSpaces(input.note());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSuspendedAt(null);
        subscription.setSuspendedReason(null);
        subscription.setSuspendedBy(null);
        var saved = schoolSubscriptionRepository.save(subscription);

        financialEventRepository.save(new FinancialEvent(
            input.schoolId(),
            saved.getId(),
            FinancialEventType.SUB_UNSUSPENDED,
            BigDecimal.ZERO,
            "VND",
            PaymentMethod.MANUAL,
            currentUserId,
            note,
            now
        ));

        suspensionNotificationService.publishUnsuspended(saved.getId(), input.schoolId(), now);

        return SchoolSubscriptionDtoMapper.toDto(saved);
    }
}
