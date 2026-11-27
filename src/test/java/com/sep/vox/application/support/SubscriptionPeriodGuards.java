package com.sep.vox.application.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * A subscription-period guard that "always lets through", for tests that do NOT check the period rule.
 *
 * <p>This guard sits on every path that creates/edits an assessment, so every test has to build it.
 * It uses the real guard on a plan that spans 100 years instead of an empty mock: if the guard later
 * changes how it computes the boundary, the test still runs through the real code path. A test that
 * wants to examine the period rule itself mocks {@link SubscriptionPeriodGuardService} and uses
 * {@code doThrow} (see {@code CreateClassTestSetupTests}), while the date boundary is covered
 * separately in {@code SubscriptionPeriodGuardServiceTests}.
 */
public final class SubscriptionPeriodGuards {

    private SubscriptionPeriodGuards() {}

    public static SubscriptionPeriodGuardService alwaysWithinPeriod() {
        SchoolSubscription subscription = new SchoolSubscription();
        subscription.setId(UUID.randomUUID());
        // ChronoUnit.YEARS/MONTHS cannot be used with Instant (throws
        // UnsupportedTemporalTypeException): an Instant has no calendar, so it does not know how
        // long a year is. Count in days -- the point here is only "a window wide enough that every
        // test falls inside it", so leap-year accuracy does not matter.
        subscription.setStartDate(Instant.now().minus(50L * 365, ChronoUnit.DAYS));
        subscription.setEndDate(Instant.now().plus(50L * 365, ChronoUnit.DAYS));

        SchoolSubscriptionRepository repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveBySchoolId(any())).thenReturn(Optional.of(subscription));
        return new SubscriptionPeriodGuardService(repository);
    }
}
