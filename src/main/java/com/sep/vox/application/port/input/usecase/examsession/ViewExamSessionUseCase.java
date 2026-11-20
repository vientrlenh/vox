package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewExamSessionQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examsession.ExamSessionResponse;
import com.sep.vox.domain.repository.ExamCandidateRepository;

@Service
public class ViewExamSessionUseCase implements IUseCase<ViewExamSessionQuery, ExamSessionResponse> {

    private final ExamResultAccessService examResultAccessService;
    private final ExamCandidateRepository examCandidateRepository;

    public ViewExamSessionUseCase(
            ExamResultAccessService examResultAccessService,
            ExamCandidateRepository examCandidateRepository) {
        this.examResultAccessService = examResultAccessService;
        this.examCandidateRepository = examCandidateRepository;
    }

    @Override
    public ExamSessionResponse execute(ViewExamSessionQuery input) {
        var session = examResultAccessService.getAuthorizedSession(input.sessionId());
        var candidateBlocked = examCandidateRepository.findById(session.getCandidateId())
            .map(candidate -> candidate.getBlockedAt() != null)
            .orElse(false);
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
            candidateBlocked
        );
    }
}
