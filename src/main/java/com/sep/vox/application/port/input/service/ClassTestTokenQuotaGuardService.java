package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.response.input.assessment.AssessmentTokenEstimateResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

/**
 * Estimates the worst-case AI cost (duration x number of candidates x maxAttempt x estimatedCostPerAssessmentSecondUsd)
 * and blocks when it exceeds what the school can afford, and for a class test also (if present) the personal quota
 * of the teacher who owns the test -- because CompleteAssessmentSessionGradingUseCase really deducts from exactly these 2 places when grading
 * finishes, so publishing/editing an assessment must check both beforehand.
 *
 * <p>Every comparison here is in VND. The estimate is produced in USD (the provider's list cost price is in
 * USD), so it is converted RIGHT in {@link #computeEstimatedCostVnd} and not carried further down: quota and
 * balance are both VND columns, and comparing a USD number directly with a VND number makes the left side smaller than the right side
 * by about 26,000 times -- the gate is still standing there but never closes.
 *
 * <p>"How much the school can still spend" = remaining plan quota + the balance of the self-topped-up wallet, NOT only the quota.
 * Since ConsumeQuotaService charges the part above the quota to the self-topped-up wallet (OVERAGE_CHARGE entry), a
 * school that has run out of quota but has topped up can still pay -- checking only the quota here would refuse exactly the
 * amount that we will gladly charge when grading finishes.
 *
 * <p>Previously 3 places were checked, including the school-level CLASS_TEST wallet. That wallet was removed: it is a spending ceiling that sits
 * inside the assessment wallet, not a third pocket of money, and checking it here would make the same expense
 * pass two checks against the same balance. The ceiling per TEACHER still exists -- see
 * requireWithinUserAllocation and QuotaType.
 *
 * <p>estimatedCostPerAssessmentSecondUsd is obtained through QuotaPricingPort -- preferring the price calibrated automatically from
 * real data (QuotaPricingCalibrationJob), falling back to the static .env constant
 * (QuotaPricingProperties) when there is not enough data. It is still only an ESTIMATE, NOT the real
 * cost -- the real cost deducted from the quota is the sum of cost_vnd in the ai_usage_records of the session,
 * not multiplied by this formula.
 *
 * <p>Shared by publishing (UpdateAssessmentStatusUseCase), editing a published assessment (UpdateAssessmentUseCase),
 * and adding candidates (AddAssessmentCandidateUseCase/ImportAssessmentCandidatesFromClassUseCase) so that the
 * logic does not diverge between places.
 */
@Service
@RequiredArgsConstructor
public class ClassTestTokenQuotaGuardService {

    // Matches the scale of school_subscription_quota_records.used_amount_vnd numeric(18,6) -- same reason
    // as RecordAiUsageUseCase: if wider, Postgres truncates further; if narrower, the number compared here has
    // already drifted from the number that will actually be deducted.
    private static final int COST_VND_SCALE = 6;

    // % so it is smooth enough to show "37.5%" instead of rounding to a whole number -- it does not need to match the money scale
    // because this is a displayed ratio, not a number deducted or compared directly with ai_usage_records.
    private static final int RATIO_SCALE = 4;

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;
    private final QuotaPricingPort quotaPricingPort;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;

    public void requireWithinTokenQuota(Assessment assessment) {
        if (hasNoAttemptDuration(assessment)) {
            return;
        }
        BigDecimal estimatedCostVnd = computeEstimatedCostVnd(assessment);

        SchoolSubscription subscription = schoolSubscriptionRepository.findActiveBySchoolId(assessment.getSchoolId())
            .orElseThrow(() -> new BusinessRuleViolationException(
                "Trường chưa có gói subscription đang hoạt động, không thể lên lịch kỳ thi"));

        // A school in debt (negative self-top-up wallet balance) is blocked right here, even before the
        // worst-case estimate is looked at -- see SchoolSubscriptionDebtGuardService.
        schoolSubscriptionDebtGuardService.requireSchoolNotLocked(subscription.getSchoolId());

        requireSchoolFunds(subscription.getId(), assessment.getSchoolId(), QuotaType.ASSESSMENT, estimatedCostVnd);

        // A centralized assessment is organised by the school, so it counts against nobody's personal pocket; only a
        // class test touches the personal quota the school grants the teacher who authored it.
        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            requireWithinUserAllocation(subscription.getId(), assessment.createdByUserId().orElse(null), estimatedCostVnd);
        }
    }

    // If the attempt duration cannot be computed yet (no paper variant/questions yet), the estimate is 0 tokens and there is nothing
    // to check. Both null and 0 are accepted because they mean the same thing -- same idiom as
    // Assessment.isScheduleWindowShorterThanAssessmentTime and AssessmentTimeQuotaGuardService. Dropping the 0 branch would make an
    // assessment without a paper variant blocked from scheduling just because the school has not configured a quota, over a number that is 0.
    private boolean hasNoAttemptDuration(Assessment assessment) {
        return assessment.getAssessmentTimeDurationSecond() == null || assessment.getAssessmentTimeDurationSecond() <= 0;
    }

    /**
     * 0 when there is no paper variant yet (duration null/0) -- does not throw, so it is reusable by both
     * requireWithinTokenQuota (blocks) and estimateTokenQuota (only shows a warning, does not block).
     *
     * <p>Converted to VND right here, once: the per-second price listed by QuotaPricingPort is in USD, while
     * everything this number will be compared with (quota, balance, and the amount really deducted when grading
     * finishes) is VND. Leaving the exchange-rate multiplication to every caller leaves room to forget it in exactly one place.
     */
    public BigDecimal computeEstimatedCostVnd(Assessment assessment) {
        if (hasNoAttemptDuration(assessment)) {
            return BigDecimal.ZERO;
        }
        long candidateCount = assessmentCandidateRepository.countByAssessmentId(assessment.getId());
        BigDecimal estimatedSeconds = BigDecimal.valueOf((long) assessment.getAssessmentTimeDurationSecond() * candidateCount * assessment.getMaxAttempt());
        return estimatedSeconds
            .multiply(quotaPricingPort.currentEstimatedCostPerAssessmentSecondUsd())
            .multiply(quotaPricingPort.usdToVndRate())
            .setScale(COST_VND_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Estimated cost + remaining spending ability, shown as a WARNING right when the assessment is created/edited (before
     * publishing) -- does NOT throw, even when the school has no active subscription or no configured
     * quota (returns remaining = null for that case instead of an error).
     *
     * <p>Shares the two calculations of the blocking path ({@link #computeEstimatedCostVnd},
     * {@link #spendableSchoolFundsVnd}) instead of recomputing: a warning that drifts from the gate either
     * scares the user about an amount that can still be published, or lets them press publish and only then hit the error.
     */
    public AssessmentTokenEstimateResponse estimateTokenQuota(Assessment assessment) {
        BigDecimal estimatedCostVnd = computeEstimatedCostVnd(assessment);
        boolean schoolLocked = schoolSubscriptionDebtGuardService.isSchoolLocked(assessment.getSchoolId());
        Optional<SchoolSubscription> subscription = schoolSubscriptionRepository.findActiveBySchoolId(assessment.getSchoolId());
        if (subscription.isEmpty()) {
            return new AssessmentTokenEstimateResponse(estimatedCostVnd, null, null, false, false, schoolLocked, null, null);
        }

        UUID subscriptionId = subscription.get().getId();
        BigDecimal remainingAssessment = spendableSchoolFundsVnd(subscriptionId, assessment.getSchoolId(), QuotaType.ASSESSMENT);
        boolean wouldExceedAssessment = remainingAssessment != null && estimatedCostVnd.compareTo(remainingAssessment) > 0;
        PersonalAllocationEstimate personalAllocation = personalAllocationEstimate(assessment, subscriptionId, estimatedCostVnd);
        SharedPoolInsight sharedPoolInsight = sharedPoolInsightFor(assessment, subscriptionId, estimatedCostVnd);

        return new AssessmentTokenEstimateResponse(
            estimatedCostVnd, remainingAssessment, personalAllocation.remaining(), wouldExceedAssessment,
            personalAllocation.wouldExceed(), schoolLocked, sharedPoolInsight.usageRatio(),
            sharedPoolInsight.teachersWithUnusedAllocation());
    }

    private record PersonalAllocationEstimate(@Nullable BigDecimal remaining, boolean wouldExceed) {
    }

    // The second pair is now the PERSONAL quota of the teacher who owns the assessment, no longer a school-level CLASS_TEST
    // wallet. The two warnings stay separate because the two sides block for different reasons and are handled
    // differently: when the school's money runs out it must top up/upgrade the plan, while when the personal quota runs out
    // it only needs to ask the school admin for more.
    private PersonalAllocationEstimate personalAllocationEstimate(
            Assessment assessment, UUID subscriptionId, BigDecimal estimatedCostVnd) {
        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
            return new PersonalAllocationEstimate(null, false);
        }
        BigDecimal remaining = remainingUserAllocation(subscriptionId, assessment.createdByUserId().orElse(null));
        return new PersonalAllocationEstimate(remaining, remaining != null && estimatedCostVnd.compareTo(remaining) > 0);
    }

    // A centralized assessment has no personal quota (branch above), so whatever it spends comes straight out of the
    // shared wallet -- unlike CLASS_TEST, the person who presses schedule (SCHOOL_ADMIN/CHAIR) does not see by themselves
    // the effect on the other teachers who have their own personal quota. Computed only for CENTRALIZED, exactly the
    // scope of the problem -- see computeSharedPoolInsight.
    private SharedPoolInsight sharedPoolInsightFor(Assessment assessment, UUID subscriptionId, BigDecimal estimatedCostVnd) {
        return assessment.getKind() == AssessmentKind.CENTRALIZED
            ? computeSharedPoolInsight(subscriptionId, estimatedCostVnd)
            : SharedPoolInsight.NOT_APPLICABLE;
    }

    private record SharedPoolInsight(BigDecimal usageRatio, Integer teachersWithUnusedAllocation) {
        static final SharedPoolInsight NOT_APPLICABLE = new SharedPoolInsight(null, null);
    }

    /**
     * Only meaningful for CENTRALIZED -- see the call in estimateTokenQuota. The % is computed on EXACTLY the remaining PLAN
     * quota (without adding the self-topped-up wallet balance): the question here is "what % of the allotted budget does this assessment eat", which differs from
     * the question "how much can the school still spend in total" that remainingAssessmentVnd/
     * wouldExceedAssessment already answer.
     */
    private SharedPoolInsight computeSharedPoolInsight(UUID subscriptionId, BigDecimal estimatedCostVnd) {
        Optional<SchoolSubscriptionQuotaRecord> pool = subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT);
        if (pool.isEmpty()) {
            return SharedPoolInsight.NOT_APPLICABLE;
        }
        BigDecimal quotaOnlyRemaining = pool.get().getTotalAllocatedAmountVnd().subtract(pool.get().getUsedAmountVnd());
        // If the plan wallet is already exhausted/negative the ratio is meaningless (denominator <= 0) -- wouldExceedAssessment/schoolLocked in
        // the other banner already say it, and adding a % here only causes confusion.
        if (quotaOnlyRemaining.signum() <= 0) {
            return SharedPoolInsight.NOT_APPLICABLE;
        }
        BigDecimal ratio = estimatedCostVnd.divide(quotaOnlyRemaining, RATIO_SCALE, RoundingMode.HALF_UP);
        int teacherCount = (int) subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT).stream()
            .filter(allocation -> allocation.getAllocatedAmountVnd().compareTo(allocation.getUsedAmountVnd()) > 0)
            .count();
        return new SharedPoolInsight(ratio, teacherCount);
    }

    /**
     * Tổng tiền trường còn chi được cho loại hạn mức này = hạn mức kèm gói còn lại + số dư ví tự nạp.
     * null = trường chưa cấu hình ví này (khác hẳn với "đã cấu hình nhưng còn 0đ").
     *
     * <p>Số dư âm được kẹp về 0 chứ không cộng thẳng: số âm là NỢ, mà nợ đã do
     * {@link SchoolSubscriptionDebtGuardService} chặn ở một cửa riêng với thông báo riêng. Cộng nó
     * vào đây sẽ biến một trường đang bị khóa thành một trường "hết hạn mức", tức báo sai lý do và
     * chỉ sai cách khắc phục.
     */
    private BigDecimal spendableSchoolFundsVnd(UUID subscriptionId, UUID schoolId, QuotaType quotaType) {
        return subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .map(quota -> quota.getTotalAllocatedAmountVnd().subtract(quota.getUsedAmountVnd())
                .add(spendableBalanceVnd(schoolId)))
            .orElse(null);
    }

    /** Chỉ ĐỌC số dư (không phải đường ghi) nên dùng findBySchoolId -- xem javadoc SchoolBalanceRepository. */
    private BigDecimal spendableBalanceVnd(UUID schoolId) {
        return schoolBalanceRepository.findBySchoolId(schoolId)
            .map(balance -> balance.getBalanceVnd())
            .map(balanceVnd -> balanceVnd.max(BigDecimal.ZERO))
            .orElse(BigDecimal.ZERO);
    }

    /** null = giáo viên không có hạn mức cá nhân riêng, tức chỉ tiền của trường áp dụng. */
    private BigDecimal remainingUserAllocation(UUID subscriptionId, UUID teacherId) {
        return subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.ASSESSMENT, teacherId)
            .map(allocation -> allocation.getAllocatedAmountVnd().subtract(allocation.getUsedAmountVnd()))
            .orElse(null);
    }

    private void requireSchoolFunds(UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            BigDecimal estimatedCostVnd) {
        BigDecimal remaining = spendableSchoolFundsVnd(subscriptionId, schoolId, quotaType);
        if (remaining == null) {
            throw new BusinessRuleViolationException("Không tìm thấy hạn mức " + quotaType + " của gói đăng ký");
        }
        if (estimatedCostVnd.compareTo(remaining) > 0) {
            throw new BusinessRuleViolationException(
                "Chi phí ước tính cần dùng (" + estimatedCostVnd + "đ) vượt quá số tiền trường còn chi được cho "
                    + quotaType + " (" + remaining + "đ, gồm hạn mức kèm gói và số dư ví), vui lòng nạp thêm tiền"
                    + " hoặc nâng cấp gói"
            );
        }
    }

    /**
     * Trần chi CÁ NHÂN mà trường tự chia nội bộ. KHÔNG cộng số dư ví trường vào đây: đây là một giới
     * hạn nội bộ chứ không phải một túi tiền, nên cộng tiền của trường vào sẽ xóa sạch chính cái trần
     * mà quản trị trường vừa đặt ra -- xem ConsumeQuotaService.consumeUserAllocation và QuotaType.
     */
    private void requireWithinUserAllocation(UUID subscriptionId, UUID teacherId, BigDecimal estimatedCostVnd) {
        BigDecimal remaining = remainingUserAllocation(subscriptionId, teacherId);
        if (remaining != null && estimatedCostVnd.compareTo(remaining) > 0) {
            throw new BusinessRuleViolationException(
                "Chi phí ước tính cần dùng (" + estimatedCostVnd
                    + "đ) vượt quá hạn mức bài kiểm tra trên lớp cá nhân còn lại (" + remaining
                    + "đ), vui lòng liên hệ quản trị trường để cấp thêm hạn mức"
            );
        }
    }
}
