package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdatePlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class UpdatePlanUseCase implements IUseCase<UpdatePlanCommand, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final UserContextPort userContextPort;

    public UpdatePlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionPlanDto execute(UpdatePlanCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var plan = subscriptionPlanRepository.findById(input.planId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        if (input.name() != null) {
            plan.setName(input.name());
        }
        if (input.tagline() != null) {
            plan.setTagline(input.tagline());
        }
        if (input.pricePerYear() != null) {
            plan.setPricePerYear(input.pricePerYear());
        }
        if (input.validityDays() != null) {
            plan.setValidityDays(input.validityDays());
        }
        if (input.maxTimePerAttemptMin() != null) {
            plan.setMaxTimePerAttemptMin(input.maxTimePerAttemptMin());
        }
        if (input.popular() != null) {
            plan.setPopular(input.popular());
        }
        plan.setVersion(plan.getVersion() + 1);
        var savedPlan = subscriptionPlanRepository.save(plan);

        var quotas = planQuotaRepository.findAllByPlanId(savedPlan.getId());
        if (input.quotas() != null) {
            planQuotaRepository.deleteAllByPlanId(savedPlan.getId());
            quotas = input.quotas().stream()
                .map(quotaInput -> planQuotaRepository.save(new PlanQuota(
                    savedPlan.getId(),
                    quotaInput.quotaType(),
                    quotaInput.includedQuantity(),
                    quotaInput.tokenUnitPrice()
                )))
                .toList();
        }

        return SubscriptionPlanDtoMapper.toDto(savedPlan, quotas);
    }
}
