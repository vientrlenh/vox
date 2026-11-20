package com.sep.vox.application.port.input.usecase.examsession;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examsession.ExamSessionResponse;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class CreateExamSessionUseCase implements IUseCase<CreateExamSessionCommand, ExamSessionResponse> {

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamSessionRepository examSessionRepository;

    public CreateExamSessionUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamPaperRepository examPaperRepository,
            ExamSessionRepository examSessionRepository) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examPaperRepository = examPaperRepository;
        this.examSessionRepository = examSessionRepository;
    }

    @Override
    public ExamSessionResponse execute(CreateExamSessionCommand input) {
        examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        examCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh được gán"));
        examPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));

        var now = Instant.now();
        var session = new ExamSession(
            input.examId(),
            input.candidateId(),
            input.paperId(),
            now,
            null,
            ExamSessionStatus.IN_PROGRESS,
            false,
            null
        );
        return toResponse(examSessionRepository.save(session));
    }

    static ExamSessionResponse toResponse(ExamSession session) {
        return new ExamSessionResponse(
            session.getId(),
            session.getExamId(),
            session.getCandidateId(),
            session.getPaperId(),
            session.getStartedAt() == null ? null : session.getStartedAt().toString(),
            session.getSubmittedAt() == null ? null : session.getSubmittedAt().toString(),
            session.getStatus() == null ? null : session.getStatus().name(),
            session.isFlagged(),
            session.getFlagReason(),
            false
        );
    }
}
