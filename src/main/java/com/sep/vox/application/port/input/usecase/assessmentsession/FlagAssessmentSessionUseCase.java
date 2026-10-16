package com.sep.vox.application.port.input.usecase.assessmentsession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.FlagAssessmentSessionCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Service
public class FlagAssessmentSessionUseCase implements IUseCase<FlagAssessmentSessionCommand, java.util.UUID> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionModerationAccessService moderationAccessService;

    public FlagAssessmentSessionUseCase(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            AssessmentSessionModerationAccessService moderationAccessService) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.moderationAccessService = moderationAccessService;
    }

    @Override
    @Transactional
    public java.util.UUID execute(FlagAssessmentSessionCommand input) {
        var session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
        var candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của phiên thi"));
        var assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của phiên thi"));

        moderationAccessService.authorize(assessment, candidate);

        if (input.flagged()) {
            var reason = StringNormalization.trimAndCollapseSpaces(input.reason());
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Phải nhập lý do khi đánh dấu nghi vấn");
            }
            session.setFlagReason(reason);
        } else {
            session.setFlagReason(null);
        }
        session.setFlagged(input.flagged());
        assessmentSessionRepository.save(session);
        return session.getId();
    }
}
