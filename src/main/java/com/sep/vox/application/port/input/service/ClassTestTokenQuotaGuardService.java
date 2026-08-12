package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;

/**
 * Ước lượng worst-case token (duration × số thí sinh × maxAttempt) và chặn khi vượt hạn mức
 * GRADING của trường, và với bài trên lớp thì thêm hạn mức CLASS_TEST của trường và (nếu có)
 * hạn mức cá nhân của giáo viên chủ bài -- vì CompleteExamSessionGradingUseCase trừ thật vào
 * cả 3 chỗ này khi chấm xong, nên publish/sửa bài phải soi trước cả 3 chứ không chỉ GRADING.
 *
 * <p>Dùng chung cho lúc publish (UpdateExamStatusUseCase) và lúc sửa bài đã publish
 * (UpdateExamUseCase) để không lệch logic giữa 2 nơi.
 */
@Service
public class ClassTestTokenQuotaGuardService {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final ExamCandidateRepository examCandidateRepository;

    public ClassTestTokenQuotaGuardService(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            ExamCandidateRepository examCandidateRepository) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.examCandidateRepository = examCandidateRepository;
    }

    public void requireWithinTokenQuota(Exam exam) {
        // Chưa tính được thời gian làm bài (chưa có mã đề/câu hỏi) thì ước tính ra 0 token, không có
        // gì để soi. Chấp cả null lẫn 0 vì hai giá trị này cùng một nghĩa -- cùng idiom với
        // Exam.isScheduleWindowShorterThanExamTime và ExamTimeQuotaGuardService. Bỏ nhánh 0 thì kỳ
        // thi chưa có mã đề bị chặn lên lịch chỉ vì trường chưa cấu hình hạn mức, cho một con số 0.
        if (exam.getExamTimeDurationSecond() == null || exam.getExamTimeDurationSecond() <= 0) {
            return;
        }
        var candidateCount = examCandidateRepository.countByExamId(exam.getId());
        var estimatedTokens = (long) exam.getExamTimeDurationSecond() * candidateCount * exam.getMaxAttempt();

        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(exam.getSchoolId())
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể lên lịch kỳ thi"));

        requireSchoolQuota(subscription.getId(), QuotaType.GRADING, estimatedTokens);

        if (exam.getKind() == ExamKind.CLASS_TEST) {
            requireSchoolQuota(subscription.getId(), QuotaType.CLASS_TEST, estimatedTokens);
            requireWithinUserAllocation(subscription.getId(), exam.getCreatedBy(), estimatedTokens);
        }
    }

    private void requireSchoolQuota(UUID subscriptionId, QuotaType quotaType, long estimatedTokens) {
        var quota = subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new PlanLimitExceededException("Không tìm thấy hạn mức " + quotaType + " của gói đăng ký"));
        var remaining = quota.getTotalAllocated() - quota.getUsedQuantity();
        if (estimatedTokens > remaining) {
            throw new PlanLimitExceededException(
                "Số token ước tính cần dùng (" + estimatedTokens + ") vượt quá hạn mức " + quotaType
                    + " còn lại của trường (" + remaining + "), vui lòng mua thêm token hoặc nâng cấp gói"
            );
        }
    }

    private void requireWithinUserAllocation(UUID subscriptionId, UUID teacherId, long estimatedTokens) {
        subscriptionQuotaUserAllocationRepository
            .findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.CLASS_TEST, teacherId)
            .ifPresent(allocation -> {
                var remaining = allocation.getAllocatedQuantity() - allocation.getUsedQuantity();
                if (estimatedTokens > remaining) {
                    throw new PlanLimitExceededException(
                        "Số token ước tính cần dùng (" + estimatedTokens
                            + ") vượt quá hạn mức bài kiểm tra trên lớp cá nhân còn lại (" + remaining
                            + "), vui lòng liên hệ quản trị trường để cấp thêm hạn mức"
                    );
                }
            });
    }
}