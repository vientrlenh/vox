package com.sep.vox.application.port.input.usecase.exam;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CompleteExamSessionGradingCommand;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ConsumeQuotaUseCase;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

// Internal service-to-service use case (called sau khi pipeline chấm bài AI hoàn tất), không end-user-facing
@Service
public class CompleteExamSessionGradingUseCase implements IUseCase<CompleteExamSessionGradingCommand, Void> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final ConsumeQuotaUseCase consumeQuotaUseCase;

    public CompleteExamSessionGradingUseCase(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            ExamItemResponseRepository examItemResponseRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            ConsumeQuotaUseCase consumeQuotaUseCase) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.consumeQuotaUseCase = consumeQuotaUseCase;
    }

    @Override
    @Transactional
    public Void execute(CompleteExamSessionGradingCommand input) {
        var session = examSessionRepository.findById(input.examSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        if (session.getStatus() != ExamSessionStatus.GRADING) {
            throw new IllegalStateException("Phiên thi hiện không ở trạng thái đang chấm");
        }

        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(exam.getSchoolId())
            .orElseThrow(() -> new NotFoundException("Trường chưa có gói subscription đang hoạt động"));

        var totalDurationSeconds = examItemResponseRepository.sumDurationSecondsBySessionId(session.getId());
        if (totalDurationSeconds > 0) {
            consumeQuotaUseCase.execute(new ConsumeQuotaCommand(
                subscription.getId(), session.getId(), QuotaType.GRADING, totalDurationSeconds
            ));
        }

        session.setStatus(ExamSessionStatus.GRADED);
        examSessionRepository.save(session);
        return null;
    }
}