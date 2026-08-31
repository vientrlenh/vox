package com.sep.vox.application.port.input.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.output.QuotaDebtConfigPort;
import com.sep.vox.application.port.output.QuotaUsageWarningConfigPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Cảnh báo SỚM (checkUsageWarningTransition) phải chỉ bắn đúng 1 lần lúc used/total CHUYỂN từ dưới
 * ngưỡng sang vượt ngưỡng -- cùng kỹ thuật crossing-detection với checkDebtCapTransition (không có
 * test riêng trong repo, xem javadoc ConsumeQuotaService).
 */
class ConsumeQuotaServiceTests {

    private static final BigDecimal WARNING_RATIO = new BigDecimal("0.70");

    private SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository quotaUserAllocationRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private SchoolQuotaUsageNotificationService schoolQuotaUsageNotificationService;
    private ConsumeQuotaService service;

    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID examSessionId = UUID.randomUUID();
    private final UUID quotaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        quotaRecordRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        quotaUserAllocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);
        var schoolBalanceEntryRepository = mock(SchoolBalanceEntryRepository.class);
        var schoolDebtNotificationService = mock(SchoolDebtNotificationService.class);
        var quotaDebtConfig = mock(QuotaDebtConfigPort.class);
        schoolQuotaUsageNotificationService = mock(SchoolQuotaUsageNotificationService.class);
        var quotaUsageWarningConfig = mock(QuotaUsageWarningConfigPort.class);
        when(quotaUsageWarningConfig.warningRatio()).thenReturn(WARNING_RATIO);

        service = new ConsumeQuotaService(
            quotaRecordRepository,
            quotaUserAllocationRepository,
            schoolSubscriptionRepository,
            schoolBalanceRepository,
            schoolBalanceEntryRepository,
            schoolDebtNotificationService,
            quotaDebtConfig,
            schoolQuotaUsageNotificationService,
            quotaUsageWarningConfig);

        var subscription = new SchoolSubscription();
        subscription.setId(subscriptionId);
        subscription.setSchoolId(schoolId);
        when(schoolSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(schoolBalanceRepository.findBySchoolId(schoolId)).thenReturn(Optional.empty());
        when(quotaUserAllocationRepository.findBySchoolSubscriptionIdAndQuotaTypeAndUserId(any(), any(), any()))
            .thenReturn(Optional.empty());
    }

    @Test
    void should_publish_usage_warning_once_when_charge_crosses_threshold() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(69_000));

        service.consumeExamAllowingDebt(subscriptionId, examSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        verify(schoolQuotaUsageNotificationService).publishQuotaUsageWarning(
            eq(subscriptionId), eq(schoolId), eq(QuotaType.EXAM),
            eq(BigDecimal.valueOf(100_000)), eq(BigDecimal.valueOf(71_000)), any());
    }

    @Test
    void should_not_publish_again_when_usage_was_already_past_threshold() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(71_000));

        service.consumeExamAllowingDebt(subscriptionId, examSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        verify(schoolQuotaUsageNotificationService, never()).publishQuotaUsageWarning(
            any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_not_publish_when_charge_stays_below_threshold() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(50_000));

        service.consumeExamAllowingDebt(subscriptionId, examSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        verify(schoolQuotaUsageNotificationService, never()).publishQuotaUsageWarning(
            any(), any(), any(), any(), any(), any());
    }

    private void givenQuota(BigDecimal totalAllocatedVnd, BigDecimal usedVnd) {
        var quota = new SchoolSubscriptionQuotaRecord(
            quotaId, subscriptionId, QuotaType.EXAM, totalAllocatedVnd, usedVnd);
        when(quotaRecordRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.EXAM))
            .thenReturn(Optional.of(quota));
        when(quotaRecordRepository.findById(quotaId)).thenReturn(Optional.of(quota));
        when(quotaRecordRepository.tryConsume(eq(quotaId), any())).thenReturn(true);
    }
}
