package com.sep.vox.application.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
        var subscription = new SchoolSubscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStartDate(LocalDate.now().minusYears(50));
        subscription.setEndDate(LocalDate.now().plusYears(50));

        var repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveBySchoolId(any())).thenReturn(Optional.of(subscription));
        return new SubscriptionPeriodGuardService(repository);
    }
}
