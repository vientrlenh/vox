package com.sep.vox.application.port.input.usecase.examsession;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitExamSessionCommand;
import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examsession.ExamSessionResponse;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class UpdateExamSessionStatusUseCase implements IUseCase<UpdateExamSessionStatusCommand, ExamSessionResponse> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final SubmitExamSessionUseCase submitExamSessionUseCase;

    public UpdateExamSessionStatusUseCase(
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            SubmitExamSessionUseCase submitExamSessionUseCase) {
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.submitExamSessionUseCase = submitExamSessionUseCase;
    }

    @Override
    @Transactional
    public ExamSessionResponse execute(UpdateExamSessionStatusCommand input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));

        // B.4: race - job hết-ca-thi vừa set EXPIRED ngay trước khi client kịp gửi SUBMITTED.
        // Coi là no-op thành công, giữ nguyên EXPIRED (việc chấm đã/sẽ diễn ra qua đường EXPIRED),
        // không throw để học sinh không thấy lỗi ngay lúc tưởng đã nộp bài xong.
        if (session.getStatus() == ExamSessionStatus.EXPIRED && input.status() == ExamSessionStatus.SUBMITTED) {
            return CreateExamSessionUseCase.toResponse(session);
        }

        validateTransition(session.getStatus(), input.status());
        session.setStatus(input.status());
        if ((input.status() == ExamSessionStatus.SUBMITTED || input.status() == ExamSessionStatus.EXPIRED)
                && session.getSubmittedAt() == null) {
            session.setSubmittedAt(Instant.now());
        }

        examSessionRepository.save(session);
        if (input.status() == ExamSessionStatus.SUBMITTED && canSubmitImmediately(session)) {
            submitExamSessionUseCase.execute(new SubmitExamSessionCommand(session.getId()));
        }

        return CreateExamSessionUseCase.toResponse(
            examSessionRepository.findById(session.getId()).orElse(session)
        );
    }

    /**
     * B.0: chỉ blockedAt quyết định hoãn/chấm ngay - flagged chỉ ảnh hưởng việc
     * có tính vào kết quả chính thức hay không (mục G/I), không hoãn việc chấm AI.
     */
    private boolean canSubmitImmediately(com.sep.vox.domain.model.exam.ExamSession session) {
        return examCandidateRepository.findById(session.getCandidateId())
            .map(candidate -> candidate.getBlockedAt() == null)
            .orElse(false);
    }

    private void validateTransition(ExamSessionStatus from, ExamSessionStatus to) {
        if (from == null || to == null) {
            throw new IllegalStateException("Trạng thái phiên thi không hợp lệ");
        }
        if (from == to) {
            return;
        }
        if (isAllowedTransition(from, to)) {
            return;
        }
        throw new IllegalStateException("Không thể chuyển trạng thái phiên thi từ " + from + " sang " + to);
    }

    private boolean isAllowedTransition(ExamSessionStatus from, ExamSessionStatus to) {
        return switch (from) {
            case IN_PROGRESS -> to == ExamSessionStatus.SUBMITTED
                || to == ExamSessionStatus.INTERRUPTED
                || to == ExamSessionStatus.EXPIRED;
            case INTERRUPTED -> to == ExamSessionStatus.IN_PROGRESS || to == ExamSessionStatus.EXPIRED;
            case SUBMITTED -> to == ExamSessionStatus.GRADING;
            case EXPIRED -> to == ExamSessionStatus.GRADING;
            case GRADING -> to == ExamSessionStatus.GRADED || to == ExamSessionStatus.GRADING_FAILED;
            case GRADING_FAILED -> to == ExamSessionStatus.GRADING;
            case GRADED -> to == ExamSessionStatus.GRADING;
        };
    }
}
