package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

/**
 * Ước lượng worst-case (duration × số thí sinh × maxAttempt × giá/giây) phải soi đúng những chỗ mà
 * CompleteExamSessionGradingUseCase/ConsumeQuotaService sẽ trừ thật khi chấm xong: tiền cấp TRƯỜNG
 * (hạn mức kèm gói + ví tự nạp) và -- chỉ với bài trên lớp -- trần chi CÁ NHÂN của giáo viên ra đề.
 *
 * <p>Hai bất biến mà bộ test này giữ, vì cả hai đều đã từng sai:
 * <ul>
 *   <li>Ước lượng phải quy sang VND TRƯỚC khi so. So thẳng số USD với cột VND thì vế trái nhỏ hơn
 *       khoảng 26.000 lần, cửa chặn vẫn đứng đó nhưng không bao giờ đóng.</li>
 *   <li>Số dư ví tự nạp được cộng vào phần của TRƯỜNG (ConsumeQuotaService trừ phần vượt hạn mức
 *       sang đó) nhưng KHÔNG cộng vào trần cá nhân -- trần đó là giới hạn nội bộ, không phải túi
 *       tiền.</li>
 * </ul>
 */
class ClassTestTokenQuotaGuardServiceTests {

    // costPerExamSecondUsd = 1 nên "USD ước tính" bằng đúng số giây; tỷ giá 1.000 để một lần quên
    // quy đổi là lệch hẳn 3 chữ số chứ không trốn được sau một phép nhân với 1.
    private static final BigDecimal USD_TO_VND_RATE = BigDecimal.valueOf(1_000);
    private static final int EXAM_SECONDS = 3_600;
    // 3.600 giây × 1 thí sinh × 1 lượt × 1 USD/giây × 1.000 = 3.600.000đ cho mọi test bên dưới.
    private static final BigDecimal ESTIMATE_VND = BigDecimal.valueOf(3_600_000);

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private ExamCandidateRepository examCandidateRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private ClassTestTokenQuotaGuardService guard;

    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        subscriptionQuotaUserAllocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);
        var quotaPricingPort = mock(QuotaPricingPort.class);
        when(quotaPricingPort.currentEstimatedCostPerExamSecondUsd()).thenReturn(BigDecimal.ONE);
        when(quotaPricingPort.usdToVndRate()).thenReturn(USD_TO_VND_RATE);

        guard = new ClassTestTokenQuotaGuardService(
            schoolSubscriptionRepository,
            subscriptionQuotaRepository,
            subscriptionQuotaUserAllocationRepository,
            examCandidateRepository,
            schoolBalanceRepository,
            quotaPricingPort,
            new SchoolSubscriptionDebtGuardService(schoolBalanceRepository));

        var subscription = new SchoolSubscription();
        subscription.setId(subscriptionId);
        subscription.setSchoolId(schoolId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));
        when(examCandidateRepository.countByExamId(examId)).thenReturn(1L);
        givenSchoolQuota(10_000_000, 0);
        givenNoBalanceRow();
        givenNoUserAllocation();
    }

    @Test
    void should_skip_when_duration_not_set() {
        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(null))).doesNotThrowAnyException();
    }

    /**
     * Kỳ thi chưa có mã đề nào được RecalculateExamTimeDurationService ghi 0 (không phải null). Ước
     * tính 0 thì không có gì để soi -- đi tiếp sẽ ném "Không tìm thấy hạn mức" chỉ vì trường chưa cấu
     * hình quota, tức là chặn lên lịch vì một con số bằng 0. Cố ý bỏ hạn mức để test đỏ nếu guard chỉ
     * chấp null.
     */
    @Test
    void should_skip_when_duration_is_zero() {
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.EXAM))
            .thenReturn(Optional.empty());

        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(0))).doesNotThrowAnyException();
    }

    @Test
    void should_skip_when_duration_is_zero_and_school_has_no_active_subscription() {
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(0))).doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_school_quota_covers_the_estimate() {
        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(EXAM_SECONDS))).doesNotThrowAnyException();
    }

    /**
     * Hạn mức 100.000đ nằm GIỮA ước lượng tính bằng USD (3.600) và tính bằng VND (3.600.000). Bản cũ
     * so thẳng 3.600 với 100.000 nên cho qua; chỉ khi quy đổi trước lúc so thì mới chặn. Đây là con số
     * duy nhất phân biệt được hai hành vi, nên đừng đổi nó thành một hạn mức "rõ ràng là thiếu".
     */
    @Test
    void should_convert_estimate_to_vnd_before_comparing_with_quota() {
        givenSchoolQuota(100_000, 0);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(EXAM_SECONDS)))
            .isInstanceOf(PlanLimitExceededException.class);
    }

    @Test
    void should_reject_when_estimate_exceeds_quota_and_school_has_no_balance() {
        givenSchoolQuota(1_000_000, 0);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(EXAM_SECONDS)))
            .isInstanceOf(PlanLimitExceededException.class);
    }

    /**
     * Cạn hạn mức nhưng đã nạp tiền thì vẫn publish được: ConsumeQuotaService sẽ trừ phần vượt sang ví
     * tự nạp (bút toán OVERAGE_CHARGE), nên chặn ở đây là từ chối đúng khoản mà lúc chấm xong mình sẽ
     * thu tiền.
     */
    @Test
    void should_pass_when_top_up_balance_covers_the_part_beyond_quota() {
        givenSchoolQuota(1_000_000, 0);
        givenBalance(5_000_000);

        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(EXAM_SECONDS))).doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_estimate_exceeds_quota_and_balance_combined() {
        givenSchoolQuota(1_000_000, 0);
        givenBalance(1_000_000);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(EXAM_SECONDS)))
            .isInstanceOf(PlanLimitExceededException.class);
    }

    /**
     * Số dư âm = trường đang NỢ = bị khóa, chặn ngay cả khi hạn mức còn thừa dư (khoản nợ chẳng liên
     * quan gì tới ước lượng lần này). Khóa ở cấp TRƯỜNG nên áp cho cả bài tập trung.
     *
     * <p>Kiểm cả THÔNG ĐIỆP chứ không chỉ loại exception: nợ phải được báo là "đang bị khóa" với cách
     * gỡ riêng (thanh toán bù), không được rơi xuống cửa dưới rồi báo thành "hết hạn mức". Đó cũng là
     * lý do spendableSchoolFundsVnd kẹp số dư âm về 0 thay vì cộng thẳng -- cộng thẳng thì khoản nợ bị
     * trừ lần thứ hai vào hạn mức và người dùng được chỉ sai cách khắc phục.
     */
    @Test
    void should_reject_with_lock_reason_when_balance_is_negative() {
        givenSchoolQuota(10_000_000, 0);
        givenBalance(-9_000_000);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(centralizedExam(EXAM_SECONDS)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("khóa");
    }

    /**
     * Trần cá nhân là GIỚI HẠN nội bộ trường tự đặt, không phải túi tiền -- cộng số dư ví trường vào
     * đó sẽ xóa sạch chính cái trần vừa đặt ra. Ví trường thừa sức trả, giáo viên vẫn bị chặn.
     */
    @Test
    void should_not_add_school_balance_to_personal_allocation_ceiling() {
        givenBalance(50_000_000);
        givenUserAllocation(100_000, 0);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(EXAM_SECONDS)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("cá nhân");
    }

    @Test
    void should_pass_when_teacher_has_no_personal_allocation_row() {
        // Không có allocation riêng = không bị chặn theo cá nhân, chỉ cần trường đủ tiền.
        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(EXAM_SECONDS))).doesNotThrowAnyException();
    }

    @Test
    void should_not_check_personal_allocation_for_centralized_exam() {
        // Kỳ thi tập trung do nhà trường tổ chức, không thuộc túi riêng của ai.
        givenUserAllocation(0, 0);

        assertThatCode(() -> guard.requireWithinTokenQuota(centralizedExam(EXAM_SECONDS))).doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_no_active_subscription() {
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(EXAM_SECONDS)))
            .isInstanceOf(PlanLimitExceededException.class);
    }

    /** Chưa cấu hình ví EXAM khác hẳn "đã cấu hình nhưng còn 0đ" -- số dư ví tự nạp không lấp chỗ đó. */
    @Test
    void should_reject_when_quota_record_is_missing_even_with_balance() {
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.EXAM))
            .thenReturn(Optional.empty());
        givenBalance(50_000_000);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(EXAM_SECONDS)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("Không tìm thấy hạn mức");
    }

    /** Ước lượng phải là VND: cùng con số đó xuất hiện trên cảnh báo không chặn của estimateTokenQuota. */
    @Test
    void should_report_estimate_and_spendable_funds_in_vnd() {
        givenSchoolQuota(1_000_000, 0);
        givenBalance(5_000_000);

        var estimate = guard.estimateTokenQuota(classTest(EXAM_SECONDS));

        assertThat(estimate.estimatedCostVnd()).isEqualByComparingTo(ESTIMATE_VND);
        // 1.000.000 hạn mức + 5.000.000 số dư -- cảnh báo phải dùng chung thước với cửa chặn, nếu
        // không thì hoặc dọa người dùng về một bài vẫn publish được, hoặc để họ bấm rồi mới ăn lỗi.
        assertThat(estimate.remainingExamVnd()).isEqualByComparingTo(BigDecimal.valueOf(6_000_000));
        assertThat(estimate.wouldExceedExam()).isFalse();
    }

    /**
     * CENTRALIZED không có hạn mức cá nhân nên tiêu bao nhiêu cũng ăn thẳng vào ví chung -- người
     * bấm lên lịch không tự thấy hệ quả lên các giáo viên khác đang có hạn mức cá nhân riêng cho
     * CLASS_TEST. % tính trên phần hạn mức GÓI còn lại (không cộng ví tự nạp, khác remainingExamVnd).
     */
    @Test
    void should_compute_shared_pool_usage_ratio_and_teacher_count_for_centralized_estimate() {
        givenSchoolQuota(10_000_000, 1_000_000); // quotaOnlyRemaining = 9.000.000
        var teacherWithRoom = UUID.randomUUID();
        var teacherFullyUsed = UUID.randomUUID();
        when(subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.EXAM))
            .thenReturn(List.of(
                new SchoolSubscriptionQuotaUserAllocation(subscriptionId, QuotaType.EXAM, teacherWithRoom,
                    BigDecimal.valueOf(500_000), BigDecimal.valueOf(100_000)),
                new SchoolSubscriptionQuotaUserAllocation(subscriptionId, QuotaType.EXAM, teacherFullyUsed,
                    BigDecimal.valueOf(500_000), BigDecimal.valueOf(500_000))));

        var estimate = guard.estimateTokenQuota(centralizedExam(EXAM_SECONDS));

        // estimatedCostVnd = 3.600.000 / quotaOnlyRemaining = 9.000.000 = 0.4 (40%).
        assertThat(estimate.sharedPoolUsageRatio()).isEqualByComparingTo(BigDecimal.valueOf(0.4));
        // Chỉ teacherWithRoom còn dư (500.000 - 100.000 > 0) -- teacherFullyUsed đã dùng hết, không tính.
        assertThat(estimate.teachersWithUnusedPersonalAllocationCount()).isEqualTo(1);
    }

    /** Ví gói đã cạn/âm sẵn thì % vô nghĩa -- wouldExceedExam/schoolLocked đã đủ nói, thêm % chỉ gây rối. */
    @Test
    void should_return_null_shared_pool_insight_when_quota_pool_already_exhausted() {
        givenSchoolQuota(1_000_000, 1_000_000);

        var estimate = guard.estimateTokenQuota(centralizedExam(EXAM_SECONDS));

        assertThat(estimate.sharedPoolUsageRatio()).isNull();
        assertThat(estimate.teachersWithUnusedPersonalAllocationCount()).isNull();
    }

    /** CLASS_TEST đã tự thấy trần cá nhân của chính mình rồi -- không cần soi hiệu ứng domino. */
    @Test
    void should_not_compute_shared_pool_insight_for_class_test() {
        givenSchoolQuota(10_000_000, 1_000_000);

        var estimate = guard.estimateTokenQuota(classTest(EXAM_SECONDS));

        assertThat(estimate.sharedPoolUsageRatio()).isNull();
        assertThat(estimate.teachersWithUnusedPersonalAllocationCount()).isNull();
    }

    private void givenSchoolQuota(long totalAllocatedVnd, long usedVnd) {
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.EXAM))
            .thenReturn(Optional.of(new SchoolSubscriptionQuotaRecord(
                subscriptionId, QuotaType.EXAM, BigDecimal.valueOf(totalAllocatedVnd), BigDecimal.valueOf(usedVnd))));
    }

    private void givenUserAllocation(long allocatedVnd, long usedVnd) {
        when(subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.EXAM, teacherId))
            .thenReturn(Optional.of(new SchoolSubscriptionQuotaUserAllocation(
                subscriptionId, QuotaType.EXAM, teacherId,
                BigDecimal.valueOf(allocatedVnd), BigDecimal.valueOf(usedVnd))));
    }

    private void givenNoUserAllocation() {
        when(subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.EXAM, teacherId))
            .thenReturn(Optional.empty());
    }

    private void givenBalance(long balanceVnd) {
        when(schoolBalanceRepository.findBySchoolId(schoolId)).thenReturn(Optional.of(
            new SchoolBalance(schoolId, BigDecimal.valueOf(balanceVnd), Instant.now(), Instant.now())));
    }

    /** Chưa từng nạp = chưa có dòng số dư, cùng nghĩa với số dư 0 -- xem SchoolBalance.emptyFor. */
    private void givenNoBalanceRow() {
        when(schoolBalanceRepository.findBySchoolId(schoolId)).thenReturn(Optional.empty());
    }

    private Exam classTest(Integer examTimeDurationSecond) {
        var exam = baseExam(examTimeDurationSecond);
        exam.setKind(ExamKind.CLASS_TEST);
        return exam;
    }

    private Exam centralizedExam(Integer examTimeDurationSecond) {
        var exam = baseExam(examTimeDurationSecond);
        exam.setKind(ExamKind.CENTRALIZED);
        return exam;
    }

    private Exam baseExam(Integer examTimeDurationSecond) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setCreatedBy(teacherId);
        exam.setMaxAttempt(1);
        exam.setExamTimeDurationSecond(examTimeDurationSecond);
        return exam;
    }
}
