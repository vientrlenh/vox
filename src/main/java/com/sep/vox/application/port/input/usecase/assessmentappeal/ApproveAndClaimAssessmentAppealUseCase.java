package com.sep.vox.application.port.input.usecase.assessmentappeal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.assessmentappeal.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.assessmentappeal.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;

/**
 * Approves an appeal and claims the regrading in ONE operation.
 *
 * <p>For a class test, the approver and the grader are always the same person -- the owner of the
 * test. Splitting it into two steps (approve, then assign) only creates a redundant operation and leaves the appeal stuck at
 * {@code APPROVED} when the teacher forgets the second step.
 *
 * <p>This is a <em>compose</em> use case: it calls {@link ApproveAssessmentAppealUseCase} and
 * {@link AssignAssessmentAppealReviewerUseCase} again instead of stuffing flags into them -- the school administrator flow
 * still needs exactly two separate steps (ask for the deadline, then choose a grader). The two child use cases
 * are injected beans so they run in the same {@code REQUIRED} transaction: if the assignment step
 * fails the approval step rolls back with it and the appeal stays {@code PENDING} to be retried -- very different from
 * the FE calling two APIs in a row and leaving the appeal stuck in the middle.
 */
@Service
@RequiredArgsConstructor
public class ApproveAndClaimAssessmentAppealUseCase implements IUseCase<UUID, UUID> {

    /** Fixed reason for the conflict-of-interest rule: a class test can only be graded by its owner. */
    static final String SELF_CLAIM_OVERRIDE_REASON =
        "Class test: the teacher in charge of the test claims the appeal grading.";
    static final int DEFAULT_DEADLINE_DAYS = 3;
    static final LocalTime DEFAULT_DEADLINE_TIME = LocalTime.of(17, 0);

    private final ApproveAssessmentAppealUseCase approveAssessmentAppealUseCase;
    private final AssignAssessmentAppealReviewerUseCase assignAssessmentAppealReviewerUseCase;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    /** @return the id of the ASSIGNMENT ROW just opened (same as {@code /reviewer}), not the appeal id. */
    @Override
    @Transactional
    public UUID execute(UUID appealId) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(appealId);
        // Only the owner of the class test can claim the grading. Without this check here a school admin would fall
        // all the way into Assign and get the message "the grader must be a teacher of the same school" --
        // which does not explain the real problem.
        if (!assessmentAppealAccessService.isClassTestChair(context, currentUserId)) {
            throw new ForbiddenException(
                "Only the teacher in charge of the test can claim the grading. "
                    + "A school admin should approve the appeal and then assign a grader.");
        }

        Instant deadline = defaultDeadline();
        approveAssessmentAppealUseCase.execute(new ApproveAssessmentAppealCommand(appealId, deadline));
        return assignAssessmentAppealReviewerUseCase.execute(new AssignAssessmentAppealReviewerCommand(
            appealId, currentUserId, SELF_CLAIM_OVERRIDE_REASON, deadline));
    }

    /** Same default as the old approval dialog: T+3 at 17:00 Vietnam time. */
    private Instant defaultDeadline() {
        return LocalDate.now(DateMapper.DEFAULT_INPUT_ZONE)
            .plusDays(DEFAULT_DEADLINE_DAYS)
            .atTime(DEFAULT_DEADLINE_TIME)
            .atZone(DateMapper.DEFAULT_INPUT_ZONE)
            .toInstant();
    }
}
