package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ForceSuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.mapper.SchoolSubscriptionDtoMapper;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

// System Admin cưỡng chế mất quyền dùng NGAY LẬP TỨC (khác CancelSubscriptionUseCase, chỉ tắt gia hạn
// và vẫn ACTIVE tới hết endDate) -- dùng khi phát hiện trường vi phạm (vd gian lận), luôn kèm lý do.
@Service
public class ForceSuspendSubscriptionUseCase implements IUseCase<ForceSuspendSubscriptionCommand, SchoolSubscriptionDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final FinancialEventRepository financialEventRepository;
    private final SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private final UserContextPort userContextPort;
    private final ExamScheduleRepository examScheduleRepository;

    public ForceSuspendSubscriptionUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            FinancialEventRepository financialEventRepository,
            SchoolSubscriptionSuspensionNotificationService suspensionNotificationService,
            UserContextPort userContextPort,
            ExamScheduleRepository examScheduleRepository) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.financialEventRepository = financialEventRepository;
        this.suspensionNotificationService = suspensionNotificationService;
        this.userContextPort = userContextPort;
        this.examScheduleRepository = examScheduleRepository;
    }

    @Override
    @Transactional
    public SchoolSubscriptionDto execute(ForceSuspendSubscriptionCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Chỉ System Admin được đình chỉ gói đăng ký.");
        }

        var reason = StringNormalization.trimAndCollapseSpaces(input.reason());
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Phải nêu lý do đình chỉ gói đăng ký.");
        }

        var subscription = schoolSubscriptionRepository.findById(input.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (!subscription.getSchoolId().equals(input.schoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ đình chỉ được gói đang ở trạng thái đang hoạt động");
        }

        var now = Instant.now();
        var hasOngoingExam = examScheduleRepository.findBySchoolId(input.schoolId()).stream()
            .anyMatch(schedule -> schedule.isOngoingAt(now));
        if (hasOngoingExam) {
            throw new IllegalStateException(
                "Trường đang có ca thi diễn ra, không thể đình chỉ ngay bây giờ. Vui lòng thử lại sau khi ca thi kết thúc.");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        subscription.setSuspendedAt(now);
        subscription.setSuspendedReason(reason);
        subscription.setSuspendedBy(currentUserId);
        var saved = schoolSubscriptionRepository.save(subscription);

        financialEventRepository.save(new FinancialEvent(
            input.schoolId(),
            saved.getId(),
            FinancialEventType.SUB_SUSPENDED,
            BigDecimal.ZERO,
            "VND",
            PaymentMethod.MANUAL,
            currentUserId,
            reason,
            now
        ));

        suspensionNotificationService.publishSuspended(saved.getId(), input.schoolId(), reason, now);

        return SchoolSubscriptionDtoMapper.toDto(saved);
    }
}
