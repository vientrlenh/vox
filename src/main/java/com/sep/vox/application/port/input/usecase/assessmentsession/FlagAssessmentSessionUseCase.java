package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.FlagAssessmentSessionCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

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
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của phiên thi"));

        moderationAccessService.authorize(assessment, candidate);

        if (input.flagged()) {
            String reason = StringNormalization.trimAndCollapseSpaces(input.reason());
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
