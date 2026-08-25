package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.subscription.ViewMyPracticeQuotaAllocationUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionQuotaUserAllocation;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;

class ViewMyPracticeQuotaAllocationUseCaseTests {

    private UserContextPort userContextPort;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private ViewMyPracticeQuotaAllocationUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaUserAllocationRepository = mock(SubscriptionQuotaUserAllocationRepository.class);
        useCase = new ViewMyPracticeQuotaAllocationUseCase(
            userContextPort, schoolSubscriptionRepository, subscriptionQuotaUserAllocationRepository);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        var subscription = new SchoolSubscription();
        subscription.setId(subscriptionId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));
    }

    @Test
    void should_return_allocation_when_row_exists() {
        when(subscriptionQuotaUserAllocationRepository
            .findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.PRACTICE, userId))
            .thenReturn(Optional.of(new SubscriptionQuotaUserAllocation(
                subscriptionId, QuotaType.PRACTICE, userId, BigDecimal.valueOf(1200), BigDecimal.valueOf(300))));

        var result = useCase.execute(null);

        assertThat(result).isNotNull();
        assertThat(result.allocatedQuantity()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        assertThat(result.usedQuantity()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    void should_return_null_when_no_allocation_row() {
        when(subscriptionQuotaUserAllocationRepository
            .findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.PRACTICE, userId))
            .thenReturn(Optional.empty());

        assertThat(useCase.execute(null)).isNull();
    }

    @Test
    void should_return_null_when_no_active_subscription() {
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThat(useCase.execute(null)).isNull();
    }

    @Test
    void should_return_null_when_caller_has_no_school() {
        when(userContextPort.getCurrentSchoolId()).thenReturn(null);

        assertThat(useCase.execute(null)).isNull();
    }
}