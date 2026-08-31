package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RetryGradingExamSessionCommand;
import com.sep.vox.application.port.input.command.SubmitExamSessionCommand;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class RetryGradingExamSessionUseCase implements IUseCase<RetryGradingExamSessionCommand, java.util.UUID> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamRepository examRepository;
    private final ExamSessionModerationAccessService moderationAccessService;
    private final UserContextPort userContextPort;
    private final SubmitExamSessionUseCase submitExamSessionUseCase;

    public RetryGradingExamSessionUseCase(
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            ExamSessionModerationAccessService moderationAccessService,
            UserContextPort userContextPort,
            SubmitExamSessionUseCase submitExamSessionUseCase) {
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.moderationAccessService = moderationAccessService;
        this.userContextPort = userContextPort;
        this.submitExamSessionUseCase = submitExamSessionUseCase;
    }

    @Override
    @Transactional
    public java.util.UUID execute(RetryGradingExamSessionCommand input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của phiên thi"));

        moderationAccessService.authorize(exam, candidate);
        if (exam.getStatus() == ExamStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Kỳ thi đã công bố điểm, không thể chấm lại");
        }
        // G.4: ngoài GRADING_FAILED (lỗi chấm kỹ thuật) còn cho phép chấm AI lần đầu cho 1
        // session từng bị đánh dấu vi phạm oan (INVALID) - đã được dỡ cấm - và CHƯA từng có
        // ExamItemEvaluation nào (nếu đã từng có, phải recompute từ dữ liệu cũ thay vì gọi AI
        // lại, xem UnblockExamCandidateUseCase).
        var result = examCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        var isUnblockedInvalidRetry = result != null
            && result.getStatus() == ExamCandidateResultStatus.INVALID
            && candidate.getBlockedAt() == null;
        if (session.getStatus() != ExamSessionStatus.GRADING_FAILED && !isUnblockedInvalidRetry) {
            throw new IllegalStateException("Chỉ có thể chấm lại phiên thi đang lỗi chấm");
        }

        // Định mức chỉ áp cho phía TRƯỜNG, và chỉ cho lượt chấm lại thật (phiên đang lỗi chấm).
        //
        // Quản trị hệ thống không tiêu vào định mức này: lượt của họ là lượt khắc phục sự cố nền
        // tảng, và nếu nó đốt định mức của trường thì một lần AI hỏng hàng loạt sẽ ép hàng trăm bài
        // sang chấm tay vì lỗi của chính mình. Cùng lý do sẽ áp cho job tự chạy lại sau này.
        //
        // Nhánh INVALID được dỡ cấm KHÔNG tính: đó là lần chấm AI ĐẦU TIÊN của phiên, không phải
        // chấm lại sau khi hỏng.
        var schoolInitiated = !userContextPort.isSystemAdmin();
        if (schoolInitiated && session.getStatus() == ExamSessionStatus.GRADING_FAILED) {
            if (!session.canSchoolRequestAiRegrade()) {
                throw new IllegalStateException(
                    "Đã dùng hết lượt nhờ AI chấm lại cho phiên thi này, vui lòng chuyển sang chấm tay"
                );
            }
            session.setSchoolRegradeCount(session.getSchoolRegradeCount() + 1);
            examSessionRepository.save(session);
        }

        submitExamSessionUseCase.execute(new SubmitExamSessionCommand(session.getId()));
        return session.getId();
    }
}
