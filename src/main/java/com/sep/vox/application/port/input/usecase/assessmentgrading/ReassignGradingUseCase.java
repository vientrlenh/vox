package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.port.input.command.assessmentgrading.ReassignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

/**
 * Changes the teacher who grades an attempt. It is an UPDATE of the existing row, not adding a second
 * row -- an attempt has only one grader.
 *
 * <p>The {@code APPEAL} round shares the conflict-of-interest rule with
 * {@code AssignAssessmentAppealReviewerUseCase}: a person who has ever written a score verdict for this
 * attempt must not sit and re-inspect themself. There is NO override here -- to make an exception
 * go through the appeal screen, the only place where the reason for the exception can be written onto the appeal.
 */
@Service
@RequiredArgsConstructor
public class ReassignGradingUseCase implements IUseCase<ReassignGradingCommand, UUID> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final GradingAssignmentNotificationService gradingAssignmentNotificationService;

    @Override
    @Transactional
    public UUID execute(ReassignGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        // Lock: otherwise, if the teacher submits just as the admin clicks reassign, the overwriting row
        // would carry an old snapshot (ASSIGNED, no outcome) and bring a graded attempt back to ungraded.
        AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.loadForUpdate(command.assignmentId());
        assessmentGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);

        AssessmentGradingAssignment assignment = context.assignment();
        requireReassignable(assignment, command.teacherId(), context.schoolId());
        handOver(assignment, command.teacherId(), currentUserId);

        // Only notify the NEW person. The old one losing the work is a matter for the coordinator, and a
        // "the work was taken away" notification does not help them do anything.
        gradingAssignmentNotificationService.publishAssigned(List.of(assignment), Instant.now());

        return assignment.getId();
    }

    private void requireReassignable(AssessmentGradingAssignment assignment, UUID teacherId, UUID schoolId) {
        if (assignment.isCompleted()) {
            throw new IllegalArgumentException("Không thể đổi giáo viên cho bài đã chấm xong.");
        }
        if (teacherId == null) {
            throw new IllegalArgumentException("Thiếu giáo viên cần phân công.");
        }
        assessmentGradingAccessService.rejectClassTestCoordination(assignment.getCandidateResultId());
        if (!assessmentGradingAccessService.isTeacherOfSchool(teacherId, schoolId)) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }
        if (assignment.getRoundType() == GradingRoundType.APPEAL) {
            rejectPreviousGrader(assignment, teacherId);
        }
    }

    private void rejectPreviousGrader(AssessmentGradingAssignment assignment, UUID teacherId) {
        Set<UUID> conflicted = assessmentGradingQueryRepository
            .findTeacherIdsWithHumanEvaluation(assignment.getCandidateResultId());
        if (conflicted.contains(teacherId)) {
            throw new IllegalArgumentException(
                "Giáo viên này đã từng chấm bài thi này nên không được chấm phúc khảo. "
                    + "Đổi người chấm ở màn đơn phúc khảo nếu cần ghi lý do ngoại lệ.");
        }
    }

    private void handOver(AssessmentGradingAssignment assignment, UUID teacherId, UUID currentUserId) {
        // assignedAt restarts for the new person: for the recipient the work starts now,
        // and their queue is ordered by that timestamp.
        assignment.setTeacherId(teacherId);
        assignment.setAssignedBy(currentUserId);
        assignment.setAssignedAt(Instant.now());
        // Old deadline but new person: if the reminded mark is not cleared, findDueForReminder
        // (which filters reminded_at IS NULL) skips this row forever and the new person never
        // receives the deadline reminder email. Same reasoning as SetGradingDeadlineUseCase when the deadline changes.
        assignment.setRemindedAt(null);
        assessmentGradingAssignmentRepository.save(assignment);
    }
}
