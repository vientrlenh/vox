package com.sep.vox.application.port.input.usecase.exam;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CompleteExamSessionGradingCommand;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.service.SchoolDebtNotificationService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ConsumeQuotaUseCase;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.AiUsageRecordRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

// Internal service-to-service use case (called sau khi pipeline chấm bài AI hoàn tất), không end-user-facing
@Service
public class CompleteExamSessionGradingUseCase implements IUseCase<CompleteExamSessionGradingCommand, Void> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final ConsumeQuotaUseCase consumeQuotaUseCase;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;
    private final SchoolDebtNotificationService schoolDebtNotificationService;

    public CompleteExamSessionGradingUseCase(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            AiUsageRecordRepository aiUsageRecordRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            ConsumeQuotaUseCase consumeQuotaUseCase,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService,
            SchoolDebtNotificationService schoolDebtNotificationService) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.aiUsageRecordRepository = aiUsageRecordRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.consumeQuotaUseCase = consumeQuotaUseCase;
        this.schoolSubscriptionDebtGuardService = schoolSubscriptionDebtGuardService;
        this.schoolDebtNotificationService = schoolDebtNotificationService;
    }

    @Override
    @Transactional
    public Void execute(CompleteExamSessionGradingCommand input) {
        var session = examSessionRepository.findById(input.examSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));

        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(exam.getSchoolId())
            .orElseThrow(() -> new NotFoundException("Trường chưa có gói subscription đang hoạt động"));

        // Chuyển trạng thái có điều kiện (atomic UPDATE ... WHERE status = GRADING) để giữ row lock
        // của session tới khi transaction này commit/rollback -- đây là "chốt chặn" chống trừ quota
        // 2 lần khi các item cuối của cùng 1 session được xử lý đồng thời (Kafka partition theo answerId,
        // không theo sessionId) hoặc khi message bị Kafka redeliver. Nếu không giành được (đã bị luồng
        // khác hoàn tất trước, hoặc gọi vào session không còn ở GRADING), coi là no-op thành công --
        // không throw, để endpoint /complete-grading gọi lại được nhiều lần an toàn.
        boolean claimed = examSessionRepository.tryTransitionStatus(
            session.getId(), ExamSessionStatus.GRADING, ExamSessionStatus.GRADED
        );
        if (!claimed) {
            return null;
        }

        // Nguồn trừ quota là tổng cost_usd thật từ ai_usage_record (LLM token + STT/TTS/avatar
        // duration do Agentic AI báo cáo), KHÔNG phải số giây câu trả lời -- xem V22__quota_unit_to_usd.sql.
        var totalCostUsd = aiUsageRecordRepository.sumCostUsdByExamSessionId(session.getId());
        if (totalCostUsd.compareTo(BigDecimal.ZERO) > 0) {
            var now = Instant.now();

            // allowDebt=true: chi phí AI thật đã phát sinh, phải ghi nhận đủ dù vượt hạn mức --
            // xem SchoolSubscriptionDebtGuardService cho phần khóa trường khi rơi vào nợ.
            //
            // So sánh TỪNG bucket riêng (không gộp OR như trước) -- gộp sẽ bỏ sót trường hợp GRADING
            // đã nợ sẵn từ trước (wasLocked đã = true) nhưng CLASS_TEST mới thật sự transition ở lần
            // trừ này, khiến notification/audit log không được ghi dù CLASS_TEST vừa vượt hạn mức.
            checkAndReportLockTransition(subscription.getId(), exam.getSchoolId(), QuotaType.GRADING,
                session.getId(), totalCostUsd, null, now);

            if (exam.getKind() == ExamKind.CLASS_TEST) {
                checkAndReportLockTransition(subscription.getId(), exam.getSchoolId(), QuotaType.CLASS_TEST,
                    session.getId(), totalCostUsd, exam.getCreatedBy(), now);
            }
        }
        return null;
    }

    private void checkAndReportLockTransition(UUID subscriptionId, UUID schoolId,
            QuotaType quotaType, UUID examSessionId, BigDecimal totalCostUsd, UUID userId, Instant now) {
        var wasOver = schoolSubscriptionDebtGuardService.isQuotaOverLimit(subscriptionId, quotaType);

        var after = consumeQuotaUseCase.execute(new ConsumeQuotaCommand(
            subscriptionId, examSessionId, quotaType, totalCostUsd, userId, true
        ));

        if (!wasOver && after.isLocked()) {
            schoolDebtNotificationService.publishSchoolLockedDueToDebt(
                subscriptionId, schoolId, quotaType, examSessionId, totalCostUsd,
                after.totalAllocated(), after.usedQuantity(), now
            );
        }
    }
}