package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.PublishSubscriptionPlanCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanReplacementValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class PublishSubscriptionPlanUseCase implements IUseCase<PublishSubscriptionPlanCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private final SubscriptionPlanReplacementValidator replacementValidator;

    @Override
    @Transactional
    public UUID execute(PublishSubscriptionPlanCommand input) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        requireDraft(plan);
        requireAtLeastOneQuota(plan);
        requireCompatibleWithReplacedPlans(plan);

        plan.setStatus(SubscriptionPlanStatus.ACTIVE);
        // The version is not incremented by hand: the version column is managed by Hibernate through @Version -- see
        // UpdateSubscriptionPlanUseCase.
        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);

        return savedPlan.getId();
    }

    // ACTIVE means it was already sold, and ARCHIVED means selling stopped PERMANENTLY -- allowing a publish of an ARCHIVED
    // plan would resurrect a plan whose replacedByPlanId points elsewhere, giving two live plans on one replacement chain.
    // To sell again, create a new plan.
    private void requireDraft(SubscriptionPlan plan) {
        if (plan.getStatus() != SubscriptionPlanStatus.DRAFT) {
            throw new IllegalArgumentException("Only a plan in DRAFT status can be published.");
        }
    }

    // The last gate before the plan can be sold to schools. No database constraint forces a plan to have at least one quota
    // row -- CreateSubscriptionPlanUseCase and UpdateSubscriptionPlanUseCase both check it at the application level, so any
    // path that does not go through those two use cases (seed, scripts, a later import) can still produce an empty plan.
    // Publish is the only moment left that can still block it: a plan without quota leaves the paying school unable to use
    // anything, and the error only shows up when grading.
    private void requireAtLeastOneQuota(SubscriptionPlan plan) {
        if (subscriptionPlanQuotaRepository.findBySubscriptionPlanId(plan.getId()).isEmpty()) {
            throw new IllegalArgumentException("A plan must have at least one quota before it is published.");
        }
    }

    // Whether this plan is currently the replacement (replacedByPlanId) of any other plan -- if so, this is the moment it
    // REALLY takes on that role (schools will be moved to this plan on renewal, with no chance to refuse). It must meet the
    // same standard as the manual archive flow before it is allowed to go live.
    private void requireCompatibleWithReplacedPlans(SubscriptionPlan plan) {
        for (SubscriptionPlan replacedPlan : subscriptionPlanRepository.findByReplacedByPlanId(plan.getId())) {
            replacementValidator.requireCompatibleTerms(plan, replacedPlan);
        }
    }
}
