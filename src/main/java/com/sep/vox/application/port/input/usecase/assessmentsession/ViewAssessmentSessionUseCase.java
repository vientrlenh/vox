package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.AssessmentSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewAssessmentSessionQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentSessionUseCase implements IUseCase<ViewAssessmentSessionQuery, AssessmentSessionResponse> {

    private final AssessmentResultAccessService assessmentResultAccessService;
    private final AssessmentCandidateRepository assessmentCandidateRepository;

    @Override
    public AssessmentSessionResponse execute(ViewAssessmentSessionQuery input) {
        AssessmentSession session = assessmentResultAccessService.getAuthorizedSession(input.sessionId());
        Boolean candidateBlocked = assessmentCandidateRepository.findById(session.getCandidateId())
            .map(candidate -> candidate.getBlockedAt() != null)
            .orElse(false);
        return new AssessmentSessionResponse(
            session.getId(),
            session.getAssessmentId(),
            session.getCandidateId(),
            session.getPaperId(),
            session.getStartedAt(),
            session.getSubmittedAt(),
            session.getStatus(),
            session.isFlagged(),
            session.getFlagReason(),
            candidateBlocked
        );
    }
}
