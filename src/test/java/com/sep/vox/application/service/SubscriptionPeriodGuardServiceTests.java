package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * The open/close window of an assessment must fall inside the school's subscription period. The easiest mistake is the upper bound:
 * {@code school_subscriptions.end_date} is a {@code timestamptz} and EXCLUSIVE -- it is the EXACT MOMENT
 * the period ends, not "the last day it can be used".
 *
 * <p>Before V2 this column was a {@code date} and was checked with {@code CURRENT_DATE BETWEEN start_date AND
 * end_date}, i.e. INCLUSIVE by DAY, so the guard had to add one day to get the upper bound. That addition
 * is wrong now: it lets an assessment schedule spill 24 hours past the plan period, and the assessment only dies at real run time.
 * The native queries in {@code SpringDataSchoolSubscriptionRepository} were also changed to
 * {@code end_date > CURRENT_TIMESTAMP} to match.
 */
class SubscriptionPeriodGuardServiceTests {

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPeriodGuardService guard;

    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        guard = new SubscriptionPeriodGuardService(schoolSubscriptionRepository);
        // end_date is the REAL and EXCLUSIVE END moment, not "the last day it can be used": a one-year plan
        // bought on 01/01/2026 00:00 has endDate = 01/01/2027 00:00, because SubscriptionPlan.endDateFrom
        // adds the period straight onto the start moment and KEEPS the time. The old fixture set endDate = 31/12 00:00,
        // simulating the LocalDate era when this column was a DAY value -- that shape can no longer
        // be produced by the real purchase flow.
        givenSubscription(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));
    }

    @Test
    void should_pass_when_window_inside_subscription_period() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-06-01T07:00"), at("2026-06-01T09:00")))
            .doesNotThrowAnyException();
    }

    /** INCLUSIVE lower bound: 00:00 exactly on start_date must still pass. */
    @Test
    void should_pass_when_open_at_is_first_moment_of_start_date() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-01-01T00:00"), at("2026-01-01T09:00")))
            .doesNotThrowAnyException();
    }

    /** EXCLUSIVE upper bound: right before the end moment must still pass. */
    @Test
    void should_pass_when_close_at_is_last_moment_before_end() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-12-31T07:00"), at("2026-12-31T23:59")))
            .doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_open_at_before_subscription_start() {
        assertThatThrownBy(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2025-12-31T23:00"), at("2026-01-02T09:00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Opening time")
            .hasMessageContaining("01/01/2026")
            .hasMessageContaining("01/01/2027");
    }

    @Test
    void should_reject_when_close_at_after_subscription_end() {
        assertThatThrownBy(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-06-01T07:00"), at("2027-01-05T09:00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Closing time")
            .hasMessageContaining("01/01/2027");
    }

    /** Exactly the end moment is already outside the period -- the EXCLUSIVE upper bound. */
    @Test
    void should_reject_when_close_at_is_exactly_end_instant() {
        assertThatThrownBy(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-12-31T07:00"), at("2027-01-01T00:00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Closing time");
    }

    /**
     * The same kind of error as the existing plan-based guards (AssessmentTimeQuotaGuardService,
     * ClassTestTokenQuotaGuardService) so the FE can tell "an error caused by the plan" from "an error caused by data entry".
     */
    @Test
    void should_reject_when_school_has_no_active_subscription() {
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-06-01T07:00"), at("2026-06-01T09:00")))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("active subscription");
    }

    /**
     * An older centralized assessment may still have one side null; the guard must not become the place that forces input
     * (that belongs to the use case), it only inspects the values that really exist.
     */
    @Test
    void should_ignore_null_dates() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(schoolId, null, null))
            .doesNotThrowAnyException();
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(schoolId, at("2026-06-01T07:00"), null))
            .doesNotThrowAnyException();
    }

    @Test
    void should_skip_when_school_id_is_null() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            null, at("2020-01-01T07:00"), at("2030-01-01T09:00")))
            .doesNotThrowAnyException();
    }

    /**
     * A plan with null start_date/end_date is corrupt data, not "unlimited" -- but blocking
     * here would freeze every operation of that school over a data error unrelated to the end
     * user, so the constraint is skipped instead of throwing.
     */
    @Test
    void should_skip_when_subscription_has_no_period() {
        givenSubscription(null, null);

        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2020-01-01T07:00"), at("2030-01-01T09:00")))
            .doesNotThrowAnyException();
    }

    /** null = the plan has no period yet -- the guard must skip, not blow up. */
    private static Instant atStartOfDayVn(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneConstant.BUSINESS_ZONE).toInstant();
    }

    private void givenSubscription(LocalDate startDate, LocalDate endDate) {
        SchoolSubscription subscription = new SchoolSubscription();
        subscription.setId(UUID.randomUUID());
        subscription.setSchoolId(schoolId);
        // V2 section 19: these two columns are now INSTANTS, no longer dates. Convert in exactly
        // the zone the guard uses; otherwise the boundary points in the test are off by 7 hours.
        subscription.setStartDate(atStartOfDayVn(startDate));
        subscription.setEndDate(atStartOfDayVn(endDate));
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));
    }

    /** Vietnam local time -- the same zone the guard uses to convert start_date/end_date. */
    private Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
    }
}
