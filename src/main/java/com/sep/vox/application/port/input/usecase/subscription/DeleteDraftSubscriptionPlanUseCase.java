package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.DeleteDraftSubscriptionPlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class DeleteDraftSubscriptionPlanUseCase implements IUseCase<DeleteDraftSubscriptionPlanCommand, Void> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    @Override
    @Transactional
    public Void execute(DeleteDraftSubscriptionPlanCommand input) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        // A published plan may already have been bought by schools, and even if not, the order line
        // (order_items.item_id) still points at this id -- a hard delete would orphan the documents. That column is
        // deliberately NOT a foreign key (polymorphic by order_items.type), so the DB does not block it. A DRAFT was
        // never sold, so deleting it outright is cleaner than leaving unused ARCHIVED garbage behind.
        if (plan.getStatus() != SubscriptionPlanStatus.DRAFT) {
            throw new IllegalArgumentException(
                "Only a plan in DRAFT status can be hard-deleted. A published plan must be archived instead of deleted.");
        }

        // Delete the quotas first, then the plan. subscription_plan_quotas.subscription_plan_id has NO foreign key to
        // the plan (the table only has a primary key), so nothing at the DB layer cleans up or blocks for us:
        // dropping this line leaves orphan quotas pointing at a plan that no longer exists.
        subscriptionPlanQuotaRepository.deleteBySubscriptionPlanId(plan.getId());
        subscriptionPlanRepository.deleteById(plan.getId());

        return null;
    }
}
