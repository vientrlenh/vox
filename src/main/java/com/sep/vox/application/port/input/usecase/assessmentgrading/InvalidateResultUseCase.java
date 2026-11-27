package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentResultInvalidatedPayloadV1;
import com.sep.vox.application.port.input.command.assessmentgrading.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * A teacher concludes that the submission is a real violation -> result INVALID, no score entered.
 *
 * <p>Two changes compared with the old version:
 * <ul>
 *   <li><strong>The reason is mandatory</strong> (enforced in {@code GradingRoundPolicy}). The old version allowed
 *       it to be empty, so a later dispute could not be justified.
 *   <li><strong>The {@code session.isFlagged()} requirement is dropped</strong>. Cheating often shows up
 *       when READING THE SUBMISSION, not only when the proctor manages to raise a flag during the assessment; requiring a flag
 *       first forces the teacher either to ignore the violation or to take a detour.
 * </ul>
 *
 * <p>The permission comes from the <em>assignment row</em>, not from the proctor role: a teacher
 * who is assigned a submission can decide exactly that submission.
 */
@Service
@RequiredArgsConstructor
public class InvalidateResultUseCase implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        GradingActionSupport.PreparedAction prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.INVALIDATED, command.reason());

        AssessmentCandidateResult result = prepared.context().candidateResult();
        gradingActionSupport.finish(prepared, result);
        publishInvalidated(prepared, result);

        return new GradingActionResponse(
            command.assignmentId(),
            result.getId(),
            GradingOutcome.INVALIDATED.name(),
            result.getStatus() == null ? null : result.getStatus().name(),
            result.getTotalScore(),
            null
        );
    }

    private void publishInvalidated(GradingActionSupport.PreparedAction prepared, AssessmentCandidateResult result) {
        UUID studentId = gradingActionSupport.resolveStudentId(result);
        if (studentId != null) {
            AssessmentResultInvalidatedPayloadV1 payload = new AssessmentResultInvalidatedPayloadV1(
                result.getId(), studentId, prepared.context().assessmentName(), prepared.reason(),
                result.getSessionId(), prepared.context().assessmentKind());
            outboxRepository.save(Outbox.create(
                AggregateTypeConstant.ASSESSMENT_CANDIDATE_RESULT,
                result.getId(),
                EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED,
                jsonSerializationPort.toJson(payload),
                Instant.now()
            ));
        }
    }
}
