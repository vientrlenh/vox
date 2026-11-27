package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.application.port.input.command.metering.RecordAiUsageCommand;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.ReportAiUsageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.metering.RecordAiUsageUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Receives AI cost reports from a source with no direct Kafka connection -- for example the desktop app that aggregates TTS itself
 * on the learner's machine (the desktop app, LocalAvatarSpeaker.cs) instead of going through the Agentic AI. This REST path is parallel to the
 * Kafka consumer AiUsageRecordedConsumer (sourced from the Agentic AI), and both
 * feed into RecordAiUsageUseCase to share the idempotency logic
 * (uk_ai_usage_record_usage_event_id).
 *
 * <p>Only accepts a report for the calling learner's OWN assessment session, and that session must still be
 * RESUMABLE (in progress) -- the same constraint as UpdateAssessmentSessionRemainingTimeUseCase, because a usage report
 * only makes sense while the session is running.
 */
@Service
@RequiredArgsConstructor
public class ReportAiUsageUseCase implements IUseCase<ReportAiUsageCommand, Void> {

    private final UserContextPort userContextPort;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final RecordAiUsageUseCase recordAiUsageUseCase;

    /**
     * DELIBERATELY NOT @Transactional: every usage event must stand on its own, exactly like the Kafka path
     * (AiUsageRecordedConsumer is not @Transactional, so its loop also gives every event its own
     * transaction in RecordAiUsageUseCase).
     *
     * <p>Wrapping the whole batch in one transaction would let one bad event drag down the events that WERE successfully recorded in the
     * same request. The caller of this path is the desktop app, which reports best-effort and does NOT retry
     * (LocalAvatarSpeaker.ReportTtsUsageBestEffortAsync swallows the error and moves on) -- a rejected batch is real cost
     * that disappears for good, with no second attempt.
     *
     * <p>The two permission checks below only READ, so no outer transaction is needed.
     */
    @Override
    public Void execute(ReportAiUsageCommand input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();

        AssessmentSession session = assessmentSessionRepository.findByIdAndResumable(input.assessmentSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found or already ended"));

        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (!candidate.getStudentId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        for (RecordAiUsageCommand item : input.usageEvents()) {
            recordAiUsageUseCase.execute(item);
        }
        return null;
    }
}
