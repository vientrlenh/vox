package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewPlanDetailQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ViewPlanDetailUseCase implements IUseCase<ViewPlanDetailQuery, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;

    public ViewPlanDetailUseCase(SubscriptionPlanRepository subscriptionPlanRepository, PlanQuotaRepository planQuotaRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanDto execute(ViewPlanDetailQuery input) {
        var plan = subscriptionPlanRepository.findById(input.planId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));
        return SubscriptionPlanDtoMapper.toDto(plan, planQuotaRepository.findAllByPlanId(plan.getId()));
    }
}
