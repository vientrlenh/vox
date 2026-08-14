package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import com.sep.vox.application.port.input.command.ApproveRequestCommand;
import com.sep.vox.application.port.input.service.SchoolDebtNotificationService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.subscription.ApproveRequestUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.RequestType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

/**
 * Đường duyệt gói THỦ CÔNG (đối xứng với InvoiceSettlementService.approveSubscriptionRequest, đường
 * cổng thanh toán) -- tập trung vào transition SchoolDebtCleared: gói mới luôn có SubscriptionQuota
 * tinh khôi nên chắc chắn hết nợ, chỉ cần biết gói CŨ có đang khóa hay không.
 */
class ApproveRequestUseCaseTests {

    private SubscriptionRequestRepository subscriptionRequestRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private PlanQuotaRepository planQuotaRepository;
    private SubscriptionQuotaRepository subscriptionQuotaRepository;
    private FinancialEventRepository financialEventRepository;
    private UserContextPort userContextPort;
    private SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;
    private SchoolDebtNotificationService schoolDebtNotificationService;
    private ApproveRequestUseCase useCase;

    private final UUID requestId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID newSubscriptionId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("5000000");

    @BeforeEach
    void setUp() {
        subscriptionRequestRepository = mock(SubscriptionRequestRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        planQuotaRepository = mock(PlanQuotaRepository.class);
        subscriptionQuotaRepository = mock(SubscriptionQuotaRepository.class);
        financialEventRepository = mock(FinancialEventRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolSubscriptionDebtGuardService = mock(SchoolSubscriptionDebtGuardService.class);
        schoolDebtNotificationService = mock(SchoolDebtNotificationService.class);

        useCase = new ApproveRequestUseCase(
            subscriptionRequestRepository,
            schoolSubscriptionRepository,
            subscriptionPlanRepository,
            planQuotaRepository,
            subscriptionQuotaRepository,
            financialEventRepository,
            userContextPort,
            schoolSubscriptionDebtGuardService,
            schoolDebtNotificationService
        );

        when(userContextPort.isSystemAdmin()).thenReturn(true);

        var request = new SubscriptionRequest(
            requestId, schoolId, RequestType.REGISTRATION, null, planId,
            amount, RequestStatus.PENDING, Instant.now(), null, null
        );
        var plan = new SubscriptionPlan(
            planId, "Gói Trường", null, amount, 365, 60, 500, PlanStatus.ACTIVE, 1, Instant.now(), null, null, new BigDecimal("0.20")
        );
        when(subscriptionRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(subscriptionRequestRepository.save(any(SubscriptionRequest.class))).thenAnswer(call -> call.getArgument(0));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(planQuotaRepository.findAllByPlanId(planId)).thenReturn(List.of(
            new PlanQuota(planId, QuotaType.GRADING, BigDecimal.valueOf(100), new BigDecimal("1000"))
        ));
        when(schoolSubscriptionRepository.save(any(SchoolSubscription.class))).thenAnswer(call -> {
            SchoolSubscription saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(newSubscriptionId);
            }
            return saved;
        });
        // Quota tinh khôi của gói mới -- reportDebtClearedIfNeeded đọc lại quota này để biết snapshot
        // sau khi hết nợ.
        when(subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(newSubscriptionId, QuotaType.GRADING))
            .thenReturn(Optional.of(new SubscriptionQuota(UUID.randomUUID(), newSubscriptionId, QuotaType.GRADING,
                BigDecimal.valueOf(100), BigDecimal.ZERO)));
    }

    private void givenExistingActiveSubscription(boolean locked) {
        var existingId = UUID.randomUUID();
        var existing = new SchoolSubscription(
            existingId, schoolId, planId, LocalDate.now().minusDays(360), LocalDate.now(),
            SubscriptionStatus.ACTIVE, amount, null, Instant.now(), 0L
        );
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(existing));
        when(schoolSubscriptionDebtGuardService.isQuotaOverLimit(existingId, QuotaType.GRADING)).thenReturn(locked);
    }

    @Test
    void publishesDebtClearedWhenSchoolHadNoActiveSubscriptionBeforeButNowLockedNever() {
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        useCase.execute(new ApproveRequestCommand(requestId));

        verify(schoolDebtNotificationService, never()).publishSchoolDebtCleared(any(), any(), any(), any(), any(), any());
    }

    @Test
    void publishesDebtClearedWhenPreviousSubscriptionWasLocked() {
        givenExistingActiveSubscription(true);

        useCase.execute(new ApproveRequestCommand(requestId));

        verify(schoolDebtNotificationService).publishSchoolDebtCleared(
            eq(newSubscriptionId), eq(schoolId), eq(QuotaType.GRADING), any(), any(), any());
    }

    @Test
    void doesNotPublishDebtClearedWhenPreviousSubscriptionWasNotLocked() {
        givenExistingActiveSubscription(false);

        var result = useCase.execute(new ApproveRequestCommand(requestId));

        assertThat(result).isNotNull();
        verify(schoolDebtNotificationService, never()).publishSchoolDebtCleared(any(), any(), any(), any(), any(), any());
    }
}
