package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CompleteAssessmentSessionGradingCommand;
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
public class CompleteAssessmentSessionGradingUseCase implements IUseCase<CompleteAssessmentSessionGradingCommand, Void> {

    // Trùng scale của school_balance_entries.fx_rate_used numeric(12,4).
    private static final int FX_RATE_SCALE = 4;

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final ConsumeQuotaService consumeQuotaService;
    private final QuotaPricingPort quotaPricingPort;

    public CompleteAssessmentSessionGradingUseCase(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentRepository assessmentRepository,
            AiUsageRecordRepository aiUsageRecordRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            ConsumeQuotaService consumeQuotaService,
            QuotaPricingPort quotaPricingPort) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentRepository = assessmentRepository;
        this.aiUsageRecordRepository = aiUsageRecordRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.consumeQuotaService = consumeQuotaService;
        this.quotaPricingPort = quotaPricingPort;
    }

    @Override
    @Transactional
    public Void execute(CompleteAssessmentSessionGradingCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.assessmentSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));

        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        SchoolSubscription subscription = schoolSubscriptionRepository.findActiveBySchoolId(assessment.getSchoolId())
            .orElseThrow(() -> new NotFoundException("Trường chưa có gói subscription đang hoạt động"));

        // Chuyển trạng thái có điều kiện (atomic UPDATE ... WHERE status = GRADING) để giữ row lock
        // của session tới khi transaction này commit/rollback -- đây là "chốt chặn" chống trừ quota
        // 2 lần khi các item cuối của cùng 1 session được xử lý đồng thời (Kafka partition theo answerId,
        // không theo sessionId) hoặc khi message bị Kafka redeliver. Nếu không giành được (đã bị luồng
        // khác hoàn tất trước, hoặc gọi vào session không còn ở GRADING), coi là no-op thành công --
        // không throw, để endpoint /complete-grading gọi lại được nhiều lần an toàn.
        boolean claimed = assessmentSessionRepository.tryTransitionStatus(
            session.getId(), AssessmentSessionStatus.GRADING, AssessmentSessionStatus.GRADED
        );
        if (!claimed) {
            return null;
        }

        // The source of the quota deduction is the real sum of cost_vnd from ai_usage_records (LLM tokens + STT/TTS/avatar
        // duration), NOT the number of answer seconds. It sums the VND column, whose exchange rate was fixed per row, rather than
        // converting the USD total at today's rate -- see AiUsageRecordRepository.
        //
        // CLAIM the not-yet-charged rows first, then sum exactly the rows carrying the stamp just applied: a session
        // IS ALLOWED to be regraded (UpdateAssessmentSessionStatusUseCase permits GRADED -> GRADING), and the later grading
        // produces real new cost that must be charged. Summing the whole session as before would charge the first grading's share
        // a second time -- 100k + (100k + 40k) for a real cost of 140k.
        //
        // The guard "a row is charged only once" lives in the charged_at column itself, NOT in a
        // unique constraint on school_balance_entries(assessment_session_id): such a constraint would wrongly block exactly the legitimate
        // deduction of a regrading.
        Instant now = Instant.now();
        int claimedRows = aiUsageRecordRepository.markChargedByAssessmentSessionId(session.getId(), now);
        // Vẫn lấy thêm tổng USD vì school_balance_entries.cost_usd giữ nguyên tệ gốc để đối soát ngược
        // với hóa đơn nhà cung cấp (V2 mục 7) -- cùng mốc chargedAt để hai cột của một bút toán mô tả
        // đúng cùng một tập dòng.
        BigDecimal totalCostVnd = claimedRows == 0 ? BigDecimal.ZERO : aiUsageRecordRepository
            .sumCostVndByAssessmentSessionIdAndChargedAt(session.getId(), now);
        BigDecimal totalCostUsd = claimedRows == 0 ? BigDecimal.ZERO : aiUsageRecordRepository
            .sumCostUsdByAssessmentSessionIdAndChargedAt(session.getId(), now);
        if (totalCostVnd.compareTo(BigDecimal.ZERO) > 0) {
            // allowDebt=true: chi phí AI thật đã phát sinh, phải ghi nhận đủ dù vượt hạn mức --
            // xem SchoolSubscriptionDebtGuardService cho phần khóa trường khi rơi vào nợ.
            //
            // ĐÚNG MỘT lần trừ cho một phiên thi. Trước đây chỗ này gọi hai lần với cùng
            // totalCostUsd (một lần GRADING, một lần CLASS_TEST) vì CLASS_TEST bị coi là ví thứ hai
            // -- nó vốn chỉ là trần chi nằm trong ví thi, nên lần trừ thứ hai là trừ trùng: đẩy
            // used_amount_vnd của trường lên gấp đôi tiền thật cho mọi bài kiểm tra trên lớp. Xem
            // QuotaType.
            //
            // userId chỉ truyền với bài kiểm tra trên lớp: đó là khoản chi tiêu vào hạn mức CÁ NHÂN
            // mà nhà trường cấp cho chính giáo viên ra đề. Kỳ thi tập trung do nhà trường tổ chức,
            // không thuộc túi riêng của ai nên để null -- ConsumeQuotaService sẽ bỏ qua bước trừ
            // hạn mức cá nhân.
            UUID chargedUserId = assessment.getKind() == AssessmentKind.CLASS_TEST ? assessment.getCreatedBy() : null;
            checkAndReportLockTransition(subscription.getId(), assessment.getSchoolId(), QuotaType.ASSESSMENT,
                session.getId(), totalCostVnd, totalCostUsd, chargedUserId, now);
        }
        return null;
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