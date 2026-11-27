package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanReplacementCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanReplacementValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Sets (or re-sets) the replacement plan of a plan that is ALREADY archived.
 *
 * <p>Exists to fix a dead end that previously had NO way out: replacedByPlanId could only be set once, at the moment
 * ArchiveSubscriptionPlanUseCase is called, and that use case refuses to run on an ARCHIVED plan. So the scenario below
 * locked a school out permanently:
 *
 * <ol>
 *   <li>The school's subscription period ends -> the subscription becomes EXPIRED.
 *   <li>The System Admin archives the plan without choosing a replacement -- allowed, because the guard at that time
 *       only counts schools that are ACTIVE on the plan, and this school is no longer ACTIVE.
 *   <li>The school comes back to renew -> resolveActivePlan throws "The plan is discontinued and has no replacement plan yet".
 *   <li>The admin wants to patch it by setting a replacement plan -> archiving again is blocked with "The plan has already
 *       been archived."
 * </ol>
 *
 * <p>A separate use case instead of loosening ArchiveSubscriptionPlanUseCase to run twice: archiving is a change of the
 * plan's status, whereas this edits a pointer. Merged together, every reader would have to guess what a given call means,
 * and one side's guard would get in the way of the other.
 */
@Service
@RequiredArgsConstructor
public class UpdateSubscriptionPlanReplacementUseCase
        implements IUseCase<UpdateSubscriptionPlanReplacementCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanReplacementValidator replacementValidator;

    @Override
    @Transactional
    public UUID execute(UpdateSubscriptionPlanReplacementCommand input) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        // ONLY applies to an archived plan: the replacedByPlanId of a plan still on sale means nothing -- resolveActivePlan only
        // follows that pointer when the plan is ARCHIVED. Allowing it on an ACTIVE plan would create data that is never read, and
        // one day someone would believe it has an effect.
        if (plan.getStatus() != SubscriptionPlanStatus.ARCHIVED) {
            throw new IllegalArgumentException("Only an archived plan needs a replacement plan.");
        }

        SubscriptionPlan replacement = subscriptionPlanRepository.findById(input.replacedByPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Replacement plan not found"));

        // The same rule set as at archive time. Editing the pointer later must NOT be looser: schools are still forced to follow
        // it on renewal, and still have no chance to refuse.
        replacementValidator.requireValidReplacement(replacement, plan);

        plan.setReplacedByPlanId(replacement.getId());

        return subscriptionPlanRepository.save(plan).getId();
    }
}
