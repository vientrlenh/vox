package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ForceSuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.subscription.ForceSuspendSubscriptionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

class ForceSuspendSubscriptionUseCaseTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private FinancialEventRepository financialEventRepository;
    private SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private UserContextPort userContextPort;
    private ExamScheduleRepository examScheduleRepository;
    private ForceSuspendSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        financialEventRepository = mock(FinancialEventRepository.class);
        suspensionNotificationService = mock(SchoolSubscriptionSuspensionNotificationService.class);
        userContextPort = mock(UserContextPort.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);

        useCase = new ForceSuspendSubscriptionUseCase(
            schoolSubscriptionRepository, financialEventRepository, suspensionNotificationService, userContextPort,
            examScheduleRepository);

        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        when(schoolSubscriptionRepository.save(any(SchoolSubscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(examScheduleRepository.findBySchoolId(any())).thenReturn(List.of());
    }

    private ExamSchedule examSchedule(ExamScheduleStatus status, Instant startDate, Instant endDate) {
        return new ExamSchedule(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), startDate, endDate,
            status, null, Instant.now(), Instant.now(), ADMIN_ID, ADMIN_ID
        );
    }

    private SchoolSubscription activeSubscription() {
        return new SchoolSubscription(
            SUBSCRIPTION_ID, SCHOOL_ID, UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30),
            SubscriptionStatus.ACTIVE, BigDecimal.TEN, null, Instant.now(), 0L, null, null, null
        );
    }

    @Test
    void suspends_whenActiveAndReasonGiven() {
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(activeSubscription()));

        var result = useCase.execute(new ForceSuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, "Phát hiện gian lận"));

        assertThat(result.status()).isEqualTo(SubscriptionStatus.SUSPENDED.name());
        assertThat(result.suspendedReason()).isEqualTo("Phát hiện gian lận");
        assertThat(result.suspendedAt()).isNotNull();
        verify(financialEventRepository).save(argThat(event ->
            event.getEventType() == FinancialEventType.SUB_SUSPENDED
                && event.getPayload().equals("Phát hiện gian lận")
                && event.getActorId().equals(ADMIN_ID)));
        verify(suspensionNotificationService).publishSuspended(eq(SUBSCRIPTION_ID), eq(SCHOOL_ID), eq("Phát hiện gian lận"), any());
    }

    @Test
    void rejects_whenCallerIsNotSystemAdmin() {
        when(userContextPort.isSystemAdmin()).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, "lý do")))
            .isInstanceOf(ForbiddenException.class);
        verify(schoolSubscriptionRepository, never()).save(any());
    }

    @Test
    void rejects_whenReasonIsBlank() {
        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, "   ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");
        verify(schoolSubscriptionRepository, never()).save(any());
    }

    @Test
    void rejects_whenReasonIsNull() {
        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_whenSubscriptionNotFound() {
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, "lý do")))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejects_whenSubscriptionNotActive() {
        var expired = new SchoolSubscription(
            SUBSCRIPTION_ID, SCHOOL_ID, UUID.randomUUID(), LocalDate.now().minusDays(60), LocalDate.now().minusDays(1),
            SubscriptionStatus.EXPIRED, BigDecimal.TEN, null, Instant.now(), 0L, null, null, null
        );
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, "lý do")))
            .isInstanceOf(IllegalStateException.class);
        verify(schoolSubscriptionRepository, never()).save(any());
    }

    @Test
    void rejects_whenSchoolHasOngoingExamSchedule() {
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(activeSubscription()));
        var now = Instant.now();
        when(examScheduleRepository.findBySchoolId(SCHOOL_ID)).thenReturn(List.of(
            examSchedule(ExamScheduleStatus.PUBLISHED, now.minusSeconds(60), now.plusSeconds(600))));

        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, "lý do")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ca thi");
        verify(schoolSubscriptionRepository, never()).save(any());
    }

    @Test
    void suspends_whenSchoolOnlyHasEndedExamSchedule() {
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(activeSubscription()));
        var now = Instant.now();
        when(examScheduleRepository.findBySchoolId(SCHOOL_ID)).thenReturn(List.of(
            examSchedule(ExamScheduleStatus.PUBLISHED, now.minusSeconds(600), now.minusSeconds(60))));

        var result = useCase.execute(new ForceSuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, "lý do"));

        assertThat(result.status()).isEqualTo(SubscriptionStatus.SUSPENDED.name());
    }
}
