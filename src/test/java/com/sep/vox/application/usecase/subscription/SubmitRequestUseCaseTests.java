package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.SubmitRequestCommand;
import com.sep.vox.application.port.input.usecase.subscription.SubmitRequestUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.RequestType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

/**
 * Chặn gửi yêu cầu đăng ký/nâng cấp mới khi trường đang bị System Admin cưỡng chế đình chỉ -- tránh
 * vòng qua ApproveRequestUseCase âm thầm tạo gói ACTIVE mới mà không qua UnsuspendSubscriptionUseCase.
 */
class SubmitRequestUseCaseTests {

    private SubscriptionRequestRepository subscriptionRequestRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private UserContextPort userContextPort;
    private SubmitRequestUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("5000000");

    @BeforeEach
    void setUp() {
        subscriptionRequestRepository = mock(SubscriptionRequestRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new SubmitRequestUseCase(
            subscriptionRequestRepository,
            subscriptionPlanRepository,
            schoolSubscriptionRepository,
            userContextPort
        );

        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(schoolSubscriptionRepository.findAllBySchoolId(schoolId)).thenReturn(List.of());

        var plan = new SubscriptionPlan(
            planId, "Gói Trường", null, amount, 365, 60, PlanStatus.ACTIVE, 1, Instant.now(), null, null, new BigDecimal("0.20")
        );
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(subscriptionRequestRepository.save(any(SubscriptionRequest.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void submitsRequestWhenSchoolHasNoSuspendedSubscription() {
        var result = useCase.execute(new SubmitRequestCommand(schoolId, RequestType.REGISTRATION, null, planId));

        assertThat(result).isNotNull();
        verify(subscriptionRequestRepository).save(any(SubscriptionRequest.class));
    }

    @Test
    void throwsWhenSchoolHasSuspendedSubscription() {
        var suspended = new SchoolSubscription(
            UUID.randomUUID(), schoolId, planId, LocalDate.now().minusDays(30), LocalDate.now().plusDays(335),
            SubscriptionStatus.SUSPENDED, amount, null, Instant.now(), 0L, Instant.now(), "Gian lận", UUID.randomUUID()
        );
        when(schoolSubscriptionRepository.findAllBySchoolId(schoolId)).thenReturn(List.of(suspended));

        assertThatThrownBy(() -> useCase.execute(new SubmitRequestCommand(schoolId, RequestType.REGISTRATION, null, planId)))
            .isInstanceOf(IllegalStateException.class);

        verify(subscriptionRequestRepository, never()).save(any(SubscriptionRequest.class));
    }
}
