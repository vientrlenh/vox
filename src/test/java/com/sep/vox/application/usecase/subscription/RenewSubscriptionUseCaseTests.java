package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.sep.vox.application.port.input.command.RenewSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolDebtNotificationService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.subscription.RenewSubscriptionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;

/**
 * Đường gia hạn THỦ CÔNG (đối xứng với InvoiceSettlementService.renewSubscription, đường cổng thanh
 * toán) -- tập trung vào transition SchoolDebtCleared: gói mới luôn có SchoolSubscriptionQuotaRecord tinh khôi
 * nên chắc chắn hết nợ, chỉ cần biết gói CŨ có đang khóa hay không.
 */
class RenewSubscriptionUseCaseTests {

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private SubscriptionPlanQuotaRepository planQuotaRepository;
    private SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private FinancialEventRepository financialEventRepository;
    private UserContextPort userContextPort;
    private SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;
    private SchoolDebtNotificationService schoolDebtNotificationService;
    private RenewSubscriptionUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID newSubscriptionId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("5000000");

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        planQuotaRepository = mock(SubscriptionPlanQuotaRepository.class);
        subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        financialEventRepository = mock(FinancialEventRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolSubscriptionDebtGuardService = mock(SchoolSubscriptionDebtGuardService.class);
        schoolDebtNotificationService = mock(SchoolDebtNotificationService.class);

        useCase = new RenewSubscriptionUseCase(
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

        var plan = new SubscriptionPlan(
            planId, "Gói Trường", null, amount, 365, 60, PlanStatus.ACTIVE, 1, Instant.now(), null, null, new BigDecimal("0.20")
        );
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(planQuotaRepository.findAllByPlanId(planId)).thenReturn(List.of(
            new SubscriptionPlanQuota(planId, QuotaType.GRADING, BigDecimal.valueOf(100), new BigDecimal("1000"))
        ));
        when(schoolSubscriptionRepository.save(any(SchoolSubscription.class))).thenAnswer(call -> {
            SchoolSubscription saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(newSubscriptionId);
            }
            return saved;
        });
        when(subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(newSubscriptionId, QuotaType.GRADING))
            .thenReturn(Optional.of(new SchoolSubscriptionQuotaRecord(UUID.randomUUID(), newSubscriptionId, QuotaType.GRADING,
                BigDecimal.valueOf(100), BigDecimal.ZERO)));
    }

    private void givenActiveSubscription(boolean locked) {
        var current = new SchoolSubscription(
            subscriptionId, schoolId, planId, LocalDate.now().minusDays(360), LocalDate.now(),
            SubscriptionStatus.ACTIVE, amount, null, Instant.now(), 0L, null, null, null
        );
        when(schoolSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(current));
        when(schoolSubscriptionDebtGuardService.isQuotaOverLimit(subscriptionId, QuotaType.GRADING)).thenReturn(locked);
    }

    @Test
    void publishesDebtClearedWhenSubscriptionWasLocked() {
        givenActiveSubscription(true);

        useCase.execute(new RenewSubscriptionCommand(schoolId, subscriptionId));

        verify(schoolDebtNotificationService).publishSchoolDebtCleared(
            eq(newSubscriptionId), eq(schoolId), eq(QuotaType.GRADING), any(), any(), any());
    }

    @Test
    void doesNotPublishDebtClearedWhenSubscriptionWasNotLocked() {
        givenActiveSubscription(false);

        var result = useCase.execute(new RenewSubscriptionCommand(schoolId, subscriptionId));

        assertThat(result).isNotNull();
        verify(schoolDebtNotificationService, never()).publishSchoolDebtCleared(any(), any(), any(), any(), any(), any());
    }

    @Test
    void throwsWhenSubscriptionIsSuspended() {
        var current = new SchoolSubscription(
            subscriptionId, schoolId, planId, LocalDate.now().minusDays(360), LocalDate.now(),
            SubscriptionStatus.SUSPENDED, amount, null, Instant.now(), 0L, Instant.now(), "Gian lận", UUID.randomUUID()
        );
        when(schoolSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> useCase.execute(new RenewSubscriptionCommand(schoolId, subscriptionId)))
            .isInstanceOf(IllegalStateException.class);

        verify(schoolSubscriptionRepository, never()).save(any(SchoolSubscription.class));
    }
}
