package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Service
public class CreateAssessmentSessionUseCase implements IUseCase<CreateAssessmentSessionCommand, AssessmentSessionResponse> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

    public CreateAssessmentSessionUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentSessionRepository assessmentSessionRepository) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
    }

    @Override
    public AssessmentSessionResponse execute(CreateAssessmentSessionCommand input) {
        assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        assessmentCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh được gán"));
        assessmentPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));

        var now = Instant.now();
        var session = new AssessmentSession(
            input.assessmentId(),
            input.candidateId(),
            input.paperId(),
            now,
            null,
            AssessmentSessionStatus.IN_PROGRESS,
            false,
            null
        );
        return toResponse(assessmentSessionRepository.save(session));
    }

    static AssessmentSessionResponse toResponse(AssessmentSession session) {
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
            false
        );
    }
}
