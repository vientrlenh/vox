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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UnsuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.subscription.UnsuspendSubscriptionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

class UnsuspendSubscriptionUseCaseTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private FinancialEventRepository financialEventRepository;
    private SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private UserContextPort userContextPort;
    private UnsuspendSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        financialEventRepository = mock(FinancialEventRepository.class);
        suspensionNotificationService = mock(SchoolSubscriptionSuspensionNotificationService.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new UnsuspendSubscriptionUseCase(
            schoolSubscriptionRepository, financialEventRepository, suspensionNotificationService, userContextPort);

        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        when(schoolSubscriptionRepository.save(any(SchoolSubscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private SchoolSubscription suspendedSubscription() {
        return new SchoolSubscription(
            SUBSCRIPTION_ID, SCHOOL_ID, UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30),
            SubscriptionStatus.SUSPENDED, BigDecimal.TEN, null, Instant.now(), 0L,
            Instant.now(), "Phát hiện gian lận", ADMIN_ID
        );
    }

    @Test
    void unsuspends_backToActive_andClearsSuspensionFields() {
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(suspendedSubscription()));

        var result = useCase.execute(new UnsuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, "Xác minh không gian lận"));

        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE.name());
        assertThat(result.suspendedAt()).isNull();
        assertThat(result.suspendedReason()).isNull();
        verify(financialEventRepository).save(argThat(event ->
            event.getEventType() == FinancialEventType.SUB_UNSUSPENDED
                && "Xác minh không gian lận".equals(event.getPayload())));
        verify(suspensionNotificationService).publishUnsuspended(eq(SUBSCRIPTION_ID), eq(SCHOOL_ID), any());
    }

    @Test
    void unsuspends_withoutNote() {
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(suspendedSubscription()));

        var result = useCase.execute(new UnsuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, null));

        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE.name());
    }

    @Test
    void rejects_whenCallerIsNotSystemAdmin() {
        when(userContextPort.isSystemAdmin()).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new UnsuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, null)))
            .isInstanceOf(ForbiddenException.class);
        verify(schoolSubscriptionRepository, never()).save(any());
    }

    @Test
    void rejects_whenSubscriptionNotFound() {
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UnsuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, null)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejects_whenSubscriptionNotSuspended() {
        var active = new SchoolSubscription(
            SUBSCRIPTION_ID, SCHOOL_ID, UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30),
            SubscriptionStatus.ACTIVE, BigDecimal.TEN, null, Instant.now(), 0L, null, null, null
        );
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> useCase.execute(new UnsuspendSubscriptionCommand(SCHOOL_ID, SUBSCRIPTION_ID, null)))
            .isInstanceOf(IllegalStateException.class);
        verify(schoolSubscriptionRepository, never()).save(any());
    }
}
