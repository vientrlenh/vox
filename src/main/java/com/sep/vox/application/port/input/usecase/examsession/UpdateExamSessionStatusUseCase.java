package com.sep.vox.application.port.input.usecase.examsession;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitExamSessionCommand;
import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examsession.ExamSessionResponse;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class UpdateExamSessionStatusUseCase implements IUseCase<UpdateExamSessionStatusCommand, ExamSessionResponse> {

    private final ExamSessionRepository examSessionRepository;
    private final SubmitExamSessionUseCase submitExamSessionUseCase;

    public UpdateExamSessionStatusUseCase(
            ExamSessionRepository examSessionRepository,
            SubmitExamSessionUseCase submitExamSessionUseCase) {
        this.examSessionRepository = examSessionRepository;
        this.submitExamSessionUseCase = submitExamSessionUseCase;
    }

    @Override
    public ExamSessionResponse execute(UpdateExamSessionStatusCommand input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));

        validateTransition(session.getStatus(), input.status());
        session.setStatus(input.status());
        if ((input.status() == ExamSessionStatus.SUBMITTED || input.status() == ExamSessionStatus.EXPIRED)
                && session.getSubmittedAt() == null) {
            session.setSubmittedAt(OffsetDateTime.now());
        }

        examSessionRepository.save(session);
        if (input.status() == ExamSessionStatus.SUBMITTED && !session.isFlagged()) {
            submitExamSessionUseCase.execute(new SubmitExamSessionCommand(session.getId()));
        }

        return CreateExamSessionUseCase.toResponse(
            examSessionRepository.findById(session.getId()).orElse(session)
        );
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
