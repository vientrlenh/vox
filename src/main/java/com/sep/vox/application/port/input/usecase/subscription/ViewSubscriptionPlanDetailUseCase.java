package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.subscription.ViewSubscriptionPlanDetailQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class ViewSubscriptionPlanDetailUseCase implements IUseCase<ViewSubscriptionPlanDetailQuery, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanDto execute(ViewSubscriptionPlanDetailQuery input) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        // Only the System Admin manages plans and so may see every status (DRAFT/ARCHIVED, to know which plans are
        // unpublished/discontinued). Other roles only see plans on sale -- the existence of a DRAFT/ARCHIVED plan is
        // hidden entirely instead of returning a permission error, consistent with ViewPlansUseCase.
        if (!userContextPort.isSystemAdmin() && plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new ResourceNotFoundException("Plan not found");
        }

        return SubscriptionPlanDto.toDto(plan);
    }
}
