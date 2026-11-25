package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentAppealPublishedPayloadV1;
import com.sep.vox.application.event.AssessmentResultRegradedPayloadV1;
import com.sep.vox.application.event.AssessmentResultReleasedPayloadV1;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * Phần dùng chung của bốn hành động chấm bài: mở đầu (phân quyền + kiểm luật vòng ×
 * hành động) và kết thúc (đổi trạng thái bài, ghi audit, đóng phân công, phát sự kiện).
 *
 * <p>Tồn tại vì bốn use case chỉ khác nhau ở đúng phần giữa. Nếu để mỗi use case tự
 * viết cả preamble lẫn postamble thì bốn bản sao sẽ trôi khỏi nhau, và cái trôi xa
 * nhất sẽ là cái quên ghi audit.
 */
@Service
@RequiredArgsConstructor
public class GradingActionSupport {

    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    /** Ngữ cảnh đã qua mọi kiểm tra, kèm ảnh chụp trạng thái trước khi thao tác. */
    public record PreparedAction(
        GradingContext context,
        UUID currentUserId,
        GradingRoundType roundType,
        GradingOutcome outcome,
        String reason,
        ResultStatusHistoryRecorder.Snapshot before
    ) {
    }

    /**
     * Authorizes, checks the rules and captures the state beforehand. Call BEFORE any change.
     *
     * @throws IllegalArgumentException when the assignment is already finalized, the action is not
     *         valid for the round, or the result has left the status this round handles
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PreparedAction prepare(UUID assignmentId, GradingOutcome outcome, String reason) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        // Lock from here: every check below reads and then decides, so they are only
        // correct if nobody slips in between the read and the write in finish().
        AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.loadForUpdate(assignmentId);
        assessmentGradingAccessService.authorizeAssignedTeacher(context, currentUserId);

        GradingRoundType roundType = requireActionableRound(context, outcome);
        String normalizedReason = reason == null || reason.isBlank() ? null : reason.trim();
        if (GradingRoundPolicy.requiresReason(outcome) && normalizedReason == null) {
            throw new IllegalArgumentException("Phải nhập lý do cho quyết định này.");
        }

        return new PreparedAction(
            context,
            currentUserId,
            roundType,
            outcome,
            normalizedReason,
            resultStatusHistoryRecorder.snapshot(context.candidateResult())
        );
    }

    private GradingRoundType requireActionableRound(GradingContext context, GradingOutcome outcome) {
        AssessmentGradingAssignment assignment = context.assignment();
        if (assignment.isCompleted()) {
            throw new IllegalArgumentException("Bạn đã chốt kết quả cho bài thi này.");
        }
        GradingRoundType roundType = assignment.getRoundType();
        if (roundType == null) {
            throw new IllegalArgumentException("Phân công này thiếu vòng chấm.");
        }
        if (!GradingRoundPolicy.isAllowed(roundType, outcome)) {
            throw new IllegalArgumentException("Hành động này không hợp lệ với vòng chấm hiện tại.");
        }
        // Another flow (appeal, finalization) may have moved the result while the teacher had the
        // grading screen open; the assignment is then still ASSIGNED but there is nothing left to do.
        if (!GradingRoundPolicy.isAssignable(roundType, context.candidateResult().getStatus())) {
            throw new IllegalArgumentException("Bài thi này không còn ở trạng thái xử lý được của vòng chấm.");
        }
        return roundType;
    }

    /**
     * Closes the assignment, changes the result status according to the round x action matrix, writes the audit
     * and publishes the events. Call AFTER the action-specific part has finished writing its data.
     *
     * @param result the result already updated by the use case (new score after recalculation), or
     *               {@code context.candidateResult()} itself when the action does not change the score
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void finish(PreparedAction prepared, AssessmentCandidateResult result) {
        Instant now = Instant.now();
        applyTargetStatus(prepared, result, now);
        recordStatusHistory(prepared, result);

        AssessmentGradingAssignment assignment = prepared.context().assignment();
        assignment.complete(prepared.outcome(), prepared.reason(), now);
        assessmentGradingAssignmentRepository.save(assignment);

        if (prepared.roundType() == GradingRoundType.APPEAL) {
            publishAppeal(prepared, result, now);
        }
        publishScoreEvents(prepared, result, now);
    }

    private void applyTargetStatus(PreparedAction prepared, AssessmentCandidateResult result, Instant now) {
        AssessmentCandidateResultStatus targetStatus = GradingRoundPolicy.resultStatusAfter(prepared.roundType(), prepared.outcome());
        if (targetStatus == null || result.getStatus() == targetStatus) {
            return;
        }
        result.setStatus(targetStatus);
        if (targetStatus == AssessmentCandidateResultStatus.RELEASED && result.getReleasedAt() == null) {
            result.setReleasedAt(now);
        }
        if (targetStatus == AssessmentCandidateResultStatus.INVALID) {
            result.setFinalizedAt(now);
        }
        // Un-invalidating pulls the result back to PENDING_REVIEW: the "finalized" mark of the earlier
        // invalidation is no longer true. Leaving it would create a PENDING_REVIEW result that carries
        // finalized_at -- a trap for the next reader.
        if (targetStatus == AssessmentCandidateResultStatus.PENDING_REVIEW) {
            result.setFinalizedAt(null);
        }
        assessmentCandidateResultRepository.save(result);
    }

    private void recordStatusHistory(PreparedAction prepared, AssessmentCandidateResult result) {
        // The "keep as is" decision at REMEDIATION (confirming a violation) and handing an assignment back
        // must both leave a trace even though the status stays put.
        ResultStatusChangeSource source = ResultStatusChangeSource.ofRound(prepared.roundType());
        if (prepared.outcome() == GradingOutcome.UPHELD || prepared.outcome() == GradingOutcome.DECLINED) {
            resultStatusHistoryRecorder.recordAlways(
                prepared.before(), result, source, prepared.currentUserId(), prepared.reason());
        } else {
            resultStatusHistoryRecorder.record(
                prepared.before(), result, source, prepared.currentUserId(), prepared.reason());
        }
    }

    /**
     * What the teacher submits is published immediately -- there is no longer an admin step that cross-checks and then
     * publishes. This is the only place that closes an appeal, so it sits next to where the assignment is closed instead of
     * being scattered across the action use cases.
     *
     * <p>{@code DECLINED} publishes nothing: when the teacher hands the attempt back the appeal is still waiting for
     * someone else, and only steps back to {@code APPROVED} so the admin can reassign it.
     */
    private void publishAppeal(PreparedAction prepared, AssessmentCandidateResult result, Instant now) {
        UUID appealId = prepared.context().assignment().getAppealId();
        if (appealId == null) {
            return;
        }
        AssessmentResultAppeal appeal = assessmentResultAppealRepository.findById(appealId).orElse(null);
        if (appeal == null) {
            return;
        }
        if (prepared.outcome() == GradingOutcome.DECLINED) {
            appeal.setStatus(AssessmentAppealStatus.APPROVED);
            assessmentResultAppealRepository.save(appeal);
            return;
        }
        appeal.setStatus(AssessmentAppealStatus.PUBLISHED);
        appeal.setScoreAfter(result.getTotalScore());
        appeal.setResolvedBy(prepared.currentUserId());
        appeal.setResolvedAt(now);
        appeal.setDecisionNote(prepared.reason());
        assessmentResultAppealRepository.save(appeal);

        saveAppealPublishedEvent(appeal, prepared, result, now);
    }

    private void saveAppealPublishedEvent(
            AssessmentResultAppeal appeal, PreparedAction prepared, AssessmentCandidateResult result, Instant now) {
        UUID studentId = resolveStudentId(result);
        if (studentId == null) {
            return;
        }
        saveOutbox(
            AggregateTypeConstant.ASSESSMENT_RESULT_APPEAL,
            appeal.getId(),
            EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED,
            new AssessmentAppealPublishedPayloadV1(
                appeal.getId(),
                studentId,
                prepared.context().assessmentName(),
                appeal.getScoreBefore(),
                result.getTotalScore()
            ),
            now
        );
    }

    /**
     * Two different mails for two different situations: the score appears for the first time, and an already
     * published score now changes. A spot-check that keeps the score publishes NOTHING -- bothering the student
     * with a "your score just changed" notice when it did not change is wrong.
     */
    private void publishScoreEvents(PreparedAction prepared, AssessmentCandidateResult result, Instant now) {
        UUID studentId = resolveStudentId(result);
        if (studentId == null) {
            return;
        }
        boolean wasPublished = prepared.before().status() != AssessmentCandidateResultStatus.PENDING_REVIEW;
        boolean nowPublished = result.getStatus() == AssessmentCandidateResultStatus.RELEASED;

        if (nowPublished && !wasPublished) {
            saveResultReleasedEvent(prepared, result, studentId, now);
            return;
        }
        if (wasPublished && hasScoreChanged(prepared, result)) {
            saveResultRegradedEvent(prepared, result, studentId, now);
        }
    }

    private boolean hasScoreChanged(PreparedAction prepared, AssessmentCandidateResult result) {
        return prepared.before().totalScore() == null
            ? result.getTotalScore() != null
            : result.getTotalScore() != null
                && prepared.before().totalScore().compareTo(result.getTotalScore()) != 0;
    }

    private void saveResultReleasedEvent(
            PreparedAction prepared, AssessmentCandidateResult result, UUID studentId, Instant now) {
        saveOutbox(
            AggregateTypeConstant.ASSESSMENT_CANDIDATE_RESULT,
            result.getId(),
            EventTypeConstant.ASSESSMENT_RESULT_RELEASED,
            new AssessmentResultReleasedPayloadV1(
                result.getId(), studentId, prepared.context().assessmentName(), result.getTotalScore(),
                result.getSessionId(), prepared.context().assessmentKind()),
            now
        );
    }

    private void saveResultRegradedEvent(
            PreparedAction prepared, AssessmentCandidateResult result, UUID studentId, Instant now) {
        saveOutbox(
            AggregateTypeConstant.ASSESSMENT_CANDIDATE_RESULT,
            result.getId(),
            EventTypeConstant.ASSESSMENT_RESULT_REGRADED,
            new AssessmentResultRegradedPayloadV1(
                result.getId(),
                studentId,
                prepared.context().assessmentName(),
                prepared.roundType().name(),
                prepared.before().totalScore(),
                result.getTotalScore(),
                result.getSessionId(),
                prepared.context().assessmentKind()
            ),
            now
        );
    }

    /**
     * Writes to the outbox in the SAME transaction that changes the result status: the mail and the data
     * live or die together, with no more "committed, then the mail was lost because SMTP failed".
     */
    private void saveOutbox(String aggregateType, UUID aggregateId, String eventType, Object payload, Instant now) {
        outboxRepository.save(Outbox.create(
            aggregateType,
            aggregateId,
            eventType,
            jsonSerializationPort.toJson(payload),
            now
        ));
    }

    /** The student who receives the email is {@code AssessmentCandidate.studentId}, not the candidateId. */
    public UUID resolveStudentId(AssessmentCandidateResult result) {
        if (result.getCandidateId() == null) {
            return null;
        }
        return assessmentCandidateRepository.findById(result.getCandidateId())
            .map(candidate -> candidate.getStudentId())
            .orElse(null);
    }

    /** Required: used when a missing student is a data fault, not an edge case. */
    public UUID requireStudentId(AssessmentCandidateResult result) {
        UUID studentId = resolveStudentId(result);
        if (studentId == null) {
            throw new ResourceNotFoundException("Không tìm thấy thí sinh của bài thi.");
        }
        return studentId;
    }
}
