package com.sep.vox.application.port.input.usecase.assessmentappeal;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentAppealApprovedPayloadV1;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.assessmentappeal.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * Assigns ONE teacher to grade an appeal. The appeal moves {@code APPROVED -> GRADING}.
 *
 * <p>The appeal round is an ordinary {@code assessment_grading_assignments} row
 * ({@code roundType = APPEAL}, {@code appealId} points back to the appeal) -- there is no longer a separate
 * {@code assessment_appeal_reviewers} table. This way the teacher's queue, the grading deadline, overdue
 * reclaiming and the audit share exactly one mechanism with the other three rounds.
 *
 * <p><strong>Conflict-of-interest rule:</strong> whoever has ever written an {@code AssessmentItemEvaluation}
 * with engine HUMAN for this attempt may not grade its appeal -- they would have to re-inspect
 * their own verdict. Someone who only <em>confirmed</em> the old score ({@code UPHOLD}, without
 * writing a new version) is NOT excluded: they have no verdict of their own to defend. A small school
 * with too few teachers can have the admin override it, but a reason must be entered and that reason stays
 * on the appeal.
 */
@Service
@RequiredArgsConstructor
public class AssignAssessmentAppealReviewerUseCase implements IUseCase<AssignAssessmentAppealReviewerCommand, UUID> {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public UUID execute(AssignAssessmentAppealReviewerCommand command) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(command.appealId());
        assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);
        AssessmentResultAppeal appeal = context.appeal();
        requireAssignableReviewer(command, context);
        AssessmentCandidateResult candidateResult = context.candidateResult();
        requireNoOpenAssignment(candidateResult);
        String overrideReason = normalize(command.overrideReason());
        applyConflictOfInterestRule(command, appeal, candidateResult, overrideReason);

        Instant now = Instant.now();
        Instant deadlineAt = requireFutureDeadline(command, appeal, now);

        AssessmentGradingAssignment assignment = openAppealAssignment(
            command, appeal, candidateResult, currentUserId, deadlineAt, now);

        appeal.setStatus(AssessmentAppealStatus.GRADING);
        assessmentResultAppealRepository.save(appeal);
        moveResultToReGrading(candidateResult, currentUserId, overrideReason);
        saveAppealApprovedOutbox(appeal, context, deadlineAt, now);

        return assignment.getId();
    }

    private Instant requireFutureDeadline(
        AssignAssessmentAppealReviewerCommand command, AssessmentResultAppeal appeal, Instant now) {
        Instant deadlineAt = command.deadlineAt() == null ? appeal.getDeadline() : command.deadlineAt();
        if (deadlineAt != null && deadlineAt.isBefore(now)) {
            throw new IllegalArgumentException("The grading deadline must be in the future.");
        }
        return deadlineAt;
    }

    private void requireAssignableReviewer(
        AssignAssessmentAppealReviewerCommand command, AssessmentAppealAccessService.AppealContext context) {
        if (context.appeal().getStatus() != AssessmentAppealStatus.APPROVED) {
            throw new IllegalArgumentException("A grader can only be assigned to an approved appeal.");
        }
        if (command.reviewerId() == null) {
            throw new IllegalArgumentException("The appeal grader is missing.");
        }
        if (command.reviewerId().equals(context.studentId())) {
            throw new IllegalArgumentException("The candidate cannot be assigned as the grader.");
        }
        if (!assessmentAppealAccessService.isTeacherOfSchool(command.reviewerId(), context.schoolId())) {
            throw new IllegalArgumentException("The grader must be a teacher of the same school as the assessment.");
        }
    }

    /**
     * A result that already has another open assignment cannot get a second one. The unique index on
     * active_result_id would block it too, but failing here gives a readable error.
     */
    private void requireNoOpenAssignment(AssessmentCandidateResult candidateResult) {
        if (assessmentGradingAssignmentRepository.findOpenByCandidateResultId(candidateResult.getId()).isPresent()) {
            throw new ResourceDuplicatedException("This submission is being graded by a teacher in another round.");
        }
    }

    /**
     * For a class test the owner of the test assigns themselves, so the conflict-of-interest rule below almost
     * always fires, and that is intended. A reason is needed only when they ALREADY graded this attempt by hand
     * (REGRADED produces a HUMAN evaluation); UPHELD produces no evaluation, so there is nothing to conflict with.
     */
    private void applyConflictOfInterestRule(
        AssignAssessmentAppealReviewerCommand command,
        AssessmentResultAppeal appeal,
        AssessmentCandidateResult candidateResult,
        String overrideReason) {
        Set<UUID> conflicted = assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResult.getId());
        if (!conflicted.contains(command.reviewerId())) {
            return;
        }
        if (overrideReason == null) {
            throw new IllegalArgumentException(
                "This teacher has graded this submission before. Choose someone else, "
                    + "or enter a reason if you still want to assign them.");
        }
        appeal.setReviewerOverrideReason(overrideReason);
    }

    private AssessmentGradingAssignment openAppealAssignment(
        AssignAssessmentAppealReviewerCommand command,
        AssessmentResultAppeal appeal,
        AssessmentCandidateResult candidateResult,
        UUID currentUserId,
        Instant deadlineAt,
        Instant now) {
        return assessmentGradingAssignmentRepository.save(AssessmentGradingAssignment.open(
            candidateResult.getId(),
            command.reviewerId(),
            GradingRoundType.APPEAL,
            appeal.getId(),
            candidateResult.getTotalScore(),
            now,
            currentUserId,
            deadlineAt
        ));
    }

    private void moveResultToReGrading(
        AssessmentCandidateResult candidateResult, UUID currentUserId, String overrideReason) {
        ResultStatusHistoryRecorder.Snapshot before = resultStatusHistoryRecorder.snapshot(candidateResult);
        candidateResult.setStatus(AssessmentCandidateResultStatus.RE_GRADING);
        assessmentCandidateResultRepository.save(candidateResult);
        resultStatusHistoryRecorder.record(
            before, candidateResult, ResultStatusChangeSource.TEACHER_APPEAL, currentUserId, overrideReason);
    }

    /**
     * The student learns that the appeal was accepted and is being re-graded, the most important signal between
     * filing and the result. The outbox row is written inside the assignment transaction so the mail and the
     * assignment live or die together.
     */
    private void saveAppealApprovedOutbox(
        AssessmentResultAppeal appeal,
        AssessmentAppealAccessService.AppealContext context,
        Instant deadlineAt,
        Instant now) {
        AssessmentAppealApprovedPayloadV1 payload = new AssessmentAppealApprovedPayloadV1(
            appeal.getId(), context.studentId(), context.assessmentName(), deadlineAt);
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT_RESULT_APPEAL,
            appeal.getId(),
            EventTypeConstant.ASSESSMENT_APPEAL_APPROVED,
            jsonSerializationPort.toJson(payload),
            now
        ));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
