package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.domain.dto.ExamTokenEstimateDto;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;

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
    private final QuotaPricingPort quotaPricingPort;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;

    public ClassTestTokenQuotaGuardService(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            ExamCandidateRepository examCandidateRepository,
            QuotaPricingPort quotaPricingPort,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.examCandidateRepository = examCandidateRepository;
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
    public BigDecimal computeEstimatedCostUsd(Exam exam) {
        if (exam.getExamTimeDurationSecond() == null || exam.getExamTimeDurationSecond() <= 0) {
            return BigDecimal.ZERO;
        }
        var candidateCount = examCandidateRepository.countByExamId(exam.getId());
        var estimatedSeconds = BigDecimal.valueOf((long) exam.getExamTimeDurationSecond() * candidateCount * exam.getMaxAttempt());
        return estimatedSeconds.multiply(quotaPricingPort.currentEstimatedCostPerExamSecondUsd());
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