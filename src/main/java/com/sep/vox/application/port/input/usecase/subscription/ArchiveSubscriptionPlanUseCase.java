package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.ArchiveSubscriptionPlanCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanReplacementValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class ArchiveSubscriptionPlanUseCase implements IUseCase<ArchiveSubscriptionPlanCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanReplacementValidator replacementValidator;

    @Override
    @Transactional
    public UUID execute(ArchiveSubscriptionPlanCommand input) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        requireArchivable(plan);
        requireReplacementWhenPlanInUse(plan, input.replacedByPlanId());
        assignReplacementIfGiven(plan, input.replacedByPlanId());
        plan.setStatus(SubscriptionPlanStatus.ARCHIVED);
        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);

        return savedPlan.getId();
    }

    private void requireArchivable(SubscriptionPlan plan) {
        if (plan.getStatus() == SubscriptionPlanStatus.DRAFT) {
            throw new IllegalArgumentException("A plan in DRAFT status must be hard-deleted, not archived.");
        }
        if (plan.getStatus() == SubscriptionPlanStatus.ARCHIVED) {
            throw new IllegalArgumentException("The plan was already archived.");
        }
    }

    // existsActiveByPlanId already existed but was never called -- a real gap: archiving unconditionally while an ACTIVE
    // school still uses this plan would leave that school stuck, unable to renew until someone creates a replacement plan
    // (SubscriptionPlanResolver.resolveActivePlan throws on renewal). Choosing a replacement plan is mandatory AT THE TIME OF
    // archiving if any school is using it.
    //
    // The second clause (existsByReplacedByPlanId) patches the same hole ONE STEP FURTHER AWAY: plan A already points to B as
    // its replacement, and now B is archived without naming a next plan. No school is ACTIVE on B, so the first clause lets
    // it through, but the schools on A would walk the whole chain and stop at an ARCHIVED plan with no way out -- still stuck,
    // just harder to see.
    private void requireReplacementWhenPlanInUse(SubscriptionPlan plan, @Nullable UUID replacedByPlanId) {
        if (replacedByPlanId == null) {
            if (schoolSubscriptionRepository.existsActiveByPlanId(plan.getId())) {
                throw new IllegalArgumentException(
                    "Schools are using this plan; choose a replacement plan or wait until no school uses this plan before archiving.");
            }
            if (subscriptionPlanRepository.existsByReplacedByPlanId(plan.getId())) {
                throw new IllegalArgumentException(
                    "This plan is the replacement of another plan; choose the next replacement plan before archiving.");
            }
        }
    }

    private void assignReplacementIfGiven(SubscriptionPlan plan, @Nullable UUID replacedByPlanId) {
        if (replacedByPlanId != null) {
            SubscriptionPlan replacement = subscriptionPlanRepository.findById(replacedByPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Replacement plan not found"));
            // A school forced to change plan on renewal (SubscriptionPlanResolver.resolveActivePlan) has no chance to refuse -- see
            // SubscriptionPlanReplacementValidator for the full set of rules.
            replacementValidator.requireValidReplacement(replacement, plan);
            plan.setReplacedByPlanId(replacement.getId());
        }
    }

}
