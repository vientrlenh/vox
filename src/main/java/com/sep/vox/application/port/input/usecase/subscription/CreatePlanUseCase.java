package com.sep.vox.application.port.input.usecase.subscription;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.CreatePlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class CreatePlanUseCase implements IUseCase<CreatePlanCommand, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final UserContextPort userContextPort;

    public CreatePlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionPlanDto execute(CreatePlanCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (input.quotas() == null || input.quotas().isEmpty()) {
            throw new IllegalArgumentException("Gói phải có ít nhất một hạn mức");
        }

        var plan = new SubscriptionPlan(
            input.name(),
            input.tagline(),
            input.pricePerYear(),
            input.validityDays(),
            input.maxTimePerAttemptMin(),
            input.maxStudentCount(),
            input.popular(),
            PlanStatus.ACTIVE,
            1,
            OffsetDateTime.now(),
            userContextPort.getCurrentAuthenticatedUserId()
        );
        var savedPlan = subscriptionPlanRepository.save(plan);

        var savedQuotas = input.quotas().stream()
            .map(quotaInput -> planQuotaRepository.save(new PlanQuota(
                savedPlan.getId(),
                quotaInput.quotaType(),
                quotaInput.includedQuantity(),
                quotaInput.tokenUnitPrice()
            )))
            .toList();

        return SubscriptionPlanDtoMapper.toDto(savedPlan, savedQuotas);
    }
}
