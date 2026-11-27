package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.SubscriptionPlanResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.subscription.SchoolSubscriptionRenewalPreviewResponse;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Lets the school preview which plan will be used on renewal, and when the new period starts.
 *
 * <p>Needed because the plan in use may have been archived and given a replacement plan -- in that case the
 * renewal automatically moves to the new plan. The school confirms by sending renewalPlan.id back as acceptedPlanId
 * when placing the order (RenewSchoolSubscriptionUseCase), instead of being silently switched to another plan.
 *
 * <p>Takes NO parameter: the school comes from the token, and the period to renew is the school's own most
 * recent one. If a subscriptionId came from outside, {@code hasRole('SCHOOL_ADMIN')} would not be enough to
 * protect it -- it answers "is this a school admin", not "is this period OF THIS SCHOOL" -- so an ownership check
 * would have to be built into the use case. With no id accepted, there is nothing to check.
 *
 * <p>That is also why there is no System Admin branch: this is an action of the school. An admin who wants to see
 * the state of a school's plan uses the subscription management screen, not this path.
 */
@Service
@RequiredArgsConstructor
public class PreviewSchoolSubscriptionRenewalUseCase
        implements IUseCase<Void, SchoolSubscriptionRenewalPreviewResponse> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanResolver subscriptionPlanResolver;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public SchoolSubscriptionRenewalPreviewResponse execute(Void input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();

        // The MOST RECENT period, with no status filter: a late renewal (the plan expired last week) is the most common
        // path, and there is no ACTIVE period left to ask about there.
        SchoolSubscription subscription = schoolSubscriptionRepository.findMostRecentBySchoolId(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "The school has never subscribed to a plan; choose a plan from the list instead of renewing."));

        SubscriptionPlan currentPlan = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        // Throws if the plan is archived and has no replacement plan -- intended: the school must learn right away
        // that it cannot renew yet, instead of seeing a pretty preview and then failing at the order step.
        SubscriptionPlan renewalPlan = subscriptionPlanResolver.resolveActivePlan(currentPlan);

        Instant now = Instant.now();
        Instant startsAt = schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now).stream()
            .findFirst()
            .map(s -> s.getEndDate())
            .orElse(now);

        return SchoolSubscriptionRenewalPreviewResponse.toResponse(currentPlan, renewalPlan, startsAt);
    }
}
