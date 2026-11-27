package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.assessmentgrading.ReclaimOverdueAssignmentsCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.application.response.input.assessmentgrading.ReclaimOverdueResponse;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

/**
 * Reclaims overdue assignments, and reassigns right away if the admin has chosen a replacement.
 *
 * <p>Reclaim = close the old row with {@code outcome = DECLINED} instead of deleting it: the trace
 * "this person was assigned and did not finish in time" must be kept, both to reassign to the right person and to
 * see who is overloaded.
 *
 * <p>Close the old row FIRST and only then open the new one, and the adapter's {@code save} flushes immediately
 * -- otherwise the unique index on {@code active_result_id} would block the new row because the old one
 * has not yet released the slot.
 */
@Service
@RequiredArgsConstructor
public class ReclaimOverdueAssignmentsUseCase
        implements IUseCase<ReclaimOverdueAssignmentsCommand, ReclaimOverdueResponse> {

    private static final String RECLAIM_REASON = "Reclaimed because the grading deadline passed.";

    /**
     * Cap on one reclaim run. The "unassigned" branch runs inside a WRITE transaction, so without
     * a cap a school with a large backlog holds a connection and locks for a very long time. Fetching
     * exactly one extra row tells whether more remain, without an additional COUNT query.
     */
    private static final int MAX_RECLAIM_BATCH = 500;

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final RoundRobinLoadBalancer roundRobinLoadBalancer;

    @Override
    @Transactional
    public ReclaimOverdueResponse execute(ReclaimOverdueAssignmentsCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);

        Instant now = Instant.now();
        requireFutureDeadline(command.newDeadlineAt(), now);
        List<UUID> teacherIds = requireReplacementTeachers(command.reassignToTeacherIds(), schoolId);

        OverdueBatch batch = selectOverdue(command, schoolId, currentUserId, now);
        List<AssessmentGradingAssignment> overdue = batch.assignments();
        if (overdue.isEmpty()) {
            return ReclaimOverdueResponse.empty();
        }

        rejectConflictedReplacements(overdue, teacherIds);
        List<UUID> reclaimedIds = declineOverdue(overdue, now);
        if (teacherIds.isEmpty()) {
            releaseAppeals(overdue);
            return new ReclaimOverdueResponse(reclaimedIds, List.of(), batch.hasMore());
        }

        List<UUID> reassignedIds = reopenForReplacements(overdue, teacherIds, command, currentUserId, now);
        return new ReclaimOverdueResponse(reclaimedIds, reassignedIds, batch.hasMore());
    }

    /**
     * Used when there is no replacement: the appeal must be released, otherwise it stays stuck in
     * GRADING with no open assignment left to continue grading it.
     */
    private void releaseAppeals(List<AssessmentGradingAssignment> overdue) {
        overdue.forEach(this::releaseAppeal);
    }

    private void requireFutureDeadline(Instant newDeadlineAt, Instant now) {
        if (newDeadlineAt != null && newDeadlineAt.isBefore(now)) {
            throw new IllegalArgumentException("The new grading deadline must be in the future.");
        }
    }

    private List<UUID> requireReplacementTeachers(List<UUID> requestedTeacherIds, UUID schoolId) {
        List<UUID> teacherIds = requestedTeacherIds == null ? List.<UUID>of() : requestedTeacherIds;
        if (!teacherIds.isEmpty()) {
            if (new HashSet<>(teacherIds).size() != teacherIds.size()) {
                throw new ResourceDuplicatedException("The same teacher must not be selected twice.");
            }
            Set<UUID> validTeachers = assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds);
            if (!validTeachers.containsAll(teacherIds)) {
                throw new IllegalArgumentException("The grader must be a teacher of the same school as the assessment.");
            }
        }
        return teacherIds;
    }

    private List<UUID> declineOverdue(List<AssessmentGradingAssignment> overdue, Instant now) {
        for (AssessmentGradingAssignment assignment : overdue) {
            assignment.complete(GradingOutcome.DECLINED, RECLAIM_REASON, now);
            assessmentGradingAssignmentRepository.save(assignment);
        }
        return overdue.stream().map(assignment -> assignment.getId()).toList();
    }

    /**
     * The load is read AFTER the old rows are closed, so a person who has just been reclaimed is no
     * longer counted as holding those attempts -- redistributing is only fair that way.
     */
    private List<UUID> reopenForReplacements(
            List<AssessmentGradingAssignment> overdue, List<UUID> teacherIds,
            ReclaimOverdueAssignmentsCommand command, UUID currentUserId, Instant now) {
        Map<UUID, Long> loads = assessmentGradingQueryRepository.assignedLoadByTeacherIds(teacherIds);
        List<UUID> workItems = overdue.stream().map(assignment -> assignment.getCandidateResultId()).toList();
        List<UUID> picked = roundRobinLoadBalancer.distribute(workItems, teacherIds, loads);

        List<AssessmentGradingAssignment> reopened = new ArrayList<AssessmentGradingAssignment>();
        for (int index = 0; index < overdue.size(); index++) {
            AssessmentGradingAssignment previous = overdue.get(index);
            reopened.add(AssessmentGradingAssignment.open(
                previous.getCandidateResultId(),
                picked.get(index),
                previous.getRoundType(),
                previous.getAppealId(),
                previous.getScoreBefore(),
                now,
                currentUserId,
                command.newDeadlineAt()
            ));
        }
        return assessmentGradingAssignmentRepository.saveAll(reopened).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    /**
     * One reclaim batch together with the answer to "is there more".
     *
     * @param hasMore the cap {@link #MAX_RECLAIM_BATCH} was reached and some rows are still unprocessed
     */
    private record OverdueBatch(List<AssessmentGradingAssignment> assignments, boolean hasMore) {}

    /**
     * The {@code APPEAL} round shares the conflict-of-interest rule with
     * {@code ReassignGradingUseCase}: a person who has given a score ruling on a submission must not
     * sit and re-examine themselves. BULK reassignment must not be a back door around
     * what row-by-row reassignment already blocks -- and since
     * {@code AutoAssignGradingUseCase} forbids the appeal round entirely, this is the last door.
     *
     * <p>Blocking the whole batch instead of filtering person by person is deliberate: round-robin distributes the
     * submissions after the list is fixed, so filtering person by person would make the distribution depend on hidden
     * data that the admin cannot see when clicking.
     *
     * <p>Only asks when the batch really contains an appeal round -- the other three rounds have no such
     * rule, and asking needlessly adds a query to every reclaim.
     */
    private void rejectConflictedReplacements(List<AssessmentGradingAssignment> overdue, List<UUID> teacherIds) {
        if (teacherIds.isEmpty()) {
            return;
        }
        List<UUID> appealResultIds = overdue.stream()
            .filter(assignment -> assignment.getRoundType() == GradingRoundType.APPEAL)
            .map(assignment -> assignment.getCandidateResultId())
            .distinct()
            .toList();
        for (UUID candidateResultId : appealResultIds) {
            Set<UUID> conflicted = assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId);
            if (conflicted.stream().anyMatch(teacherIds::contains)) {
                throw new IllegalArgumentException(
                    "The replacement teacher list contains someone who has graded a submission that is under appeal "
                        + "so they cannot grade that appeal. Remove this person, or reassign it "
                        + "on the appeal screen if an exception reason has to be recorded.");
            }
        }
    }

    /**
     * Releases the appeal back to {@code APPROVED} so the admin can reassign it. Only called on the
     * plain-reclaim branch: when there is a replacement, the new row is opened right away with the same {@code appealId}
     * in the same transaction, so the appeal staying in {@code GRADING} is still correct.
     *
     * <p>The other three rounds are not attached to an appeal, so this is a no-op for them.
     */
    private void releaseAppeal(AssessmentGradingAssignment assignment) {
        if (assignment.getRoundType() != GradingRoundType.APPEAL || assignment.getAppealId() == null) {
            return;
        }
        assessmentResultAppealRepository.findById(assignment.getAppealId())
            .filter(appeal -> appeal.getStatus() == AssessmentAppealStatus.GRADING)
            .ifPresent(appeal -> {
                appeal.setStatus(AssessmentAppealStatus.APPROVED);
                assessmentResultAppealRepository.save(appeal);
            });
    }

    /**
     * With a designated list: authorize + check overdue row by row; without one, let
     * SQL pre-filter by school (and by assessment if given).
     *
     * <p>The "not designated" branch used to scan {@code findOverdue} across the whole system and then
     * call {@code load()} row by row just to read {@code schoolId} -- 4 queries per row, and it
     * touched other schools' data. The filter now lives in SQL, so here it is ONE query.
     *
     * <p>That branch fetches exactly ONE row more than the cap: if {@code MAX + 1} comes back, we know
     * for sure there is more work, without running an extra COUNT on the same predicate. For a
     * designated list the admin picked the rows themselves, so no cap applies.
     */
    private List<AssessmentGradingAssignment> excludeClassTest(List<AssessmentGradingAssignment> assignments) {
        if (assignments.isEmpty()) {
            return assignments;
        }
        Set<UUID> classTestResultIds = assessmentGradingAccessService.classTestResultIds(
            assignments.stream().map(assignment -> assignment.getCandidateResultId()).toList());
        if (classTestResultIds.isEmpty()) {
            return assignments;
        }
        return assignments.stream()
            .filter(assignment -> !classTestResultIds.contains(assignment.getCandidateResultId()))
            .toList();
    }

    private OverdueBatch selectOverdue(
            ReclaimOverdueAssignmentsCommand command, UUID schoolId, UUID currentUserId, Instant now) {
        List<UUID> assignmentIds = command.assignmentIds() == null ? List.<UUID>of() : command.assignmentIds();
        return assignmentIds.isEmpty()
            ? selectOverdueInSchool(command, schoolId, now)
            : selectNamedAssignments(assignmentIds, currentUserId, now);
    }

    private OverdueBatch selectOverdueInSchool(ReclaimOverdueAssignmentsCommand command, UUID schoolId, Instant now) {
        List<AssessmentGradingAssignment> found = assessmentGradingAssignmentRepository.findOverdueInSchool(
            now, schoolId, command.assessmentId(), MAX_RECLAIM_BATCH + 1);
        // The "reclaim the whole school" branch: FILTER OUT class tests instead of throwing. Throwing here
        // would let one overdue class test block the reclaim button of the whole school.
        found = excludeClassTest(found);
        return found.size() > MAX_RECLAIM_BATCH
            ? new OverdueBatch(found.subList(0, MAX_RECLAIM_BATCH), true)
            : new OverdueBatch(found, false);
    }

    private OverdueBatch selectNamedAssignments(List<UUID> assignmentIds, UUID currentUserId, Instant now) {
        List<AssessmentGradingAssignment> selected = new ArrayList<AssessmentGradingAssignment>();
        for (UUID assignmentId : assignmentIds) {
            AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.load(assignmentId);
            assessmentGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);
            if (!context.assignment().isOverdue(now)) {
                throw new IllegalArgumentException("Only an open and overdue assignment can be reclaimed.");
            }
            selected.add(context.assignment());
        }
        // The opposite of the branch above: the admin ticked each row explicitly, so silently skipping
        // would let them believe it was reclaimed. Here it must be said out loud.
        assessmentGradingAccessService.rejectClassTestCoordination(
            selected.stream().map(assignment -> assignment.getCandidateResultId()).toList());
        return new OverdueBatch(selected, false);
    }
}
