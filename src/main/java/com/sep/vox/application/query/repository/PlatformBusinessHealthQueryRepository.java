package com.sep.vox.application.query.repository;

import java.math.BigDecimal;
import java.time.Instant;

import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;

/**
 * Reads the BUSINESS health of the platform for the system admin dashboard: how many schools still have a plan, how
 * many are about to drop, and the AI cost price to set next to revenue.
 *
 * <p>This is a REPORT and not the write port of any aggregate -- the same mold as
 * {@link PlatformOperationalHealthQueryRepository}. It is gathered here instead of adding a pile of
 * {@code countBy...} to {@code SchoolSubscriptionRepository} and {@code SchoolBalanceRepository}: if every new
 * statistics screen stuffs another count into a business port, soon nobody can tell which one is a
 * business model and which only serves a chart.
 */
public interface PlatformBusinessHealthQueryRepository {

    /**
     * Takes an {@link Instant} and not a {@code LocalDate}: {@code school_subscriptions.start_date}
     * and {@code end_date} were changed to {@code timestamptz} by V2 (the "ALTER COLUMN start_date TYPE
     * timestamp with time zone" section), so comparing against a {@code date} would let Postgres coerce that
     * date to a midnight instant IN THE SESSION'S TIME ZONE -- a hidden dependency on the JDBC configuration,
     * exactly what {@code ZoneConstant} warns about. Comparing instant with instant leaves nothing to guess.
     *
     * @param now             the moment used to decide which subscription period is covering
     * @param expiringThrough the expiring-soon warning threshold, INCLUSIVE
     */
    SchoolSubscriptionHealthDto countSchoolSubscriptionHealth(Instant now, Instant expiringThrough);

    /**
     * Schools with a negative top-up wallet -- exactly the schools blocked from opening assessment sessions.
     *
     * <p>There is no lock flag to read: the locked state is DERIVED from the balance on every query, see
     * {@code SchoolBalance.isInDebt()} and {@code SchoolSubscriptionDebtGuardService}. The condition here
     * ({@code balance_vnd < 0}) must match that predicate, otherwise the dashboard would count a different
     * set of schools than the ones actually blocked.
     *
     * <p>CUTS ACROSS the subscription buckets: it reads from another table, so a school that still has a plan but a negative
     * wallet falls into both counts. Do not add this number to the other three buckets.
     */
    long countSchoolsInDebt();

    /**
     * The real AI cost price incurred in the period, converted to VND so it can sit next to revenue.
     *
     * <p>Uses {@code cost_vnd} and not {@code cost_usd}: revenue is recorded in VND, and subtracting two
     * different units gives a meaningless number. Does not filter on {@code charged_at} -- the profit margin
     * asks for the cost INCURRED, whether the school's money has been collected yet is a different matter.
     *
     * @param from inclusive
     * @param to   EXCLUSIVE, the same convention as {@code OrderRepository.sumTotalAmountByStatusInRange}
     * @return never null; a period with no records returns {@link BigDecimal#ZERO}
     */
    BigDecimal sumAiCostVnd(Instant from, Instant to);
}
