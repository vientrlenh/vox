package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewPlansQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ViewPlansUseCase implements IUseCase<ViewPlansQuery, PageResult<SubscriptionPlanDto>> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;

    public ViewPlansUseCase(SubscriptionPlanRepository subscriptionPlanRepository, PlanQuotaRepository planQuotaRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SubscriptionPlanDto> execute(ViewPlansQuery input) {
        var plans = subscriptionPlanRepository.findAllByStatus(PlanStatus.ACTIVE);
        var fromIndex = Math.min(input.page() * input.size(), plans.size());
        var toIndex = Math.min(fromIndex + input.size(), plans.size());
        var pageContent = plans.subList(fromIndex, toIndex).stream()
            .map(plan -> SubscriptionPlanDtoMapper.toDto(plan, planQuotaRepository.findAllByPlanId(plan.getId())))
            .toList();
        var totalPages = (int) Math.ceil(plans.size() / (double) input.size());
        return new PageResult<>(pageContent, input.page(), input.size(), plans.size(), totalPages);
    }
}
