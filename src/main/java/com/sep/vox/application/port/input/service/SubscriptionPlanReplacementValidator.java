package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;

/**
 * Whether a replacement plan is allowed to replace an archived plan.
 *
 * <p>The constraint here is unusually strict, and the reason is that the school HAS NO RIGHT TO REFUSE: at renewal,
 * SubscriptionPlanResolver follows replacedByPlanId on its own and the school receives the new plan without being asked. So
 * everything that decides "what the school gets for what it pays" must stay the same or get better --
 * otherwise a System Admin could unilaterally lower the service quality of a paying school.
 *
 * <p>Split out of ArchiveSubscriptionPlanUseCase because there are TWO entry paths that both need exactly this rule set: at
 * archiving time, and when editing the replacement plan after archiving (UpdateSubscriptionPlanReplacementUseCase).
 * Letting each one check on its own is how one gets fixed some day and the other forgotten.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionPlanReplacementValidator {

    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    public void requireValidReplacement(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        if (replacement.getId().equals(replaced.getId())) {
            throw new IllegalArgumentException("The replacement plan must not be the same as the plan being archived");
        }
        if (replacement.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("The replacement plan must be in active status");
        }

        requireCompatibleTerms(replacement, replaced);
    }

    /**
     * Only the terms-comparison part (period/price/duration/quota) -- does NOT require the replacement to be ACTIVE.
     *
     * <p>Split out for the "create replacement plan" flow (CreateSubscriptionPlanReplacementUseCase): the newly
     * created plan is still DRAFT so requireValidReplacement cannot be called right away; the terms are only settled at
     * publish time (PublishSubscriptionPlanUseCase), the moment the new plan really takes on the replacement role.
     */
    public void requireCompatibleTerms(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        requireSamePeriod(replacement, replaced);
        requireSamePrice(replacement, replaced);
        requireNotLessAttemptTime(replacement, replaced);
        requireNotLessQuota(replacement, replaced);
    }

    /**
     * Both the UNIT and the COUNT of the period. Without the unit side, a "12 DAY" plan could replace a
     * "12 MONTH" plan at the old price -- the school pays the full amount and receives 1/30 of the duration.
     */
    private void requireSamePeriod(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        // equals() rather than !=: periodCount is an Integer (boxed), so != compares REFERENCES. Right now
        // it still works only because Java caches Integers from -128..127 -- a plan with period 200 is wrong
        // immediately, with nothing to warn about it.
        if (replacement.getPeriodType() != replaced.getPeriodType()
                || !replacement.getPeriodCount().equals(replaced.getPeriodCount())) {
            throw new IllegalArgumentException(
                "The period of the replacement plan must be identical to the plan being replaced ("
                    + replaced.getPeriodCount() + " " + replaced.getPeriodType() + ")");
        }
    }

    private void requireSamePrice(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        if (replacement.getPriceVnd().compareTo(replaced.getPriceVnd()) != 0) {
            throw new IllegalArgumentException("The replacement plan must have exactly the same price as the plan being replaced");
        }
    }

    /** MORE is allowed: raising quality for the school needs no one's consent. */
    private void requireNotLessAttemptTime(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        if (replacement.getMaxTimePerAttemptMin() < replaced.getMaxTimePerAttemptMin()) {
            throw new IllegalArgumentException(
                "The maximum duration per attempt of the replacement plan must not be lower than the plan being replaced ("
                    + replaced.getMaxTimePerAttemptMin() + " minutes)");
        }
    }

    /**
     * The quota included in the plan is the main VALUE of a plan, so this is the easiest place to regress while the price still looks
     * identical. Checked per QuotaType: missing a whole type also counts as a regression, because a school using that
     * type would lose all its quota next period.
     */
    private void requireNotLessQuota(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        Map<QuotaType, BigDecimal> replacementQuotas = subscriptionPlanQuotaRepository
            .findBySubscriptionPlanId(replacement.getId()).stream()
            .collect(Collectors.toMap(q -> q.getQuotaType(), q -> q.getIncludedAmountVnd()));

        for (SubscriptionPlanQuota replacedQuota : subscriptionPlanQuotaRepository.findBySubscriptionPlanId(replaced.getId())) {
            BigDecimal replacementAmount = replacementQuotas.get(replacedQuota.getQuotaType());
            if (replacementAmount == null) {
                throw new IllegalArgumentException(
                    "The replacement plan lacks the quota " + replacedQuota.getQuotaType() + " that the old plan has");
            }
            if (replacementAmount.compareTo(replacedQuota.getIncludedAmountVnd()) < 0) {
                throw new IllegalArgumentException(
                    "Quota " + replacedQuota.getQuotaType() + " of the replacement plan is lower than the plan being replaced ("
                        + replacedQuota.getIncludedAmountVnd() + " VND)");
            }
        }
    }
}
