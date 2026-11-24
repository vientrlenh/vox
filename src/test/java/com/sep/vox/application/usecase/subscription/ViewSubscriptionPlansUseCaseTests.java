package com.sep.vox.application.usecase.subscription;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.query.subscription.ViewSubscriptionPlansQuery;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionPlansUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SubscriptionPlanQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@ExtendWith(MockitoExtension.class)
class ViewSubscriptionPlansUseCaseTests {

    private static final ViewSubscriptionPlansQuery QUERY = new ViewSubscriptionPlansQuery(1, 10);
    private static final PageResult<SubscriptionPlan> EMPTY_PAGE = new PageResult<>(List.of(), 1, 10, 0, 0);

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock
    private SubscriptionPlanQueryRepository subscriptionPlanQueryRepository;
    @Mock
    private UserContextPort userContextPort;

    @InjectMocks
    private ViewSubscriptionPlansUseCase useCase;

    @Test
    void should_show_only_active_plans_to_an_anonymous_caller_without_asking_for_the_role() {
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.empty());
        when(subscriptionPlanRepository.findByStatus(SubscriptionPlanStatus.ACTIVE, 1, 10)).thenReturn(EMPTY_PAGE);
        when(subscriptionPlanQueryRepository.findMostPopularPlanId()).thenReturn(Optional.empty());

        useCase.execute(QUERY);

        verify(userContextPort, never()).isSystemAdmin();
        verify(subscriptionPlanRepository, never()).findAll(1, 10);
    }

    @Test
    void should_show_archived_plans_too_to_a_system_admin() {
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(subscriptionPlanRepository.findAll(1, 10)).thenReturn(EMPTY_PAGE);
        when(subscriptionPlanQueryRepository.findMostPopularPlanId()).thenReturn(Optional.empty());

        useCase.execute(QUERY);

        verify(subscriptionPlanRepository, never()).findByStatus(SubscriptionPlanStatus.ACTIVE, 1, 10);
    }
}
