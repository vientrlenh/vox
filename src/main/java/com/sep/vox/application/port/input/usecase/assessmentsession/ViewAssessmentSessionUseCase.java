package com.sep.vox.application.port.input.usecase.assessmentsession;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewAssessmentSessionQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;

@Service
public class ViewAssessmentSessionUseCase implements IUseCase<ViewAssessmentSessionQuery, AssessmentSessionResponse> {

    private final AssessmentResultAccessService assessmentResultAccessService;
    private final AssessmentCandidateRepository assessmentCandidateRepository;

    public ViewAssessmentSessionUseCase(
            AssessmentResultAccessService assessmentResultAccessService,
            AssessmentCandidateRepository assessmentCandidateRepository) {
        this.assessmentResultAccessService = assessmentResultAccessService;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
    }

    @Override
    public AssessmentSessionResponse execute(ViewAssessmentSessionQuery input) {
        var session = assessmentResultAccessService.getAuthorizedSession(input.sessionId());
        var candidateBlocked = assessmentCandidateRepository.findById(session.getCandidateId())
            .map(candidate -> candidate.getBlockedAt() != null)
            .orElse(false);
        return new AssessmentSessionResponse(
            session.getId(),
            session.getAssessmentId(),
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
