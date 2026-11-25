package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentgrading.AssignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * The school admin manually assigns teachers to each attempt, in one of the four grading rounds. Accepts a batch
 * so the admin can tick several rows and assign them at once.
 *
 * <p>Validates everything before writing: a wrong row must not leave a half-done assignment
 * from the rows before it.
 *
 * <p>Every lookup is batched so there is no N+1 by number of attempts: results and assessments are loaded in batches,
 * the already-assigned check is one query, and teacher validity is grouped by <em>school</em>
 * (not calling {@code isTeacherOfSchool} repeatedly per person). Permission checking also runs
 * only once per distinct school -- usually one.
 *
 * <p>The {@code APPEAL} round is NOT assigned through here: it is tied to a specific appeal so it
 * goes through {@code AssignAssessmentAppealReviewerUseCase}, the only place that knows the
 * conflict-of-interest rule.
 */
@Service
@RequiredArgsConstructor
public class AssignGradingUseCase implements IUseCase<AssignGradingCommand, List<UUID>> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final GradingAssignmentNotificationService gradingAssignmentNotificationService;

    @Override
    @Transactional
    public List<UUID> execute(AssignGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        GradingRoundType roundType = requireNotAppealRound(command.roundType());
        Instant deadlineAt = validateDeadline(command.deadlineAt());
        List<AssignGradingCommand.AssignmentItem> items = requireAssignmentItems(command.assignments());
        List<UUID> candidateResultIds = items.stream()
            .map(item -> item.candidateResultId()).toList();
        if (new HashSet<>(candidateResultIds).size() != candidateResultIds.size()) {
            throw new ResourceDuplicatedException("Không được phân công trùng bài thi.");
        }

        AssignmentLookups lookups = loadLookups(items, candidateResultIds, currentUserId);
        Instant now = Instant.now();
        List<AssessmentGradingAssignment> assignments =
            buildAssignments(items, roundType, lookups, now, currentUserId, deadlineAt);

        List<AssessmentGradingAssignment> saved = assessmentGradingAssignmentRepository.saveAll(assignments);

        // After saveAll: the notification needs the id of the assignment row to lead the teacher to the right attempt.
        gradingAssignmentNotificationService.publishAssigned(saved, now);

        return saved.stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    private List<AssignGradingCommand.AssignmentItem> requireAssignmentItems(
            List<AssignGradingCommand.AssignmentItem> requestedItems) {
        List<AssignGradingCommand.AssignmentItem> items = requestedItems == null
            ? List.<AssignGradingCommand.AssignmentItem>of() : requestedItems;
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một bài thi để phân công.");
        }
        for (AssignGradingCommand.AssignmentItem item : items) {
            if (item.candidateResultId() == null || item.teacherId() == null) {
                throw new IllegalArgumentException("Thiếu bài thi hoặc giáo viên cần phân công.");
            }
        }
        return items;
    }

    private AssignmentLookups loadLookups(
            List<AssignGradingCommand.AssignmentItem> items, List<UUID> candidateResultIds, UUID currentUserId) {
        Map<UUID, AssessmentCandidateResult> resultsById = loadResultsById(candidateResultIds);
        Map<UUID, UUID> schoolIdByResultId = schoolIdByResultId(resultsById);
        Set<UUID> distinctSchoolIds = new HashSet<>(schoolIdByResultId.values());
        authorizeAssigner(distinctSchoolIds, candidateResultIds, currentUserId);
        return new AssignmentLookups(
            resultsById,
            openAssignedResultIds(candidateResultIds),
            schoolIdByResultId,
            validTeachersBySchool(items, distinctSchoolIds));
    }

    private Map<UUID, AssessmentCandidateResult> loadResultsById(List<UUID> candidateResultIds) {
        Map<UUID, AssessmentCandidateResult> resultsById = assessmentCandidateResultRepository.findByIdIn(candidateResultIds).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));
        for (UUID candidateResultId : candidateResultIds) {
            if (!resultsById.containsKey(candidateResultId)) {
                throw new ResourceNotFoundException("Không tìm thấy kết quả bài thi.");
            }
        }
        return resultsById;
    }

    /** The school of each attempt; also the distinct school set used for authorization and teacher filtering. */
    private Map<UUID, UUID> schoolIdByResultId(Map<UUID, AssessmentCandidateResult> resultsById) {
        Map<UUID, UUID> schoolIdByAssessmentId = assessmentRepository.findByIdIn(
                resultsById.values().stream().map(result -> result.getAssessmentId()).distinct().toList()).stream()
            .collect(Collectors.toMap(assessment -> assessment.getId(), assessment -> assessment.getSchoolId(), (left, right) -> left));
        Map<UUID, UUID> schoolIdByResultId = new HashMap<UUID, UUID>();
        for (AssessmentCandidateResult result : resultsById.values()) {
            UUID schoolId = schoolIdByAssessmentId.get(result.getAssessmentId());
            if (schoolId == null) {
                throw new ResourceNotFoundException("Không tìm thấy bài kiểm tra của kết quả này.");
            }
            schoolIdByResultId.put(result.getId(), schoolId);
        }
        return schoolIdByResultId;
    }

    /** Authorization runs once per distinct school (usually one). */
    private void authorizeAssigner(Set<UUID> distinctSchoolIds, List<UUID> candidateResultIds, UUID currentUserId) {
        for (UUID schoolId : distinctSchoolIds) {
            assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);
        }
        // A class test takes its own path: its owner claims it through /class-tests/{id}/grading/claim.
        assessmentGradingAccessService.rejectClassTestCoordination(candidateResultIds);
    }

    /** Results that already have an OPEN assignment: one query for the whole batch. */
    private Set<UUID> openAssignedResultIds(List<UUID> candidateResultIds) {
        return assessmentGradingAssignmentRepository
            .findOpenByCandidateResultIdIn(candidateResultIds).stream()
            .map(assignment -> assignment.getCandidateResultId())
            .collect(Collectors.toSet());
    }

    /** The valid teachers of each school, one query per school. */
    private Map<UUID, Set<UUID>> validTeachersBySchool(
            List<AssignGradingCommand.AssignmentItem> items, Set<UUID> distinctSchoolIds) {
        List<UUID> teacherIds = items.stream()
            .map(item -> item.teacherId()).distinct().toList();
        Map<UUID, Set<UUID>> validTeachersBySchool = new HashMap<UUID, Set<UUID>>();
        for (UUID schoolId : distinctSchoolIds) {
            validTeachersBySchool.put(schoolId,
                assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds));
        }
        return validTeachersBySchool;
    }

    private List<AssessmentGradingAssignment> buildAssignments(List<AssignGradingCommand.AssignmentItem> items,
            GradingRoundType roundType, AssignmentLookups lookups, Instant now, UUID currentUserId,
            Instant deadlineAt) {
        List<AssessmentGradingAssignment> assignments = new ArrayList<AssessmentGradingAssignment>();
        for (AssignGradingCommand.AssignmentItem item : items) {
            AssessmentCandidateResult result = lookups.resultsById().get(item.candidateResultId());
            requireAssignable(item, result, roundType, lookups);
            // scoreBefore is captured RIGHT AT ASSIGNMENT: it is the reference point for measuring "how far
            // the AI / previous round was off" after the teacher finishes grading. Taken later, it would
            // already have been changed by the teacher themself.
            assignments.add(AssessmentGradingAssignment.open(
                item.candidateResultId(),
                item.teacherId(),
                roundType,
                null,
                result.getTotalScore(),
                now,
                currentUserId,
                deadlineAt
            ));
        }
        return assignments;
    }

    private void requireAssignable(
            AssignGradingCommand.AssignmentItem item,
            AssessmentCandidateResult result,
            GradingRoundType roundType,
            AssignmentLookups lookups) {
        if (!GradingRoundPolicy.isAssignable(roundType, result.getStatus())) {
            throw new IllegalArgumentException(
                "Bài thi không ở trạng thái phù hợp với vòng chấm " + roundType.name() + ".");
        }
        // Pre-check so the error message is in Vietnamese instead of letting the unique index throw
        // DataIntegrityViolation; the index remains the last safeguard when there is a race.
        if (lookups.alreadyAssigned().contains(item.candidateResultId())) {
            throw new ResourceDuplicatedException("Bài thi này đang được một giáo viên chấm.");
        }
        UUID schoolId = lookups.schoolIdByResultId().get(item.candidateResultId());
        if (!lookups.validTeachersBySchool().getOrDefault(schoolId, Set.of()).contains(item.teacherId())) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }
    }

    private GradingRoundType requireNotAppealRound(GradingRoundType roundType) {
        if (roundType == GradingRoundType.APPEAL) {
            throw new IllegalArgumentException(
                "Vòng phúc khảo được phân công từ màn đơn phúc khảo, không phân công ở đây.");
        }
        return roundType;
    }

    private Instant validateDeadline(Instant deadlineAt) {
        if (deadlineAt != null && deadlineAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Hạn chấm phải ở tương lai.");
        }
        return deadlineAt;
    }

    private record AssignmentLookups(
        Map<UUID, AssessmentCandidateResult> resultsById,
        Set<UUID> alreadyAssigned,
        Map<UUID, UUID> schoolIdByResultId,
        Map<UUID, Set<UUID>> validTeachersBySchool
    ) {
    }
}
