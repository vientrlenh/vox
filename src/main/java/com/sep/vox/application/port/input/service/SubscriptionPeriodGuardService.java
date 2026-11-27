package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * The assessment open/close window of centralized assessments and class tests must fall inside the school's purchased subscription period.
 *
 * <p>Without this constraint a school can schedule an assessment outside the plan period, and it only breaks at real run time
 * (ClassTestTokenQuotaGuardService / StartClassTestSessionUseCase) -- i.e. after the teacher
 * has already prepared the paper and arranged the students.
 *
 * <p>Deliberately compares directly with {@code start_date}/{@code end_date} instead of trusting
 * {@code status = ACTIVE}: demoting ACTIVE → EXPIRED is done by {@code SubscriptionExpiryJob}, which runs hourly,
 * so right after a plan expires there is still a window in which the repository returns the old plan.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionPeriodGuardService {

    // Instant.toString() prints UTC with a trailing Z, so a plan period starting 01/01 shows as
    // "2025-12-31T17:00:00Z" -- one day off from the number on the subscription management screen, exactly
    // when the reader is trying to compare the two places.
    private static final DateTimeFormatter DISPLAY_DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneConstant.BUSINESS_ZONE);

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;

    /**
     * @param openAt  skipped when null -- making it mandatory belongs to the use case, not to this guard
     * @param closeAt skipped when null, same reason
     */
    public void requireWithinSubscriptionPeriod(UUID schoolId, Instant openAt, Instant closeAt) {
        if (schoolId == null || (openAt == null && closeAt == null)) {
            return;
        }

        SchoolSubscription subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new BusinessRuleViolationException(
                "The school has no active subscription, so the assessment time cannot be set"));

        Instant startDate = subscription.getStartDate();
        Instant endDate = subscription.getEndDate();
        // A plan without dates is corrupt data, not "unlimited" -- but blocking here would freeze the whole school
        // over a data error the end user cannot fix.
        if (startDate == null || endDate == null) {
            return;
        }

        requireInsidePeriod(subscription, startDate, endDate, openAt, closeAt);
    }

    private void requireInsidePeriod(
            SchoolSubscription subscription, Instant startDate, Instant endDate, Instant openAt, Instant closeAt) {
        // end_date is an EXCLUSIVE bound, exactly the moment the period ends -- it matches "end_date >
        // CURRENT_TIMESTAMP" in the native queries and "endDate > :at" in findInForceBySchoolId.
        //
        // Do NOT add one more day. The old version added it because end_date used to be a LocalDate,
        // a DAY-level bound whose real upper limit was the end of that day. V2 changed the column to
        // timestamptz and the model to Instant, so end_date is now exactly the moment the period
        // expires; adding 24 hours would let a schedule fall OUTSIDE the plan period -- the schedule
        // would be accepted and then fail at run time in ClassTestTokenQuotaGuardService, the very
        // late failure this guard exists to prevent.
        if (openAt != null && isOutside(openAt, startDate, endDate)) {
            throw new IllegalArgumentException(outsidePeriodMessage("Opening time", subscription));
        }
        if (closeAt != null && isOutside(closeAt, startDate, endDate)) {
            throw new IllegalArgumentException(outsidePeriodMessage("Closing time", subscription));
        }
    }

    private boolean isOutside(Instant value, Instant lowerBound, Instant upperBoundExclusive) {
        return value.isBefore(lowerBound) || !value.isBefore(upperBoundExclusive);
    }

    /**
     * Displays in Vietnam date-time instead of {@code Instant.toString()} (UTC, with a Z suffix) -- users compare it
     * with the plan expiry shown on the subscription management screen, which is also a local date.
     */
    private String outsidePeriodMessage(String fieldLabel, SchoolSubscription subscription) {
        return "%s must be within the school's subscription period (from %s to %s)".formatted(
            fieldLabel,
            DISPLAY_DATE.format(subscription.getStartDate()),
            DISPLAY_DATE.format(subscription.getEndDate()));
    }

}
