package com.sep.vox.infrastructure.persistence.query;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.repository.SubscriptionPlanQueryRepository;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaSubscriptionPlanQueryRepository implements SubscriptionPlanQueryRepository {

    /**
     * Counts the NUMBER OF SCHOOLS in use, not the number of payments: a replaced plan (replacedByPlanId) gets a new
     * id, so the payment history permanently stays on the old ARCHIVED plan -- the new plan would count 0 even if
     * everybody buys it. Counting the schools in use, the number follows automatically when a school renews onto
     * the new plan. A renewal is also not counted twice as it would be when counting transactions.
     *
     * <p>DISTINCT guards the case where a school accidentally has two ACTIVE rows on the same plan -- it still
     * counts as one customer. Only s.status = ACTIVE counts: a SUSPENDED school has lost its right to use the
     * service, so it is no longer evidence that the plan is popular.
     *
     * <p>ORDER BY must be EXHAUSTED down to a unique key (school count -> price -> id): if sorted only by school
     * count then, at the start, plans with 1 school each would tie and Postgres would return an arbitrary order --
     * the "most popular" label would jump to another plan on every page load. On a tie of school count the CHEAPER
     * plan wins, then the one created earlier; id is uuidv7 so "id ASC" means "oldest first" and, being unique,
     * it fixes the order completely.
     *
     * <p>JOIN (not LEFT JOIN) is deliberate: a plan with no school using it is dropped outright, and when nobody
     * in the system has subscribed yet no plan is labelled.
     */
    private static final String MOST_POPULAR_PLAN_QUERY = """
        SELECT p.id
        FROM SubscriptionPlanJpaEntity p
            JOIN SchoolSubscriptionJpaEntity s ON s.subscriptionPlanId = p.id
        WHERE p.status = :planStatus AND s.status = :subscriptionStatus
        GROUP BY p.id, p.priceVnd
        ORDER BY COUNT(DISTINCT s.schoolId) DESC, p.priceVnd ASC, p.id ASC
        """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<UUID> findMostPopularPlanId() {
        return em.createQuery(MOST_POPULAR_PLAN_QUERY, UUID.class)
            .setParameter("planStatus", SubscriptionPlanStatus.ACTIVE.name())
            .setParameter("subscriptionStatus", SchoolSubscriptionStatus.ACTIVE.name())
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst();
    }
}
