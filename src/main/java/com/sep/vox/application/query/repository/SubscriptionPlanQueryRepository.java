package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Keeps only the questions that {@code SubscriptionPlanRepository} cannot answer because they must read ACROSS
 * another aggregate (school_subscriptions). Listing/paging plans is a pure read of one aggregate, so it
 * belongs to the domain repository and is not duplicated here.
 */
public interface SubscriptionPlanQueryRepository {

    /**
     * The ACTIVE plan used by the most schools -- the only source of the "most popular" flag.
     *
     * <p>Kept apart from the paged query DELIBERATELY: if the max were computed within the page, page 2
     * would label the highest plan OF THAT PAGE, not the highest in the whole system -- each page would
     * show a different "most popular plan". Computing it globally and comparing ids is the only way to make the label
     * independent of which page the user is on.
     *
     * <p>Returns {@code Optional.empty()} when no school has subscribed yet -- then no plan is labeled,
     * instead of giving the label to a plan with 0 customers.
     */
    Optional<UUID> findMostPopularPlanId();
}
