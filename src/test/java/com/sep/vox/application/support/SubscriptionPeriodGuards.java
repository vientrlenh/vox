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
 * Guard hạn gói "luôn cho qua", dành cho test KHÔNG kiểm tra luật hạn gói.
 *
 * <p>Guard này nằm trên mọi đường tạo/sửa bài kiểm tra, nên test nào cũng phải dựng nó. Dùng guard
 * thật trên một gói rộng 100 năm thay vì mock rỗng: nếu sau này guard đổi cách tính biên thì test
 * vẫn chạy qua đúng đường code thật. Test nào muốn soi chính luật hạn gói thì mock
 * {@link SubscriptionPeriodGuardService} và {@code doThrow} (xem {@code CreateClassTestSetupTests}),
 * còn biên ngày được phủ riêng ở {@code SubscriptionPeriodGuardServiceTests}.
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
