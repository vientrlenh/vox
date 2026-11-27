package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentgrading.RemoveGradingAssignmentCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

/**
 * Removing an assignment = deleting the row. There is no "removed" status: the submission goes back to exactly the
 * unassigned state, and the unique index on {@code active_result_id} is free again to assign someone else.
 *
 * <p>It is one of three paths that close an assignment WITHOUT going through
 * {@code GradingActionSupport.finish()}, so the release of the appeal has to be done here by itself.
 */
@Service
@RequiredArgsConstructor
public class RemoveGradingAssignmentUseCase implements IUseCase<RemoveGradingAssignmentCommand, UUID> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional
    public UUID execute(RemoveGradingAssignmentCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.load(command.assignmentId());
        assessmentGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);
        assessmentGradingAccessService.rejectClassTestCoordination(context.candidateResult().getId());

        AssessmentGradingAssignment assignment = context.assignment();
        // Once grading is finished the score is already written and the attempt published -- deleting the
        // assignment row now only erases the trace of who graded, and undoes nothing.
        if (assignment.isCompleted()) {
            throw new IllegalArgumentException("The assignment of a submission that has been graded cannot be removed.");
        }
        releaseAppeal(assignment);

        assessmentGradingAssignmentRepository.deleteById(command.assignmentId());
        return command.assignmentId();
    }

    /**
     * Deleting the row also loses the pointer to the appeal, so the appeal must be released BEFORE:
     * an appeal stuck in GRADING can neither be reassigned (it only accepts APPROVED) nor be reviewed
     * again (it only accepts PENDING) -- the only way out is a manual fix in the database.
     */
    private void releaseAppeal(AssessmentGradingAssignment assignment) {
        if (assignment.getRoundType() == GradingRoundType.APPEAL && assignment.getAppealId() != null) {
            assessmentResultAppealRepository.findById(assignment.getAppealId())
                .filter(appeal -> appeal.getStatus() == AssessmentAppealStatus.GRADING)
                .ifPresent(appeal -> {
                    appeal.setStatus(AssessmentAppealStatus.APPROVED);
                    assessmentResultAppealRepository.save(appeal);
                });
        }
    }
}
