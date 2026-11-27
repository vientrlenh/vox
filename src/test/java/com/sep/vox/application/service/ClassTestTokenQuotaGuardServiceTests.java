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

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.response.input.assessment.AssessmentTokenEstimateResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

/**
 * The worst-case estimate (duration x number of candidates x maxAttempt x price/second) must check exactly the places that
 * CompleteAssessmentSessionGradingUseCase/ConsumeQuotaService will really deduct from when grading finishes: the SCHOOL's money
 * (plan quota + self-topped-up wallet) and -- for a class test only -- the PERSONAL spending ceiling of the teacher who authored the test.
 *
 * <p>Two invariants this suite holds, because both were once wrong:
 * <ul>
 *   <li>The estimate must be converted to VND BEFORE comparing. Comparing a USD number directly with a VND column makes the left side smaller by
 *       about 26,000 times, and the gate is still standing but never closes.</li>
 *   <li>The self-topped-up wallet balance is added to the SCHOOL's part (ConsumeQuotaService charges the part above the quota
 *       to it) but is NOT added to the personal ceiling -- that ceiling is an internal limit, not a pocket
 *       of money.</li>
 * </ul>
 */
class ClassTestTokenQuotaGuardServiceTests {

    // costPerAssessmentSecondUsd = 1 so the "estimated USD" equals exactly the number of seconds; the exchange rate is 1,000 so that a single missed
    // conversion is off by a full 3 digits and cannot hide behind a multiplication by 1.
    private static final BigDecimal USD_TO_VND_RATE = BigDecimal.valueOf(1_000);
    private static final int ASSESSMENT_SECONDS = 3_600;
    // 3,600 seconds × 1 candidate × 1 turn × 1 USD/second × 1,000 = 3,600,000 VND for every test below.
    private static final BigDecimal ESTIMATE_VND = BigDecimal.valueOf(3_600_000);

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private ClassTestTokenQuotaGuardService guard;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        subscriptionQuotaUserAllocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);
        QuotaPricingPort quotaPricingPort = mock(QuotaPricingPort.class);
        when(quotaPricingPort.currentEstimatedCostPerAssessmentSecondUsd()).thenReturn(BigDecimal.ONE);
        when(quotaPricingPort.usdToVndRate()).thenReturn(USD_TO_VND_RATE);

        guard = new ClassTestTokenQuotaGuardService(
            schoolSubscriptionRepository,
            subscriptionQuotaRepository,
            subscriptionQuotaUserAllocationRepository,
            assessmentCandidateRepository,
            schoolBalanceRepository,
            quotaPricingPort,
            new SchoolSubscriptionDebtGuardService(schoolBalanceRepository));

        SchoolSubscription subscription = new SchoolSubscription();
        subscription.setId(subscriptionId);
        subscription.setSchoolId(schoolId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));
        when(assessmentCandidateRepository.countByAssessmentId(assessmentId)).thenReturn(1L);
        givenSchoolQuota(10_000_000, 0);
        givenNoBalanceRow();
        givenNoUserAllocation();
    }

    @Test
    void should_skip_when_duration_not_set() {
        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(null))).doesNotThrowAnyException();
    }

    /**
     * An assessment with no paper variant yet has 0 written by RecalculateAssessmentTimeDurationService (not null). An estimate
     * of 0 has nothing to check -- continuing would throw "Quota not found" just because the school has not
     * configured quota, i.e. blocking scheduling over a number that is 0. The quota is deliberately dropped so the test goes red if the guard only
     * accepts null.
     */
    @Test
    void should_skip_when_duration_is_zero() {
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT))
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
        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(ASSESSMENT_SECONDS))).doesNotThrowAnyException();
    }

    /**
     * A quota of 100,000 VND sits BETWEEN the estimate in USD (3,600) and in VND (3,600,000). The old version
     * compared 3,600 straight against 100,000 and so let it pass; only converting before comparing blocks it. This is the only
     * number that tells the two behaviours apart, so do not change it into a quota that is "obviously insufficient".
     */
    @Test
    void should_convert_estimate_to_vnd_before_comparing_with_quota() {
        givenSchoolQuota(100_000, 0);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(ASSESSMENT_SECONDS)))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_reject_when_estimate_exceeds_quota_and_school_has_no_balance() {
        givenSchoolQuota(1_000_000, 0);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(ASSESSMENT_SECONDS)))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    /**
     * With the quota exhausted but money topped up, publishing is still allowed: ConsumeQuotaService will deduct the excess from the
     * self-funded balance (OVERAGE_CHARGE entry), so blocking here would reject exactly the amount we will charge when grading finishes.
     */
    @Test
    void should_pass_when_top_up_balance_covers_the_part_beyond_quota() {
        givenSchoolQuota(1_000_000, 0);
        givenBalance(5_000_000);

        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(ASSESSMENT_SECONDS))).doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_estimate_exceeds_quota_and_balance_combined() {
        givenSchoolQuota(1_000_000, 0);
        givenBalance(1_000_000);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(ASSESSMENT_SECONDS)))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    /**
     * A negative balance = the school is in DEBT = locked, blocked even if the quota has plenty left (the debt has nothing
     * to do with this estimate). The lock is at SCHOOL level so it applies to centralized assessments too.
     *
     * <p>Checks the MESSAGE as well as the exception type: debt must be reported as "locked" with its own remedy
     * (pay it off), and must not fall through to the gate below and be reported as "quota exhausted". That is also
     * why spendableSchoolFundsVnd clamps a negative balance to 0 instead of adding it directly -- added directly the debt would be
     * deducted a second time from the quota and the user pointed to the wrong remedy.
     */
    @Test
    void should_reject_with_lock_reason_when_balance_is_negative() {
        givenSchoolQuota(10_000_000, 0);
        givenBalance(-9_000_000);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(centralizedAssessment(ASSESSMENT_SECONDS)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("locked");
    }

    /**
     * The personal cap is an internal LIMIT the school sets itself, not a pocket of money -- adding the school balance to
     * it would wipe out the cap just set. The school balance can easily pay, yet the teacher is still blocked.
     */
    @Test
    void should_not_add_school_balance_to_personal_allocation_ceiling() {
        givenBalance(50_000_000);
        givenUserAllocation(100_000, 0);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(ASSESSMENT_SECONDS)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("personal");
    }

    @Test
    void should_pass_when_teacher_has_no_personal_allocation_row() {
        // No personal allocation = no personal blocking, the school only needs enough money.
        assertThatCode(() -> guard.requireWithinTokenQuota(classTest(ASSESSMENT_SECONDS))).doesNotThrowAnyException();
    }

    @Test
    void should_not_check_personal_allocation_for_centralized_assessment() {
        // A centralized assessment is run by the school, it does not belong to anyone's own pocket.
        givenUserAllocation(0, 0);

        assertThatCode(() -> guard.requireWithinTokenQuota(centralizedAssessment(ASSESSMENT_SECONDS))).doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_no_active_subscription() {
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(ASSESSMENT_SECONDS)))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    /** An ASSESSMENT wallet that is not configured is completely different from "configured but with 0 left" -- the self-topped-up wallet balance does not fill that gap. */
    @Test
    void should_reject_when_quota_record_is_missing_even_with_balance() {
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT))
            .thenReturn(Optional.empty());
        givenBalance(50_000_000);

        assertThatThrownBy(() -> guard.requireWithinTokenQuota(classTest(ASSESSMENT_SECONDS)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("of the subscription not found");
    }

    /**
     * The estimate must be in VND: the same number appears in the non-blocking warning of
     * estimateTokenQuota.
     */
    @Test
    void should_report_estimate_and_spendable_funds_in_vnd() {
        givenSchoolQuota(1_000_000, 0);
        givenBalance(5_000_000);

        AssessmentTokenEstimateResponse estimate = guard.estimateTokenQuota(classTest(ASSESSMENT_SECONDS));

        assertThat(estimate.estimatedCostVnd()).isEqualByComparingTo(ESTIMATE_VND);
        // 1,000,000 quota + 5,000,000 balance -- the warning must use the same yardstick as the
        // blocking gate; otherwise it either scares the user about a test that can still be
        // published, or lets them click and only then hit the error.
        assertThat(estimate.remainingAssessmentVnd()).isEqualByComparingTo(BigDecimal.valueOf(6_000_000));
        assertThat(estimate.wouldExceedAssessment()).isFalse();
    }

    /**
     * CENTRALIZED has no personal quota, so any spending goes straight into the shared wallet --
     * the person pressing schedule does not see the effect on other teachers who have their own
     * personal quota for CLASS_TEST. The percentage is computed on the remaining PLAN quota (top-up
     * wallet not included, unlike remainingAssessmentVnd).
     */
    @Test
    void should_compute_shared_pool_usage_ratio_and_teacher_count_for_centralized_estimate() {
        givenSchoolQuota(10_000_000, 1_000_000); // quotaOnlyRemaining = 9.000.000
        UUID teacherWithRoom = UUID.randomUUID();
        UUID teacherFullyUsed = UUID.randomUUID();
        when(subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT))
            .thenReturn(List.of(
                SchoolSubscriptionQuotaUserAllocation.builder()
                    .schoolSubscriptionId(subscriptionId)
                    .quotaType(QuotaType.ASSESSMENT)
                    .userId(teacherWithRoom)
                    .allocatedAmountVnd(BigDecimal.valueOf(500_000))
                    .usedAmountVnd(BigDecimal.valueOf(100_000))
                    .build(),
                SchoolSubscriptionQuotaUserAllocation.builder()
                    .schoolSubscriptionId(subscriptionId)
                    .quotaType(QuotaType.ASSESSMENT)
                    .userId(teacherFullyUsed)
                    .allocatedAmountVnd(BigDecimal.valueOf(500_000))
                    .usedAmountVnd(BigDecimal.valueOf(500_000))
                    .build()));

        AssessmentTokenEstimateResponse estimate = guard.estimateTokenQuota(centralizedAssessment(ASSESSMENT_SECONDS));

        // estimatedCostVnd = 3.600.000 / quotaOnlyRemaining = 9.000.000 = 0.4 (40%).
        assertThat(estimate.sharedPoolUsageRatio()).isEqualByComparingTo(BigDecimal.valueOf(0.4));
        // Only teacherWithRoom still has room left (500,000 - 100,000 > 0) -- teacherFullyUsed has
        // used everything up and is not counted.
        assertThat(estimate.teachersWithUnusedPersonalAllocationCount()).isEqualTo(1);
    }

    /**
     * When the plan wallet is already exhausted/negative the percentage is meaningless --
     * wouldExceedAssessment/schoolLocked already say it, and adding a percentage only causes confusion.
     */
    @Test
    void should_return_null_shared_pool_insight_when_quota_pool_already_exhausted() {
        givenSchoolQuota(1_000_000, 1_000_000);

        AssessmentTokenEstimateResponse estimate = guard.estimateTokenQuota(centralizedAssessment(ASSESSMENT_SECONDS));

        assertThat(estimate.sharedPoolUsageRatio()).isNull();
        assertThat(estimate.teachersWithUnusedPersonalAllocationCount()).isNull();
    }

    /**
     * CLASS_TEST already sees its own personal ceiling -- there is no need to look at the domino
     * effect.
     */
    @Test
    void should_not_compute_shared_pool_insight_for_class_test() {
        givenSchoolQuota(10_000_000, 1_000_000);

        AssessmentTokenEstimateResponse estimate = guard.estimateTokenQuota(classTest(ASSESSMENT_SECONDS));

        assertThat(estimate.sharedPoolUsageRatio()).isNull();
        assertThat(estimate.teachersWithUnusedPersonalAllocationCount()).isNull();
    }

    private void givenSchoolQuota(long totalAllocatedVnd, long usedVnd) {
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT))
            .thenReturn(Optional.of(SchoolSubscriptionQuotaRecord.builder()
                .schoolSubscriptionId(subscriptionId)
                .quotaType(QuotaType.ASSESSMENT)
                .totalAllocatedAmountVnd(BigDecimal.valueOf(totalAllocatedVnd))
                .usedAmountVnd(BigDecimal.valueOf(usedVnd))
                .build()));
    }

    private void givenUserAllocation(long allocatedVnd, long usedVnd) {
        when(subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.ASSESSMENT, teacherId))
            .thenReturn(Optional.of(SchoolSubscriptionQuotaUserAllocation.builder()
                .schoolSubscriptionId(subscriptionId)
                .quotaType(QuotaType.ASSESSMENT)
                .userId(teacherId)
                .allocatedAmountVnd(BigDecimal.valueOf(allocatedVnd))
                .usedAmountVnd(BigDecimal.valueOf(usedVnd))
                .build()));
    }

    private void givenNoUserAllocation() {
        when(subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.ASSESSMENT, teacherId))
            .thenReturn(Optional.empty());
    }

    private void givenBalance(long balanceVnd) {
        when(schoolBalanceRepository.findBySchoolId(schoolId)).thenReturn(Optional.of(
            SchoolBalance.builder()
                .schoolId(schoolId)
                .balanceVnd(BigDecimal.valueOf(balanceVnd))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()));
    }

    /** Never topped up = no balance row, the same meaning as a balance of 0 -- see SchoolBalance.emptyFor. */
    private void givenNoBalanceRow() {
        when(schoolBalanceRepository.findBySchoolId(schoolId)).thenReturn(Optional.empty());
    }

    private Assessment classTest(Integer assessmentTimeDurationSecond) {
        Assessment assessment = baseAssessment(assessmentTimeDurationSecond);
        assessment.setKind(AssessmentKind.CLASS_TEST);
        return assessment;
    }

    private Assessment centralizedAssessment(Integer assessmentTimeDurationSecond) {
        Assessment assessment = baseAssessment(assessmentTimeDurationSecond);
        assessment.setKind(AssessmentKind.CENTRALIZED);
        return assessment;
    }

    private Assessment baseAssessment(Integer assessmentTimeDurationSecond) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setCreatedBy(teacherId.toString());
        assessment.setMaxAttempt(1);
        assessment.setAssessmentTimeDurationSecond(assessmentTimeDurationSecond);
        return assessment;
    }
}
