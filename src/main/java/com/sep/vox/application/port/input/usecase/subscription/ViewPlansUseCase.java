package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewPlansQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.SubscriptionPlanQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.PlanStatus;

@Service
public class ViewPlansUseCase implements IUseCase<ViewPlansQuery, PageResult<SubscriptionPlanDto>> {

    private final SubscriptionPlanQueryRepository subscriptionPlanQueryRepository;

    public ViewPlansUseCase(SubscriptionPlanQueryRepository subscriptionPlanQueryRepository) {
        this.subscriptionPlanQueryRepository = subscriptionPlanQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SubscriptionPlanDto> execute(ViewPlansQuery input) {
        var page = subscriptionPlanQueryRepository.findAllByStatus(PlanStatus.ACTIVE, PageRequest.of(input.page(), input.size()));

        return new PageResult<>(page.getContent(), input.page(), input.size(), page.getTotalElements(), page.getTotalPages());
    }
}
