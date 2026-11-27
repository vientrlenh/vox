package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.assessmentgrading.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.GradingSampleSelector;
import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

/**
 * Picks a set of submissions and distributes them evenly among the chosen group of teachers.
 *
 * <p>Two separate parts, each one a separately testable service: {@link GradingSampleSelector}
 * decides <em>which submissions to grade</em> (4 modes), {@link RoundRobinLoadBalancer} decides
 * <em>who grades</em> (balanced by real load, not a round robin starting from 0, so a second run
 * does not pile onto the first person in the list).
 *
 * <p>Submissions that already have an open assignment are excluded at the query level, so calling it repeatedly is safe.
 */
@Service
@RequiredArgsConstructor
public class AutoAssignGradingUseCase implements IUseCase<AutoAssignGradingCommand, List<UUID>> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final GradingSampleSelector gradingSampleSelector;
    private final RoundRobinLoadBalancer roundRobinLoadBalancer;
    private final GradingAssignmentNotificationService gradingAssignmentNotificationService;

    @Override
    @Transactional
    public List<UUID> execute(AutoAssignGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);
        requireAssignmentTarget(command);
        GradingRoundType roundType = requireNotAppealRound(command.roundType());
        Instant deadlineAt = validateDeadline(command.deadlineAt());
        List<UUID> teacherIds = requireTeachersOfSchool(command.teacherIds(), schoolId);
        List<UUID> candidates = findAssignableResultIds(command, schoolId, roundType);
        if (candidates.isEmpty()) {
            return List.of();
        }
        // The entry by scheduleId does not go through the assessmentId guard above.
        assessmentGradingAccessService.rejectClassTestCoordination(candidates);
        List<UUID> selected = selectSample(command, candidates);
        if (selected.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();
        List<AssessmentGradingAssignment> assignments =
            buildAssignments(selected, teacherIds, roundType, now, currentUserId, deadlineAt);
        return saveAndNotify(assignments, now);
    }

    private void requireAssignmentTarget(AutoAssignGradingCommand command) {
        if (command.assessmentId() == null && command.scheduleId() == null) {
            throw new IllegalArgumentException("An assessment or an assessment schedule must be selected for automatic assignment.");
        }
        // Blocked right at assessmentId rather than waiting for the attempt list: a class test has already been assigned
        // entirely to its owner so the list would be EMPTY, and "empty" returns success silently --
        // the admin thinks the assignment is done while nothing happened.
        if (assessmentGradingAccessService.isClassTestAssessment(command.assessmentId())) {
            throw new ForbiddenException(AssessmentGradingAccessService.CLASS_TEST_COORDINATION_REJECTION);
        }
    }

    private List<UUID> requireTeachersOfSchool(List<UUID> requestedTeacherIds, UUID schoolId) {
        List<UUID> teacherIds = requestedTeacherIds == null ? List.<UUID>of() : requestedTeacherIds;
        if (teacherIds.isEmpty()) {
            throw new IllegalArgumentException("At least one teacher must be selected for assignment.");
        }
        if (new HashSet<>(teacherIds).size() != teacherIds.size()) {
            throw new ResourceDuplicatedException("The same teacher must not be selected twice.");
        }
        // One query for the whole group instead of repeating isTeacherOfSchool per person (N+1).
        Set<UUID> validTeachers = assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds);
        if (!validTeachers.containsAll(teacherIds)) {
            throw new IllegalArgumentException("The grader must be a teacher of the same school as the assessment.");
        }
        return teacherIds;
    }

    private List<UUID> findAssignableResultIds(
            AutoAssignGradingCommand command, UUID schoolId, GradingRoundType roundType) {
        List<String> eligibleStatuses = GradingRoundPolicy.assignableStatuses(roundType).stream()
            .map(candidateResultStatus -> candidateResultStatus.name())
            .toList();
        return assessmentGradingQueryRepository.findAssignableResultIds(
            schoolId, command.assessmentId(), command.scheduleId(), eligibleStatuses);
    }

    private List<UUID> selectSample(AutoAssignGradingCommand command, List<UUID> candidates) {
        GradingSampleSelectionMode selectionMode = command.selectionMode() == null
            ? GradingSampleSelectionMode.ALL
            : command.selectionMode();
        // Load the risk signals only when the mode really needs them -- a heavy query run for nothing
        // for the other three modes is a clear waste.
        List<GradingRiskInfo> riskInfos = selectionMode == GradingSampleSelectionMode.RISK_BASED
            ? assessmentGradingQueryRepository.findRiskInfos(candidates)
            : List.<GradingRiskInfo>of();
        return gradingSampleSelector.select(
            selectionMode, candidates, command.percent(), command.candidateResultIds(), riskInfos);
    }

    private List<AssessmentGradingAssignment> buildAssignments(List<UUID> selected, List<UUID> teacherIds,
            GradingRoundType roundType, Instant now, UUID currentUserId, Instant deadlineAt) {
        Map<UUID, Long> loads = assessmentGradingQueryRepository.assignedLoadByTeacherIds(teacherIds);
        List<UUID> picked = roundRobinLoadBalancer.distribute(selected, teacherIds, loads);
        Map<UUID, AssessmentCandidateResult> scoreByResultId = scoreBeforeByResultId(selected);

        List<AssessmentGradingAssignment> assignments = new ArrayList<AssessmentGradingAssignment>();
        for (int index = 0; index < selected.size(); index++) {
            UUID candidateResultId = selected.get(index);
            AssessmentCandidateResult result = scoreByResultId.get(candidateResultId);
            assignments.add(AssessmentGradingAssignment.open(
                candidateResultId,
                picked.get(index),
                roundType,
                null,
                result == null ? null : result.getTotalScore(),
                now,
                currentUserId,
                deadlineAt
            ));
        }
        return assignments;
    }

    /** scoreBefore is captured at assignment, in one batch query -- the reference point for measuring the deviation after grading. */
    private Map<UUID, AssessmentCandidateResult> scoreBeforeByResultId(List<UUID> selected) {
        return assessmentCandidateResultRepository.findByIdIn(selected).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));
    }

    private List<UUID> saveAndNotify(List<AssessmentGradingAssignment> assignments, Instant now) {
        List<AssessmentGradingAssignment> saved = assessmentGradingAssignmentRepository.saveAll(assignments);

        gradingAssignmentNotificationService.publishAssigned(saved, now);

        return saved.stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    private GradingRoundType requireNotAppealRound(GradingRoundType roundType) {
        if (roundType == GradingRoundType.APPEAL) {
            throw new IllegalArgumentException(
                "Appeal rounds are assigned from the appeal screen, not here.");
        }
        return roundType;
    }

    private Instant validateDeadline(Instant deadlineAt) {
        if (deadlineAt != null && deadlineAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("The grading deadline must be in the future.");
        }
        return deadlineAt;
    }
}
