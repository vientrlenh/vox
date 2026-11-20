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
    private SchoolAiSpendEntryRepository schoolAiSpendEntryRepository;
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

    /**
     * Sổ chi phí AI ghi ĐỦ số tiền, kể cả khi khoản đó nằm gọn trong hạn mức và không sinh bút toán
     * ví nào. Đây là điểm phân biệt với {@code school_balance_entries} — sổ kia cố ý chỉ nhận phần
     * tiêu vượt, nên dựng biểu đồ chi tiêu từ nó sẽ thiếu đúng phần lớn nhất.
     */
    @Test
    void should_record_the_full_amount_even_when_it_fits_inside_the_quota() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000));

        service.consumeExamAllowingDebt(subscriptionId, examSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        var entry = captureSpendEntry();
        assertThat(entry.getAmountVnd()).isEqualByComparingTo(BigDecimal.valueOf(2_000));
        assertThat(entry.getSchoolId()).isEqualTo(schoolId);
        assertThat(entry.getExamSessionId()).isEqualTo(examSessionId);
        assertThat(entry.getQuotaType()).isEqualTo(QuotaType.EXAM);
    }

    /**
     * Kỳ thi tập trung không thuộc trần chi của ai — {@code CompleteExamSessionGradingUseCase} cố ý
     * truyền null. null ở cột người dùng là một CÂU TRẢ LỜI, và bảng "ai đang tiêu" dựa vào đúng nó
     * để tách khoản của cả trường ra khỏi bảng xếp hạng.
     */
    @Test
    void should_keep_a_school_wide_charge_unattributed() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000));

        service.consumeExamAllowingDebt(subscriptionId, examSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, null);

        assertThat(captureSpendEntry().getUserId()).isNull();
    }

    @Test
    void should_attribute_a_class_test_charge_to_the_teacher_who_created_it() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000));
        var teacherId = UUID.randomUUID();

        service.consumeExamAllowingDebt(subscriptionId, examSessionId, BigDecimal.valueOf(2_000),
            BigDecimal.ONE, BigDecimal.TEN, teacherId);

        assertThat(captureSpendEntry().getUserId()).isEqualTo(teacherId);
    }

    /**
     * Đường luyện nói phải để lại dấu vết y như đường thi. Trước V10 nó KHÔNG để lại gì có mốc thời
     * gian, nên "tháng trước luyện nói tốn bao nhiêu" là câu không ai trả lời được, kể cả bằng cách
     * truy vấn tay.
     */
    @Test
    void should_record_practice_spending_too() {
        var practiceQuota = new SchoolSubscriptionQuotaRecord(
            quotaId, subscriptionId, QuotaType.PRACTICE, BigDecimal.valueOf(100_000), BigDecimal.ZERO);
        when(quotaRecordRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.PRACTICE))
            .thenReturn(Optional.of(practiceQuota));
        when(quotaRecordRepository.findById(quotaId)).thenReturn(Optional.of(practiceQuota));
        when(quotaRecordRepository.tryConsume(eq(quotaId), any())).thenReturn(true);
        var practiceSessionId = UUID.randomUUID();
        var studentId = UUID.randomUUID();

        service.consumePracticeAllowingDebt(subscriptionId, practiceSessionId, BigDecimal.valueOf(500),
            BigDecimal.ONE, BigDecimal.TEN, studentId);

        var entry = captureSpendEntry();
        assertThat(entry.getQuotaType()).isEqualTo(QuotaType.PRACTICE);
        assertThat(entry.getPracticeSessionId()).isEqualTo(practiceSessionId);
        assertThat(entry.getExamSessionId()).isNull();
        assertThat(entry.getUserId()).isEqualTo(studentId);
    }

    /** Ràng buộc CHECK từ chối dòng 0 đồng — chặn ở Java thay vì để DB ném giữa luồng chấm bài. */
    @Test
    void should_not_record_a_zero_charge() {
        givenQuota(BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000));

        service.consumeExamAllowingDebt(subscriptionId, examSessionId, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.TEN, null);

        verify(schoolAiSpendEntryRepository, never()).save(any());
    }

    private SchoolAiSpendEntry captureSpendEntry() {
        var captor = ArgumentCaptor.forClass(SchoolAiSpendEntry.class);
        verify(schoolAiSpendEntryRepository).save(captor.capture());
        return captor.getValue();
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
