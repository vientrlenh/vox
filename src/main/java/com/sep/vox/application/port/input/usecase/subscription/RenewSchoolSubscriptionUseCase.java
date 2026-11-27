package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.order.CreateSubscriptionOrderCommand;
import com.sep.vox.application.port.input.command.subscription.RenewSchoolSubscriptionCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.order.CreateSubscriptionOrderUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Renewal: places an order to buy one more cycle of the plan the school is using.
 *
 * <p>This use case does NOT create the order itself -- it only does the part specific to renewal (find the plan in
 * use, follow the replacement chain, check the school's confirmation) and hands over to
 * {@link CreateSubscriptionOrderUseCase}. Renewal deliberately has NO OrderType of its own and NO settlement
 * branch of its own: to the system, "buy one more cycle" is identical to "buy for the first time" -- see
 * CreateSubscriptionOrderUseCase.orderTypeFor. Duplicating the ordering logic here would only create a copy that
 * drifts away from the original.
 *
 * <p>This also replaces the old RenewSubscriptionUseCase, which created the new subscription directly without any
 * gateway confirming the money had been collected, and deleted the old quotas itself -- exactly the "buy tokens,
 * renew, lose everything" bug that this whole refactor was born to fix. Now the money must arrive first, and the
 * self-funded balance sits in the school-level pool so no period can touch it.
 */
@Service
@RequiredArgsConstructor
public class RenewSchoolSubscriptionUseCase implements IUseCase<RenewSchoolSubscriptionCommand, UUID> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanResolver subscriptionPlanResolver;
    private final CreateSubscriptionOrderUseCase createSubscriptionOrderUseCase;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(RenewSchoolSubscriptionCommand input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();

        SchoolSubscription subscription = schoolSubscriptionRepository.findMostRecentBySchoolId(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "The school has never subscribed to a plan; choose a plan from the list instead of renewing."));

        SubscriptionPlan currentPlan = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        SubscriptionPlan renewalPlan = subscriptionPlanResolver.resolveActivePlan(currentPlan);

        // Check against the plan the school CONFIRMED on the preview screen. This is not a formality: between the moment
        // the school looks at that screen and the moment it clicks renew, a System Admin can perfectly well archive the
        // plan or change its replacement -- and since renewal follows the replacement chain by itself, without this
        // step the school would pay for a plan completely different from the one it just saw.
        if (!renewalPlan.getId().equals(input.acceptedPlanId())) {
            throw new IllegalArgumentException(
                "The renewal plan has changed since you viewed it; please review the renewal information again before continuing.");
        }

        // From here on identical to a new purchase: the plan must be on sale, the school not suspended, and no
        // subscription order left unfinished. All of that already lives in CreateSubscriptionOrderUseCase.
        return createSubscriptionOrderUseCase.execute(new CreateSubscriptionOrderCommand(renewalPlan.getId()));
    }
}
