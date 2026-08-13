package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RenewSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolDebtNotificationService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.mapper.SchoolSubscriptionDtoMapper;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;

@Service
public class RenewSubscriptionUseCase implements IUseCase<RenewSubscriptionCommand, SchoolSubscriptionDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final FinancialEventRepository financialEventRepository;
    private final UserContextPort userContextPort;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;
    private final SchoolDebtNotificationService schoolDebtNotificationService;

    public RenewSubscriptionUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            FinancialEventRepository financialEventRepository,
            UserContextPort userContextPort,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService,
            SchoolDebtNotificationService schoolDebtNotificationService) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.financialEventRepository = financialEventRepository;
        this.userContextPort = userContextPort;
        this.schoolSubscriptionDebtGuardService = schoolSubscriptionDebtGuardService;
        this.schoolDebtNotificationService = schoolDebtNotificationService;
    }

    @Override
    @Transactional
    public SchoolSubscriptionDto execute(RenewSubscriptionCommand input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var current = schoolSubscriptionRepository.findById(input.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (!current.getSchoolId().equals(input.schoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var plan = subscriptionPlanRepository.findById(current.getPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        var now = Instant.now();

        // Chụp bucket nào của gói CŨ đang vượt hạn mức trước khi expire nó -- gói mới tạo bên dưới
        // luôn có SubscriptionQuota tinh khôi nên chắc chắn không khóa.
        var wasOverGrading = schoolSubscriptionDebtGuardService.isQuotaOverLimit(current.getId(), QuotaType.GRADING);
        var wasOverClassTest = schoolSubscriptionDebtGuardService.isQuotaOverLimit(current.getId(), QuotaType.CLASS_TEST);

        current.setStatus(SubscriptionStatus.EXPIRED);
        schoolSubscriptionRepository.save(current);

        var startDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var renewed = new SchoolSubscription(
            input.schoolId(),
            plan.getId(),
            startDate,
            startDate.plusDays(plan.getValidityDays()),
            SubscriptionStatus.ACTIVE,
            plan.getPricePerYear(),
            null,
            now
        );
        var savedSubscription = schoolSubscriptionRepository.save(renewed);

        planQuotaRepository.findAllByPlanId(plan.getId()).forEach(planQuota ->
            subscriptionQuotaRepository.save(new SubscriptionQuota(
                savedSubscription.getId(),
                planQuota.getQuotaType(),
                planQuota.getIncludedQuantity(),
                BigDecimal.ZERO
            ))
        );

        financialEventRepository.save(new FinancialEvent(
            input.schoolId(),
            savedSubscription.getId(),
            FinancialEventType.SUB_RENEWED,
            plan.getPricePerYear(),
            "VND",
            PaymentMethod.MANUAL,
            userContextPort.getCurrentAuthenticatedUserId(),
            null,
            now
        ));

        reportDebtClearedIfNeeded(wasOverGrading, savedSubscription, QuotaType.GRADING, now);
        reportDebtClearedIfNeeded(wasOverClassTest, savedSubscription, QuotaType.CLASS_TEST, now);

        return SchoolSubscriptionDtoMapper.toDto(savedSubscription);
    }

    private void reportDebtClearedIfNeeded(boolean wasOver, SchoolSubscription newSubscription, QuotaType quotaType, Instant now) {
        if (!wasOver) {
            return;
        }
        subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(newSubscription.getId(), quotaType)
            .ifPresent(quota -> schoolDebtNotificationService.publishSchoolDebtCleared(
                newSubscription.getId(), newSubscription.getSchoolId(), quotaType,
                quota.getTotalAllocated(), quota.getUsedQuantity(), now
            ));
    }
}
