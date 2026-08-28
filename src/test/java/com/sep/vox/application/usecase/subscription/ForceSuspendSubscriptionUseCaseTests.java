package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ForceSuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.subscription.ForceSuspendSubscriptionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionEventType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Đình chỉ là cưỡng chế cắt quyền dùng NGAY, nên ba điều kiện bao quanh nó mới là phần đáng test:
 * phải có lý do, chỉ áp cho gói đang ACTIVE, và KHÔNG được cắt giữa ca thi đang diễn ra.
 *
 * <p>Sổ audit bền vững là SchoolSubscriptionEvent chứ không còn FinancialEvent -- ba cột suspended_*
 * trên chính gói bị xóa trắng lúc gỡ đình chỉ nên chỉ phản ánh lần đình chỉ ĐANG hiệu lực.
 */
class ForceSuspendSubscriptionUseCaseTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final String REASON = "Phát hiện gian lận";

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionEventRepository schoolSubscriptionEventRepository;
    private SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private ExamScheduleRepository examScheduleRepository;
    private ForceSuspendSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        schoolSubscriptionEventRepository = mock(SchoolSubscriptionEventRepository.class);
        suspensionNotificationService = mock(SchoolSubscriptionSuspensionNotificationService.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        var userContextPort = mock(UserContextPort.class);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        when(examScheduleRepository.findBySchoolId(SCHOOL_ID)).thenReturn(List.of());
        when(schoolSubscriptionRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        useCase = new ForceSuspendSubscriptionUseCase(
            schoolSubscriptionRepository,
            schoolSubscriptionEventRepository,
            suspensionNotificationService,
            examScheduleRepository,
            userContextPort);
    }

    @Test
    void should_suspend_and_record_who_did_it_and_why() {
        givenSubscription(SchoolSubscriptionStatus.ACTIVE);

        var result = useCase.execute(new ForceSuspendSubscriptionCommand(SUBSCRIPTION_ID, REASON));

        assertThat(result).isEqualTo(SUBSCRIPTION_ID);
        verify(schoolSubscriptionRepository).save(argThat(saved ->
            saved.getStatus() == SchoolSubscriptionStatus.SUSPENDED
                && REASON.equals(saved.getSuspendedReason())
                && ADMIN_ID.equals(saved.getSuspendedBy())
                && saved.getSuspendedAt() != null));
        verify(schoolSubscriptionEventRepository).save(argThat(event ->
            event.getEventType() == SchoolSubscriptionEventType.SUSPENDED
                && REASON.equals(event.getReason())
                && ADMIN_ID.equals(event.getActorId())));
        verify(suspensionNotificationService).publishSuspended(any(), any(), any(), any());
    }

    @Test
    void should_reject_a_blank_reason() {
        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SUBSCRIPTION_ID, "   ")))
            .isInstanceOf(IllegalArgumentException.class);
        verify(schoolSubscriptionRepository, never()).save(any());
    }

    @Test
    void should_reject_when_the_subscription_is_not_active() {
        givenSubscription(SchoolSubscriptionStatus.EXPIRED);

        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SUBSCRIPTION_ID, REASON)))
            .isInstanceOf(IllegalStateException.class);
        verify(schoolSubscriptionRepository, never()).save(any());
    }

    @Test
    void should_reject_when_the_subscription_does_not_exist() {
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SUBSCRIPTION_ID, REASON)))
            .isInstanceOf(NotFoundException.class);
    }

    /**
     * Cắt quyền giữa ca thi làm hỏng bài của học sinh đang ngồi trong phòng -- lỗi không thuộc về các
     * em. Đình chỉ phải đợi hết ca.
     */
    @Test
    void should_refuse_to_cut_access_while_an_exam_is_running() {
        givenSubscription(SchoolSubscriptionStatus.ACTIVE);
        var ongoing = mock(ExamSchedule.class);
        when(ongoing.isOngoingAt(any())).thenReturn(true);
        when(examScheduleRepository.findBySchoolId(SCHOOL_ID)).thenReturn(List.of(ongoing));

        assertThatThrownBy(() -> useCase.execute(new ForceSuspendSubscriptionCommand(SUBSCRIPTION_ID, REASON)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ca thi");
        verify(schoolSubscriptionRepository, never()).save(any());
        verify(schoolSubscriptionEventRepository, never()).save(any());
    }

    private void givenSubscription(SchoolSubscriptionStatus status) {
        var subscription = new SchoolSubscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setSchoolId(SCHOOL_ID);
        subscription.setStatus(status);
        subscription.setPricePaidSnapshot(BigDecimal.TEN);
        subscription.setStartDate(Instant.now().minus(30, ChronoUnit.DAYS));
        subscription.setEndDate(Instant.now().plus(300, ChronoUnit.DAYS));
        when(schoolSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
    }
}
