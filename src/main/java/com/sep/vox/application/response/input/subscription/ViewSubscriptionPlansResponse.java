package com.sep.vox.application.response.input.subscription;

import com.sep.vox.domain.dto.SubscriptionPlanDto;

/**
 * The plan's quotas are NOT here: the GraphQL resolver loads them through a DataLoader, so a client that
 * does not select the quotas field costs no extra query.
 *
 * <p>As for "most popular", it does not belong to {@link SubscriptionPlanDto} because it is not a property
 * of the plan -- it is the result of comparing this plan with the others, meaningful only in the context of a list.
 */
public record ViewSubscriptionPlansResponse(
    SubscriptionPlanDto subscription,
    boolean isMostPopular
) {

}
