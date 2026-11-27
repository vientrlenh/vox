package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;
import java.time.Instant;

import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;

/**
 * What the school will get if it clicks renew right now -- seen BEFORE placing the order.
 *
 * <p>This screen exists for one reason: if the plan in use has been archived, renewing will AUTOMATICALLY switch
 * to the replacement plan (SubscriptionPlanResolver). The school must see and confirm that instead of having its plan
 * changed without knowing -- the id in {@code renewalPlan} is exactly what must be sent back as
 * {@code acceptedPlanId} when placing the order (see RenewSubscriptionUseCase).
 *
 * <p>There is NO {@code unusedCreditAmount} like the old version. It is a legacy of the old model, where renewing early
 * OVERWROTE the running period so the lost days had to be compensated. Now the new period CONTINUES from the old one, no day is
 * lost, so that number would be permanently 0 -- keeping an always-zero field only makes readers think
 * the system does some offsetting. {@code startsAt} says exactly what the school needs to know: pay today, when the new period
 * starts.
 *
 * @param planChanged true when the renewal plan differs from the plan in use -- the FE should highlight it, it is the thing most easily
 *                    clicked through
 * @param startsAt    "now" if the school has no period in effect; equal to the end date of the current
 *                    period if renewing early
 * @param amountDue   the price of the renewal plan, without the service fee -- the fee is added when placing the order
 */
public record SchoolSubscriptionRenewalPreviewResponse(
    boolean planChanged,
    SubscriptionPlanDto currentPlan,
    SubscriptionPlanDto renewalPlan,
    Instant startsAt,
    BigDecimal amountDue
) {

    public static SchoolSubscriptionRenewalPreviewResponse toResponse(
            SubscriptionPlan currentPlan, SubscriptionPlan renewalPlan, Instant startsAt) {
        return new SchoolSubscriptionRenewalPreviewResponse(
            !renewalPlan.getId().equals(currentPlan.getId()),
            SubscriptionPlanDto.toDto(currentPlan),
            SubscriptionPlanDto.toDto(renewalPlan),
            startsAt,
            renewalPlan.getPriceVnd()
        );
    }
}
