package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CheckpointExamSessionRemainingTimeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class CheckpointExamSessionRemainingTimeUseCase
        implements IUseCase<CheckpointExamSessionRemainingTimeCommand, Integer> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final UserContextPort userContextPort;

    public CheckpointExamSessionRemainingTimeUseCase(
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            UserContextPort userContextPort) {
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Integer execute(CheckpointExamSessionRemainingTimeCommand input) {
        if (input.remainingSeconds() < 0) {
            throw new IllegalArgumentException("Thời gian còn lại không được nhỏ hơn 0");
        }

        var session = examSessionRepository.findByIdForUpdate(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        if (!candidate.getStudentId().equals(userContextPort.getCurrentAuthenticatedUserId())) {
            throw new ForbiddenException("Bạn không được phép cập nhật thời gian của phiên thi này");
        }

        var current = session.getRemainingSeconds();
        var remainingSeconds = current == null
            ? input.remainingSeconds()
            : Math.min(current, input.remainingSeconds());
        session.setRemainingSeconds(remainingSeconds);
        examSessionRepository.save(session);
        return remainingSeconds;
    }
}
