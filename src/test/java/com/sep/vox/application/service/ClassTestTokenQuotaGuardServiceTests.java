package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.QuotaPricingService;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.SubscriptionQuotaUserAllocation;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;

/**
 * Ước lượng worst-case (duration × số thí sinh × maxAttempt) phải soi đủ 3 chỗ trừ quota thật của
 * CompleteExamSessionGradingUseCase/ConsumeQuotaUseCase: GRADING của trường, CLASS_TEST của trường,
 * và hạn mức cá nhân giáo viên (nếu có) -- chỉ 2 chỗ sau mới áp cho CLASS_TEST, không áp cho
 * CENTRALIZED vì CompleteExamSessionGradingUseCase chỉ trừ CLASS_TEST khi exam.getKind() là vậy.
 */
class ClassTestTokenQuotaGuardServiceTests {

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionQuotaRepository subscriptionQuotaRepository;
    private SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ClassTestTokenQuotaGuardService guard;

    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaRepository = mock(SubscriptionQuotaRepository.class);
        subscriptionQuotaUserAllocationRepository = mock(SubscriptionQuotaUserAllocationRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        var quotaPricingService = mock(QuotaPricingService.class);
        // Hệ số quy đổi = 1 để estimatedCostUsd trùng số với "estimatedTokens" cũ (duration × số thí
        // sinh × maxAttempt) -- giữ nguyên các giá trị test bên dưới thay vì phải tính lại theo USD thật.
        when(quotaPricingService.currentEstimatedCostPerExamSecondUsd()).thenReturn(BigDecimal.ONE);
        guard = new ClassTestTokenQuotaGuardService(
            schoolSubscriptionRepository,
            subscriptionQuotaRepository,
            subscriptionQuotaUserAllocationRepository,
            examCandidateRepository,
            quotaPricingService);

        var subscription = new SchoolSubscription();
        subscription.setId(subscriptionId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));
        when(examCandidateRepository.countByExamId(examId)).thenReturn(1L);
        // 3600s × 1 thí sinh × 1 lượt = 3600 token ước tính cho mọi test bên dưới.
        givenSchoolQuota(QuotaType.GRADING, 10_000, 0);
        givenSchoolQuota(QuotaType.CLASS_TEST, 10_000, 0);
        when(subscriptionQuotaUserAllocationRepository
            .findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.CLASS_TEST, teacherId))
            .thenReturn(Optional.empty());
    }

    @Test
    void should_skip_when_duration_not_set() {
        var exam = classTest(null);

        assertThatCode(() -> guard.requireWithinTokenQuota(exam)).doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_all_quotas_have_headroom() {
        var exam = classTest(3600);

        assertThatCode(() -> guard.requireWithinTokenQuota(exam)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_grading_quota_exceeded_even_for_centralized_exam() {
        givenSchoolQuota(QuotaType.GRADING, 100, 0);
        var exam = centralizedExam(3600);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(exam))
            .isInstanceOf(PlanLimitExceededException.class);
    }

    @Test
    void should_not_check_class_test_quota_for_centralized_exam() {
        // CLASS_TEST quota trống hạn mức, nhưng exam là CENTRALIZED nên không được check tới.
        givenSchoolQuota(QuotaType.CLASS_TEST, 0, 0);
        var exam = centralizedExam(3600);

        assertThatCode(() -> guard.requireWithinTokenQuota(exam)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_class_test_school_quota_exceeded() {
        givenSchoolQuota(QuotaType.CLASS_TEST, 100, 0);
        var exam = classTest(3600);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(exam))
            .isInstanceOf(PlanLimitExceededException.class);
    }

    @Test
    void should_reject_when_teacher_personal_allocation_exceeded() {
        when(subscriptionQuotaUserAllocationRepository
            .findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.CLASS_TEST, teacherId))
            .thenReturn(Optional.of(new SubscriptionQuotaUserAllocation(
                subscriptionId, QuotaType.CLASS_TEST, teacherId, BigDecimal.valueOf(100), BigDecimal.ZERO)));
        var exam = classTest(3600);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(exam))
            .isInstanceOf(PlanLimitExceededException.class);
    }

    @Test
    void should_pass_when_teacher_has_no_personal_allocation_row() {
        // Không có allocation riêng = không bị chặn theo cá nhân, chỉ cần đủ pool của trường.
        var exam = classTest(3600);

        assertThatCode(() -> guard.requireWithinTokenQuota(exam)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_no_active_subscription() {
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());
        var exam = classTest(3600);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(exam))
            .isInstanceOf(PlanLimitExceededException.class);
    }

    private void givenSchoolQuota(QuotaType type, int totalAllocated, int usedQuantity) {
        when(subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(subscriptionId, type))
            .thenReturn(Optional.of(new SubscriptionQuota(
                subscriptionId, type, BigDecimal.valueOf(totalAllocated), BigDecimal.valueOf(usedQuantity))));
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