package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.CreateAssessmentSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
@RequiredArgsConstructor
public class CreateAssessmentSessionUseCase implements IUseCase<CreateAssessmentSessionCommand, AssessmentSessionResponse> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

    @Override
    public AssessmentSessionResponse execute(CreateAssessmentSessionCommand input) {
        assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        assessmentCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Assigned candidate not found"));
        assessmentPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment paper not found"));

        Instant now = Instant.now();
        AssessmentSession session = AssessmentSession.started(
                input.assessmentId(), input.candidateId(), input.paperId(), now);
        return toResponse(assessmentSessionRepository.save(session));
    }

    static AssessmentSessionResponse toResponse(AssessmentSession session) {
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
            false
        );
    }
}
