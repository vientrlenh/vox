package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentgrading.SetGradingDeadlineCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

/**
 * Admin đặt hoặc sửa hạn chấm, một hay nhiều bài cùng lúc.
 *
 * <p>Sửa hạn thì {@code reminded_at} được xoá: hạn mới là một cam kết mới, và giáo
 * viên xứng đáng được nhắc lại theo hạn đó thay vì bị coi như đã nhắc rồi.
 */
@Service
@RequiredArgsConstructor
public class SetGradingDeadlineUseCase implements IUseCase<SetGradingDeadlineCommand, List<UUID>> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional
    public List<UUID> execute(SetGradingDeadlineCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        List<UUID> assignmentIds = command.assignmentIds() == null ? List.<UUID>of() : command.assignmentIds();
        if (assignmentIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một phân công để đặt hạn.");
        }
        if (command.deadlineAt() != null && command.deadlineAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Hạn chấm phải ở tương lai.");
        }

        List<AssessmentGradingAccessService.GradingContext> contexts = loadLockedContexts(assignmentIds);
        authorizeContexts(contexts, currentUserId);

        List<AssessmentGradingAssignment> updated = contexts.stream()
            .map(context -> {
                AssessmentGradingAssignment assignment = context.assignment();
                assignment.setDeadlineAt(command.deadlineAt());
                assignment.setRemindedAt(null);
                return assignment;
            })
            .toList();
        return assessmentGradingAssignmentRepository.saveAll(updated).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    /**
     * Validates everything before writing -- the same mold as AssignGradingUseCase.
     *
     * <p>Locks in the order of the SORTED ids, not the order the client sent: this is a batch lock, so two
     * admins sending two overlapping lists in opposite orders would lock crosswise and deadlock. Sorting first
     * makes every transaction go in the same direction. The input order does not affect the returned
     * result, so re-sorting is harmless.
     */
    private List<AssessmentGradingAccessService.GradingContext> loadLockedContexts(List<UUID> assignmentIds) {
        List<UUID> lockOrderedIds = assignmentIds.stream().sorted().toList();
        List<AssessmentGradingAccessService.GradingContext> contexts = new ArrayList<AssessmentGradingAccessService.GradingContext>();
        for (UUID assignmentId : lockOrderedIds) {
            AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.loadForUpdate(assignmentId);
            if (context.assignment().isCompleted()) {
                throw new IllegalArgumentException("Không thể đặt hạn cho phân công đã chốt.");
            }
            contexts.add(context);
        }
        return contexts;
    }

    /** Authorization runs once per distinct school -- the same mold as AssignGradingUseCase. */
    private void authorizeContexts(List<AssessmentGradingAccessService.GradingContext> contexts, UUID currentUserId) {
        contexts.stream()
            .map(context -> context.schoolId())
            .distinct()
            .forEach(schoolId -> assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId));
        // A class test has no administrative grading deadline -- the school does not set a deadline on behalf of the teacher.
        assessmentGradingAccessService.rejectClassTestCoordination(
            contexts.stream().map(context -> context.candidateResult().getId()).toList());
    }
}
