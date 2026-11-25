package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.CompleteAssessmentSessionGradingCommand;
import com.sep.vox.application.port.input.service.ConsumeQuotaService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.AiUsageRecordRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

// Internal service-to-service use case (called sau khi pipeline chấm bài AI hoàn tất), không end-user-facing
@Service
@RequiredArgsConstructor
public class CompleteAssessmentSessionGradingUseCase implements IUseCase<CompleteAssessmentSessionGradingCommand, Void> {

    // Matches the scale of school_balance_entries.fx_rate_used numeric(12,4).
    private static final int FX_RATE_SCALE = 4;

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final ConsumeQuotaService consumeQuotaService;
    private final QuotaPricingPort quotaPricingPort;

    @Override
    @Transactional
    public Void execute(CompleteAssessmentSessionGradingCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.assessmentSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));

        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        SchoolSubscription subscription = schoolSubscriptionRepository.findActiveBySchoolId(assessment.getSchoolId())
            .orElseThrow(() -> new ResourceNotFoundException("Trường chưa có gói subscription đang hoạt động"));

        if (!claimGradedTransition(session)) {
            return null;
        }

        chargeAiCost(session, assessment, subscription);
        return null;
    }

    /**
     * Conditional status transition (atomic UPDATE ... WHERE status = GRADING) to hold the session row lock until
     * this transaction commits or rolls back -- this is the "gate" against deducting the quota twice when the last
     * items of the same session are processed concurrently (Kafka partitions by answerId, not by sessionId) or when
     * Kafka redelivers a message. If the claim is not won (another flow finished first, or the call targets a
     * session that is no longer in GRADING), it is a successful no-op -- no throw, so the /complete-grading
     * endpoint can safely be called several times.
     */
    private boolean claimGradedTransition(AssessmentSession session) {
        return assessmentSessionRepository.tryTransitionStatus(
            session.getId(), AssessmentSessionStatus.GRADING, AssessmentSessionStatus.GRADED
        );
    }

    /**
     * The source of the quota deduction is the real sum of cost_vnd from ai_usage_records (LLM tokens + STT/TTS/avatar
     * duration), NOT the number of answer seconds. It sums the VND column, whose exchange rate was fixed per row, rather than
     * converting the USD total at today's rate -- see AiUsageRecordRepository.
     *
     * <p>CLAIM the not-yet-charged rows first, then sum exactly the rows carrying the stamp just applied: a session
     * IS ALLOWED to be regraded (UpdateAssessmentSessionStatusUseCase permits GRADED -> GRADING), and the later grading
     * produces real new cost that must be charged. Summing the whole session as before would charge the first grading's share
     * a second time -- 100k + (100k + 40k) for a real cost of 140k.
     *
     * <p>The guard "a row is charged only once" lives in the charged_at column itself, NOT in a
     * unique constraint on school_balance_entries(assessment_session_id): such a constraint would wrongly block exactly the legitimate
     * deduction of a regrading.
     *
     * <p>allowDebt=true: the AI cost has really been incurred, so it must be recorded in full even above the limit --
     * see SchoolSubscriptionDebtGuardService for locking the school when it falls into debt.
     *
     * <p>EXACTLY ONE deduction per assessment session. This used to be called twice with the same totalCostUsd (once
     * GRADING, once CLASS_TEST) because CLASS_TEST was treated as a second wallet -- it is only a spending cap inside
     * the assessment wallet, so the second deduction was a double charge: it doubled the school's used_amount_vnd
     * against the real money for every class test. See QuotaType.
     */
    private void chargeAiCost(AssessmentSession session, Assessment assessment, SchoolSubscription subscription) {
        Instant now = Instant.now();
        int claimedRows = aiUsageRecordRepository.markChargedByAssessmentSessionId(session.getId(), now);
        // The USD total is still fetched because school_balance_entries.cost_usd keeps the original currency to
        // reconcile against the provider invoices (V2 section 7) -- same chargedAt stamp, so the two columns of one
        // ledger entry describe exactly the same set of rows.
        BigDecimal totalCostVnd = claimedRows == 0 ? BigDecimal.ZERO : aiUsageRecordRepository
            .sumCostVndByAssessmentSessionIdAndChargedAt(session.getId(), now);
        BigDecimal totalCostUsd = claimedRows == 0 ? BigDecimal.ZERO : aiUsageRecordRepository
            .sumCostUsdByAssessmentSessionIdAndChargedAt(session.getId(), now);
        if (totalCostVnd.compareTo(BigDecimal.ZERO) > 0) {
            checkAndReportLockTransition(subscription.getId(), assessment.getSchoolId(), QuotaType.ASSESSMENT,
                session.getId(), totalCostVnd, totalCostUsd, resolveChargedUserId(assessment), now);
        }
    }

    /**
     * userId is passed only for a class test: that is spending against the PERSONAL quota the school grants to the
     * teacher who set the test. A centralized assessment is organised by the school and belongs to nobody's own
     * pocket, so it stays null -- ConsumeQuotaService then skips the personal-quota deduction.
     */
    private UUID resolveChargedUserId(Assessment assessment) {
        return assessment.getKind() == AssessmentKind.CLASS_TEST ? assessment.createdByUserId().orElse(null) : null;
    }

    /**
     * Trừ hạn mức cho ca thi vừa chấm xong.
     *
     * <p>KHÔNG còn tự báo cáo chuyển trạng thái nợ: việc đó đã chuyển vào
     * {@code ConsumeQuotaService.chargeOverage}, nơi biết cả hai nguồn trừ tiền (ca thi và phiên
     * luyện nói) và nơi dòng sự kiện commit cùng transaction với bút toán đã gây ra nó. Giữ ở đây
     * nghĩa là đường luyện nói vĩnh viễn không có ai báo -- xem lịch sử của V4.
     */
    private void checkAndReportLockTransition(UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            UUID assessmentSessionId, BigDecimal totalCostVnd, BigDecimal totalCostUsd, UUID userId, Instant now) {

        consumeQuotaService.consumeAssessmentAllowingDebt(
            subscriptionId, assessmentSessionId, totalCostVnd,
            totalCostUsd, effectiveFxRate(totalCostVnd, totalCostUsd), userId
        );
    }

    /**
     * Tỷ giá THỰC TẾ đã áp cho cả phiên = tổng VND / tổng USD.
     *
     * <p>Một phiên thi gồm nhiều lượt gọi AI, mỗi lượt đã chốt fx_rate_used riêng lúc phát sinh, nên
     * phiên vắt qua ngày đổi tỷ giá sẽ có nhiều tỷ giá khác nhau. Bút toán trên sổ cái chỉ có MỘT cột
     * fx_rate_used, và tỷ giá bình quân gia quyền này là con số duy nhất khiến
     * {@code cost_usd * fx_rate_used} khớp lại đúng {@code amount_vnd} của chính bút toán đó -- lấy
     * bừa tỷ giá của một lượt, hay tỷ giá mới nhất, đều làm phép đối soát ngược không ra.
     */
    private BigDecimal effectiveFxRate(BigDecimal totalCostVnd, BigDecimal totalCostUsd) {
        if (totalCostUsd.compareTo(BigDecimal.ZERO) <= 0) {
            // Có cost_vnd mà không có cost_usd là dữ liệu hỏng, nhưng khoản chi vẫn phải được ghi
            // nhận -- lấy tỷ giá hiện hành để bút toán có số hợp lệ thay vì chia cho 0.
            return quotaPricingPort.usdToVndRate();
        }
        return totalCostVnd.divide(totalCostUsd, FX_RATE_SCALE, RoundingMode.HALF_UP);
    }
}