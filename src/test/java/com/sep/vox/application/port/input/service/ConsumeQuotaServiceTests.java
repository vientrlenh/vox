package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.output.QuotaDebtConfigPort;
import com.sep.vox.application.port.output.QuotaUsageWarningConfigPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolAiSpendEntry;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolAiSpendEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * The EARLY warning (checkUsageWarningTransition) must fire exactly once, when used/total CROSSES from
 * below the threshold to above it -- the same crossing-detection technique as checkDebtCapTransition
 * (which has no test of its own in the repo, see the javadoc of ConsumeQuotaService).
 */
class ConsumeQuotaServiceTests {

    private static final BigDecimal WARNING_RATIO = new BigDecimal("0.70");

    private SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository quotaUserAllocationRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private SchoolQuotaUsageNotificationService schoolQuotaUsageNotificationService;
    private SchoolAiSpendEntryRepository schoolAiSpendEntryRepository;
    private ConsumeQuotaService service;

    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentSessionId = UUID.randomUUID();
    private final UUID quotaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        quotaRecordRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        quotaUserAllocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);
        SchoolBalanceEntryRepository schoolBalanceEntryRepository = mock(SchoolBalanceEntryRepository.class);
        SchoolDebtNotificationService schoolDebtNotificationService = mock(SchoolDebtNotificationService.class);
        QuotaDebtConfigPort quotaDebtConfig = mock(QuotaDebtConfigPort.class);
        schoolQuotaUsageNotificationService = mock(SchoolQuotaUsageNotificationService.class);
        QuotaUsageWarningConfigPort quotaUsageWarningConfig = mock(QuotaUsageWarningConfigPort.class);
        when(quotaUsageWarningConfig.warningRatio()).thenReturn(WARNING_RATIO);
        schoolAiSpendEntryRepository = mock(SchoolAiSpendEntryRepository.class);

        service = new ConsumeQuotaService(
            quotaRecordRepository,
            quotaUserAllocationRepository,
            schoolSubscriptionRepository,
            schoolBalanceRepository,
            schoolBalanceEntryRepository,
            schoolDebtNotificationService,
            quotaDebtConfig,
            schoolQuotaUsageNotificationService,
            quotaUsageWarningConfig,
            schoolAiSpendEntryRepository);

        SchoolSubscription subscription = new SchoolSubscription();
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

        service.consumeAssessmentAllowingDebt(subscriptionId, assessmentSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        verify(schoolQuotaUsageNotificationService).publishQuotaUsageWarning(
            eq(subscriptionId), eq(schoolId), eq(QuotaType.ASSESSMENT),
            eq(BigDecimal.valueOf(100_000)), eq(BigDecimal.valueOf(71_000)), any());
    }

    @Test
    void should_not_publish_again_when_usage_was_already_past_threshold() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(71_000));

        service.consumeAssessmentAllowingDebt(subscriptionId, assessmentSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        verify(schoolQuotaUsageNotificationService, never()).publishQuotaUsageWarning(
            any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_not_publish_when_charge_stays_below_threshold() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(50_000));

        service.consumeAssessmentAllowingDebt(subscriptionId, assessmentSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        verify(schoolQuotaUsageNotificationService, never()).publishQuotaUsageWarning(
            any(), any(), any(), any(), any(), any());
    }

    /**
     * The AI cost ledger records the FULL amount, even when it fits within the quota and produces
     * no wallet entry. That is what sets it apart from {@code school_balance_entries} -- that
     * ledger deliberately only receives the overspend, so building a spending chart from it would
     * miss the largest part.
     */
    @Test
    void should_record_the_full_amount_even_when_it_fits_inside_the_quota() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000));

        service.consumeAssessmentAllowingDebt(subscriptionId, assessmentSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        SchoolAiSpendEntry entry = captureSpendEntry();
        assertThat(entry.getAmountVnd()).isEqualByComparingTo(BigDecimal.valueOf(2_000));
        assertThat(entry.getSchoolId()).isEqualTo(schoolId);
        assertThat(entry.getAssessmentSessionId()).isEqualTo(assessmentSessionId);
        assertThat(entry.getQuotaType()).isEqualTo(QuotaType.ASSESSMENT);
    }

    /**
     * A centralized assessment belongs to nobody's spending ceiling -- {@code CompleteAssessmentSessionGradingUseCase} deliberately
     * passes null. null in the user column is an ANSWER, and the "who is spending" table relies on exactly that
     * to separate the whole school's expenses from the ranking.
     */
    @Test
    void should_keep_a_school_wide_charge_unattributed() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000));

        service.consumeAssessmentAllowingDebt(subscriptionId, assessmentSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        assertThat(captureSpendEntry().getUserId()).isNull();
    }

    @Test
    void should_attribute_a_class_test_charge_to_the_teacher_who_created_it() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000));
        UUID teacherId = UUID.randomUUID();

        service.consumeAssessmentAllowingDebt(subscriptionId, assessmentSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, teacherId);

        assertThat(captureSpendEntry().getUserId()).isEqualTo(teacherId);
    }

    /**
     * The practice path must leave a trace just like the assessment path. Before V10 it left nothing with
     * a timestamp, so "how much did practice cost last month" was a question nobody could answer,
     * not even by querying by hand.
     */
    @Test
    void should_record_practice_spending_too() {
        SchoolSubscriptionQuotaRecord practiceQuota = SchoolSubscriptionQuotaRecord.builder()
            .id(quotaId)
            .schoolSubscriptionId(subscriptionId)
            .quotaType(QuotaType.PRACTICE)
            .totalAllocatedAmountVnd(BigDecimal.valueOf(100_000))
            .usedAmountVnd(BigDecimal.ZERO)
            .build();
        when(quotaRecordRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.PRACTICE))
            .thenReturn(Optional.of(practiceQuota));
        when(quotaRecordRepository.findById(quotaId)).thenReturn(Optional.of(practiceQuota));
        when(quotaRecordRepository.tryConsume(eq(quotaId), any())).thenReturn(true);
        UUID practiceSessionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        service.consumePracticeAllowingDebt(subscriptionId, practiceSessionId, BigDecimal.valueOf(500),
            BigDecimal.ONE, BigDecimal.TEN, studentId);

        SchoolAiSpendEntry entry = captureSpendEntry();
        assertThat(entry.getQuotaType()).isEqualTo(QuotaType.PRACTICE);
        assertThat(entry.getPracticeSessionId()).isEqualTo(practiceSessionId);
        assertThat(entry.getAssessmentSessionId()).isNull();
        assertThat(entry.getUserId()).isEqualTo(studentId);
    }

    /** The CHECK constraint rejects a 0-VND row -- block it in Java instead of letting the DB throw in the middle of the grading flow. */
    @Test
    void should_not_record_a_zero_charge() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000));

        service.consumeAssessmentAllowingDebt(subscriptionId, assessmentSessionId, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.TEN, null);

        verify(schoolAiSpendEntryRepository, never()).save(any());
    }

    private SchoolAiSpendEntry captureSpendEntry() {
        ArgumentCaptor<SchoolAiSpendEntry> captor = ArgumentCaptor.forClass(SchoolAiSpendEntry.class);
        verify(schoolAiSpendEntryRepository).save(captor.capture());
        return captor.getValue();
    }

    private void givenQuota(BigDecimal totalAllocatedVnd, BigDecimal usedVnd) {
        SchoolSubscriptionQuotaRecord quota = SchoolSubscriptionQuotaRecord.builder()
            .id(quotaId)
            .schoolSubscriptionId(subscriptionId)
            .quotaType(QuotaType.ASSESSMENT)
            .totalAllocatedAmountVnd(totalAllocatedVnd)
            .usedAmountVnd(usedVnd)
            .build();
        when(quotaRecordRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT))
            .thenReturn(Optional.of(quota));
        when(quotaRecordRepository.findById(quotaId)).thenReturn(Optional.of(quota));
        when(quotaRecordRepository.tryConsume(eq(quotaId), any())).thenReturn(true);
    }
}
