package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UnsuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.subscription.UnsuspendSubscriptionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionEventType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Gỡ đình chỉ trả gói về ACTIVE và xóa trắng ba cột suspended_*.
 *
 * <p>Vì xóa trắng nên THỨ TỰ là một phần của hành vi: sổ audit phải được ghi TRƯỚC lệnh save, nếu
 * không thì lý do đình chỉ đã biến mất trước khi có chỗ chép lại nó.
 */
class UnsuspendSubscriptionUseCaseTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionEventRepository schoolSubscriptionEventRepository;
    private SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private UserContextPort userContextPort;
    private UnsuspendSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        schoolSubscriptionEventRepository = mock(SchoolSubscriptionEventRepository.class);
        suspensionNotificationService = mock(SchoolSubscriptionSuspensionNotificationService.class);
        userContextPort = mock(UserContextPort.class);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        when(schoolSubscriptionRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        useCase = new UnsuspendSubscriptionUseCase(
            schoolSubscriptionRepository,
            schoolSubscriptionEventRepository,
            suspensionNotificationService,
            userContextPort);
    }

    @Test
    void should_restore_active_and_clear_the_suspension_columns() {
        givenSubscription(SchoolSubscriptionStatus.SUSPENDED);

        var result = useCase.execute(new UnsuspendSubscriptionCommand(SUBSCRIPTION_ID, "Đã xác minh lại"));

        assertThat(result).isEqualTo(SUBSCRIPTION_ID);
        verify(schoolSubscriptionRepository).save(argThat(saved ->
            saved.getStatus() == SchoolSubscriptionStatus.ACTIVE
                && saved.getSuspendedAt() == null
                && saved.getSuspendedReason() == null
                && saved.getSuspendedBy() == null));
        verify(suspensionNotificationService).publishUnsuspended(any(), any(), any());
    }

    /** Ghi sổ trước, xóa cột sau -- đảo lại thì lý do đình chỉ mất trước khi kịp lưu. */
    @Test
    void should_write_the_audit_entry_before_wiping_the_reason() {
        givenSubscription(SchoolSubscriptionStatus.SUSPENDED);

        useCase.execute(new UnsuspendSubscriptionCommand(SUBSCRIPTION_ID, null));

        var order = inOrder(schoolSubscriptionEventRepository, schoolSubscriptionRepository);
        order.verify(schoolSubscriptionEventRepository).save(argThat(event ->
            event.getEventType() == SchoolSubscriptionEventType.UNSUSPENDED
                && ADMIN_ID.equals(event.getActorId())));
        order.verify(schoolSubscriptionRepository).save(any());
    }

    /** Ghi chú không bắt buộc: gỡ đình chỉ là trả lại quyền, không phải tước đi. */
    @Test
    void should_accept_a_missing_note() {
        givenSubscription(SchoolSubscriptionStatus.SUSPENDED);

        assertThat(useCase.execute(new UnsuspendSubscriptionCommand(SUBSCRIPTION_ID, null)))
            .isEqualTo(SUBSCRIPTION_ID);
    }

    @Test
    void should_reject_when_the_subscription_is_not_suspended() {
        givenSubscription(SchoolSubscriptionStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.execute(new UnsuspendSubscriptionCommand(SUBSCRIPTION_ID, null)))
            .isInstanceOf(IllegalStateException.class);
        verify(schoolSubscriptionRepository, never()).save(any());
        verify(schoolSubscriptionEventRepository, never()).save(any());
    }

    @Test
    void should_reject_when_the_subscription_does_not_exist() {
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UnsuspendSubscriptionCommand(SUBSCRIPTION_ID, null)))
            .isInstanceOf(NotFoundException.class);
    }

    private void givenSubscription(SchoolSubscriptionStatus status) {
        var subscription = new SchoolSubscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setSchoolId(SCHOOL_ID);
        subscription.setStatus(status);
        subscription.setPricePaidSnapshot(BigDecimal.TEN);
        subscription.setStartDate(Instant.now().minus(30, ChronoUnit.DAYS));
        subscription.setEndDate(Instant.now().plus(300, ChronoUnit.DAYS));
        if (status == SchoolSubscriptionStatus.SUSPENDED) {
            subscription.setSuspendedAt(Instant.now().minus(1, ChronoUnit.DAYS));
            subscription.setSuspendedReason("Phát hiện gian lận");
            subscription.setSuspendedBy(ADMIN_ID);
        }
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
    }
}
