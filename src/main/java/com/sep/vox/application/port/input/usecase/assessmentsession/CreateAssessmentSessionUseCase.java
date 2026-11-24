package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.CreateAssessmentSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        assessmentCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh được gán"));
        assessmentPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));

        Instant now = Instant.now();
        AssessmentSession session = AssessmentSession.builder()
                .assessmentId(input.assessmentId())
                .candidateId(input.candidateId())
                .paperId(input.paperId())
                .startedAt(now)
                .submittedAt(null)
                .status(AssessmentSessionStatus.IN_PROGRESS)
                .flagged(false)
                .flagReason(null)
                .build();
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
