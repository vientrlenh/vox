package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.DecideAssessmentCandidateResultOutcomeCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * G.3 addendum: when the assessmentPolicy has no passingScore, finalizeForPublish settles the result
 * at FINAL instead of deriving PASSED/FAILED (there is no threshold to compare with). This use case lets
 * the school decide manually FINAL -> PASSED or FINAL -> FAILED after the assessment has
 * RESULTS_PUBLISHED. Only applies to FINAL -- an already settled PASSED/FAILED (whether automatic
 * or manual) cannot be changed again, to avoid settling over and over.
 */
@Service
@RequiredArgsConstructor
public class DecideAssessmentCandidateResultOutcomeUseCase
        implements IUseCase<DecideAssessmentCandidateResultOutcomeCommand, UUID> {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionModerationAccessService moderationAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public UUID execute(DecideAssessmentCandidateResultOutcomeCommand input) {
        AssessmentCandidateResult result = assessmentCandidateResultRepository.findById(input.candidateResultId())
            .orElseThrow(() -> new ResourceNotFoundException("Submission result not found"));
        AssessmentSession session = assessmentSessionRepository.findById(result.getSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session of this result not found"));
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidate of this result not found"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment of this result not found"));

        moderationAccessService.authorize(assessment, candidate);
        requireDecidable(input, assessment, result);

        Instant now = Instant.now();
        UUID actorId = moderationAccessService.getCurrentUserId();
        ResultStatusHistoryRecorder.Snapshot before = resultStatusHistoryRecorder.snapshot(result);
        result.setStatus(input.decision());
        assessmentCandidateResultRepository.save(result);

        resultStatusHistoryRecorder.record(
            before, result, ResultStatusChangeSource.ASSESSMENT_PUBLISH, actorId, null);
        saveOutcomeDecidedOutbox(input, assessment, candidate, result, now);
        return result.getId();
    }

    private void requireDecidable(
        DecideAssessmentCandidateResultOutcomeCommand input, Assessment assessment, AssessmentCandidateResult result) {
        if (assessment.getStatus() != AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalArgumentException("Pass/fail can only be decided after the assessment results have been published");
        }
        if (result.getStatus() != AssessmentCandidateResultStatus.FINAL) {
            throw new IllegalArgumentException("Pass/fail can only be decided for a result in the FINAL status");
        }
        if (input.decision() != AssessmentCandidateResultStatus.PASSED
                && input.decision() != AssessmentCandidateResultStatus.FAILED) {
            throw new IllegalArgumentException("Invalid decision, only PASSED or FAILED is accepted");
        }
    }

    /**
     * The business behaviour is unchanged; only the trail and the notification are added -- this is the final
     * milestone of the attempt, and the student used to be told nothing about it.
     */
    private void saveOutcomeDecidedOutbox(
        DecideAssessmentCandidateResultOutcomeCommand input,
        Assessment assessment,
        AssessmentCandidate candidate,
        AssessmentCandidateResult result,
        Instant now) {
        AssessmentResultOutcomeDecidedPayloadV1 payload = new AssessmentResultOutcomeDecidedPayloadV1(
            result.getId(), candidate.getStudentId(), assessment.getName(),
            input.decision().name(), result.getTotalScore(),
            result.getSessionId(), assessment.getKind());
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT_CANDIDATE_RESULT,
            result.getId(),
            EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED,
            jsonSerializationPort.toJson(payload),
            now
        ));
    }
}
