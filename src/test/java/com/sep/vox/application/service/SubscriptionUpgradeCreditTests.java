package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.SubscriptionUpgradePolicyService;
import com.sep.vox.domain.model.subscription.SchoolSubscription;

/**
 * The credit on upgrade must always leave an amount that can REALLY be paid.
 *
 * <p>A 0-dong order cannot be paid any more than a negative one: the gateway rejects it, the order sits PENDING until it expires, and
 * the school has no way to upgrade. The old cap was exactly the new plan's price so reaching 0 really happens --
 * no odd plan needed, just a school with a running period PLUS a paid queued period, because
 * calculateUnusedCredit deliberately credits every unfinished period.
 */
class SubscriptionUpgradeCreditTests {

    private static final BigDecimal MIN_PAYABLE_VND = new BigDecimal("1000");

    private final SubscriptionUpgradePolicyService policy = new SubscriptionUpgradePolicyService();
    private final Instant now = Instant.parse("2026-06-01T00:00:00Z");

    /**
     * The running period still has its full value + a queued period that has been fully paid: the
     * total unused portion clearly exceeds the new plan's price. The old version returned exactly
     * the cap, so the order came out at 0 VND.
     */
    @Test
    void should_leave_something_payable_when_unused_credit_exceeds_the_new_plan_price() {
        BigDecimal newPlanPrice = new BigDecimal("12000000");
        List<SchoolSubscription> unfinished = List.of(
            period(new BigDecimal("10000000"), now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(364))),
            queuedPeriod(new BigDecimal("10000000"), now.plus(Duration.ofDays(364)))
        );

        BigDecimal credit = policy.calculateUnusedCredit(unfinished, newPlanPrice, now);

        assertThat(newPlanPrice.subtract(credit))
            .as("amount still to be paid")
            .isGreaterThanOrEqualTo(MIN_PAYABLE_VND);
        assertThat(credit).isEqualByComparingTo(newPlanPrice.subtract(MIN_PAYABLE_VND));
    }

    /**
     * A credit below the cap is left untouched -- clamping only applies when the cap is really
     * reached.
     */
    @Test
    void should_not_touch_a_credit_that_is_already_below_the_cap() {
        BigDecimal newPlanPrice = new BigDecimal("12000000");
        // Half of a 6,000,000 plan has elapsed -> a credit of about 3,000,000, far from the cap.
        List<SchoolSubscription> unfinished = List.of(
            period(new BigDecimal("6000000"), now.minus(Duration.ofDays(182)), now.plus(Duration.ofDays(183)))
        );

        BigDecimal credit = policy.calculateUnusedCredit(unfinished, newPlanPrice, now);

        assertThat(credit).isLessThan(newPlanPrice.subtract(MIN_PAYABLE_VND));
        assertThat(credit).isGreaterThan(BigDecimal.ZERO);
    }

    /**
     * A plan cheaper than even the minimum: no credit at all and the school pays the full price --
     * still better than an order that cannot be paid.
     */
    @Test
    void should_give_no_credit_when_the_new_plan_costs_less_than_the_minimum_payable() {
        BigDecimal newPlanPrice = new BigDecimal("500");
        List<SchoolSubscription> unfinished = List.of(queuedPeriod(new BigDecimal("10000000"), now.plus(Duration.ofDays(300))));

        BigDecimal credit = policy.calculateUnusedCredit(unfinished, newPlanPrice, now);

        assertThat(credit).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /** No unfinished period means nothing to credit. */
    @Test
    void should_give_no_credit_when_there_is_nothing_unfinished() {
        assertThat(policy.calculateUnusedCredit(List.of(), new BigDecimal("12000000"), now))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private SchoolSubscription period(BigDecimal pricePaid, Instant start, Instant end) {
        SchoolSubscription subscription = new SchoolSubscription();
        subscription.setPricePaidSnapshot(pricePaid);
        subscription.setStartDate(start);
        subscription.setEndDate(end);
        return subscription;
    }

    /** A paid period that has not reached its start day -- the unused part equals the whole price paid. */
    private SchoolSubscription queuedPeriod(BigDecimal pricePaid, Instant start) {
        return period(pricePaid, start, start.plus(Duration.ofDays(365)));
    }
}
