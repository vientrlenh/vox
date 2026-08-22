package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.domain.dto.ExamTokenEstimateDto;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.service.exam.PaperTimeCalculator;

/**
 * Ước lượng worst-case chi phí AI theo USD (duration × số thí sinh × maxAttempt ×
 * estimatedCostPerExamSecondUsd) và chặn khi vượt hạn mức GRADING của trường, và
 * với bài trên lớp thì thêm hạn mức CLASS_TEST của trường và (nếu có) hạn mức cá
 * nhân của giáo viên chủ bài -- vì CompleteExamSessionGradingUseCase trừ thật vào
 * cả 3 chỗ này khi chấm xong, nên publish/sửa bài phải soi trước cả 3 chứ không chỉ
 * GRADING.
 *
 * <p>estimatedCostPerExamSecondUsd lấy qua QuotaPricingPort -- ưu tiên giá đã tự calibrate từ
 * dữ liệu thật (QuotaPricingCalibrationJob), fallback về hằng số tĩnh .env
 * (QuotaPricingProperties) khi chưa đủ dữ liệu. Đây vẫn chỉ là số ƯỚC TÍNH, KHÔNG phải chi phí
 * thật -- chi phí thật trừ vào quota lấy từ tổng cost_usd trong ai_usage_record của session,
 * không nhân theo công thức này.
 *
 * <p>Dùng chung cho lúc publish (UpdateExamStatusUseCase), sửa bài đã publish (UpdateExamUseCase),
 * và thêm thí sinh (AddExamCandidateUseCase/ImportExamCandidatesFromClassUseCase) để không lệch
 * logic giữa các nơi.
 */
@Service
public class ClassTestTokenQuotaGuardService {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuotaPricingPort quotaPricingPort;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;

    public ClassTestTokenQuotaGuardService(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            QuotaPricingPort quotaPricingPort,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.quotaPricingPort = quotaPricingPort;
        this.schoolSubscriptionDebtGuardService = schoolSubscriptionDebtGuardService;
    }

    public void requireWithinTokenQuota(Exam exam) {
        // Chưa tính được thời gian làm bài (chưa có mã đề/câu hỏi) thì ước tính ra 0 token, không có
        // gì để soi. Chấp cả null lẫn 0 vì hai giá trị này cùng một nghĩa -- cùng idiom với
        // Exam.isScheduleWindowShorterThanExamTime và ExamTimeQuotaGuardService. Bỏ nhánh 0 thì kỳ
        // thi chưa có mã đề bị chặn lên lịch chỉ vì trường chưa cấu hình hạn mức, cho một con số 0.
        if (exam.getExamTimeDurationSecond() == null || exam.getExamTimeDurationSecond() <= 0) {
            return;
        }
        var estimatedCostUsd = computeEstimatedCostUsd(exam);

        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(exam.getSchoolId())
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể lên lịch kỳ thi"));

        // Trường đang nợ (chi phí AI thật đã vượt hạn mức cấp trường) thì chặn luôn ở đây, trước cả
        // khi soi ước lượng worst-case -- xem SchoolSubscriptionDebtGuardService.
        schoolSubscriptionDebtGuardService.requireSchoolNotLocked(subscription.getId());

        requireSchoolQuota(subscription.getId(), QuotaType.GRADING, estimatedCostUsd);

        if (exam.getKind() == ExamKind.CLASS_TEST) {
            requireSchoolQuota(subscription.getId(), QuotaType.CLASS_TEST, estimatedCostUsd);
            requireWithinUserAllocation(subscription.getId(), exam.getCreatedBy(), estimatedCostUsd);
        }
    }

    /** 0 nếu chưa có mã đề (duration null/0) -- không throw, dùng lại được cho cả requireWithinTokenQuota
     *  (chặn) và estimateTokenQuota (chỉ hiển thị cảnh báo, không chặn). */
    /**
     * Số giây dùng ở đây là {@code billableSeconds} -- KHÔNG gồm thời lượng phát AUDIO/VIDEO, nên cố
     * ý không đọc {@code exam.examTimeDurationSecond} (cột đó đã gồm media, xem
     * {@link PaperTimeCalculator}). Nhân giây phát media với một cái rate mang nghĩa "USD trên mỗi
     * giây thí sinh THẬT SỰ nói" là bịa ra chi phí không thể tồn tại.
     *
     * <p>Lấy MAX theo TỪNG mã đề chứ không lấy MAX rồi trừ: hai đại lượng có hai mã đề thắng khác
     * nhau. Mã đề A 1200s không media và mã đề B 1000s + 300s media thì ca thi do B quyết (1300s)
     * nhưng chi phí do A quyết (1200s) -- trừ media của mã đề thắng sẽ ra 1000s, hụt 200s.
     *
     * <p>Kết quả TUYẾN TÍNH theo maxAttempt (phép nhân thuần), nên frontend muốn biết "nếu lưu với
     * số lượt đang gõ thì tốn bao nhiêu" chỉ cần nhân tỉ lệ con số này -- không cần thêm tham số
     * override, cũng không cần gọi lại server mỗi lần gõ phím.
     */
    public BigDecimal computeEstimatedCostUsd(Exam exam) {
        if (exam.getExamTimeDurationSecond() == null || exam.getExamTimeDurationSecond() <= 0) {
            return BigDecimal.ZERO;
        }
        var billableSeconds = worstCaseBillableSecondsPerAttempt(exam.getId());
        if (billableSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        var candidateCount = examCandidateRepository.countByExamId(exam.getId());
        var estimatedSeconds = BigDecimal.valueOf((long) billableSeconds * candidateCount * exam.getMaxAttempt());
        return estimatedSeconds.multiply(quotaPricingPort.currentEstimatedCostPerExamSecondUsd());
    }

    /** Mã đề "đắt" nhất của kỳ thi tính theo giây sinh chi phí AI. Mỗi thí sinh chỉ làm MỘT mã đề. */
    private int worstCaseBillableSecondsPerAttempt(UUID examId) {
        var papers = examPaperRepository.findByExamId(examId);
        if (papers.isEmpty()) {
            return 0;
        }
        var paperIds = papers.stream().map(ExamPaper::getId).toList();
        var itemsByPaperId = examPaperItemRepository.findByPaperIdIn(paperIds).stream()
            .collect(Collectors.groupingBy(ExamPaperItem::getPaperId));
        var questionIds = itemsByPaperId.values().stream()
            .flatMap(List::stream)
            .map(ExamPaperItem::getQuestionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        var questionById = questionRepository.findByIdIn(questionIds).stream()
            .collect(Collectors.toMap(Question::getId, question -> question));

        var worst = 0;
        for (var paper : papers) {
            // List chứ không phải Set: một câu lặp lại trong cùng mã đề thì phải tính đủ số lần.
            var paperQuestions = new ArrayList<Question>();
            for (var item : itemsByPaperId.getOrDefault(paper.getId(), List.of())) {
                if (item.getQuestionId() == null) {
                    continue;
                }
                var question = questionById.get(item.getQuestionId());
                if (question != null) {
                    paperQuestions.add(question);
                }
            }
            worst = Math.max(worst, PaperTimeCalculator.billableSecondsOf(paperQuestions));
        }
        return worst;
    }

    /**
     * Ước lượng chi phí + hạn mức còn lại để hiển thị CẢNH BÁO ngay lúc tạo/sửa bài (trước khi
     * publish) -- KHÔNG throw, kể cả khi trường chưa có subscription active hoặc chưa cấu hình
     * hạn mức (trả về remaining = null cho trường hợp đó thay vì lỗi).
     */
    public ExamTokenEstimateDto estimateTokenQuota(Exam exam) {
        var estimatedCostUsd = computeEstimatedCostUsd(exam);
        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(exam.getSchoolId());
        if (subscription.isEmpty()) {
            return new ExamTokenEstimateDto(estimatedCostUsd, null, null, false, false);
        }

        var subscriptionId = subscription.get().getId();
        var remainingGrading = remainingSchoolQuota(subscriptionId, QuotaType.GRADING);
        var wouldExceedGrading = remainingGrading != null && estimatedCostUsd.compareTo(remainingGrading) > 0;

        BigDecimal remainingClassTest = null;
        var wouldExceedClassTest = false;
        if (exam.getKind() == ExamKind.CLASS_TEST) {
            remainingClassTest = remainingSchoolQuota(subscriptionId, QuotaType.CLASS_TEST);
            wouldExceedClassTest = remainingClassTest != null && estimatedCostUsd.compareTo(remainingClassTest) > 0;
        }

        return new ExamTokenEstimateDto(estimatedCostUsd, remainingGrading, remainingClassTest, wouldExceedGrading, wouldExceedClassTest);
    }

    private BigDecimal remainingSchoolQuota(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .map(quota -> quota.getTotalAllocated().subtract(quota.getUsedQuantity()))
            .orElse(null);
    }

    private void requireSchoolQuota(UUID subscriptionId, QuotaType quotaType, BigDecimal estimatedCostUsd) {
        var quota = subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new PlanLimitExceededException("Không tìm thấy hạn mức " + quotaType + " của gói đăng ký"));
        var remaining = quota.getTotalAllocated().subtract(quota.getUsedQuantity());
        if (estimatedCostUsd.compareTo(remaining) > 0) {
            throw new PlanLimitExceededException(
                "Chi phí ước tính cần dùng (" + estimatedCostUsd + " USD) vượt quá hạn mức " + quotaType
                    + " còn lại của trường (" + remaining + " USD), vui lòng mua thêm token hoặc nâng cấp gói"
            );
        }
    }

    private void requireWithinUserAllocation(UUID subscriptionId, UUID teacherId, BigDecimal estimatedCostUsd) {
        subscriptionQuotaUserAllocationRepository
            .findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.CLASS_TEST, teacherId)
            .ifPresent(allocation -> {
                var remaining = allocation.getAllocatedQuantity().subtract(allocation.getUsedQuantity());
                if (estimatedCostUsd.compareTo(remaining) > 0) {
                    throw new PlanLimitExceededException(
                        "Chi phí ước tính cần dùng (" + estimatedCostUsd
                            + " USD) vượt quá hạn mức bài kiểm tra trên lớp cá nhân còn lại (" + remaining
                            + " USD), vui lòng liên hệ quản trị trường để cấp thêm hạn mức"
                    );
                }
            });
    }
}