package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.Assessment;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * Class test: the owner (CHAIR) grades everything, so the {@code INITIAL} round assignment
 * is opened AUTOMATICALLY instead of waiting for the school admin to assign — the admin does not manage
 * class tests, so if we waited the teacher's queue would stay empty forever.
 *
 * <p>A {@code CENTRALIZED} assessment does not go through here: every method is a no-op when
 * {@code kind != CLASS_TEST}, so the school's assignment flow stays unchanged.
 *
 * <p><strong>Can be called multiple times.</strong> Every write path first checks "does the assessment already have an
 * open assignment" — that is the barrier against violating the unique index
 * {@code uq_grading_assignment_active_result}. Thanks to it, two hooks (when the result is created
 * and when the assessment closes) overlapping will not create a second row.
 */
@Service
@RequiredArgsConstructor
public class ClassTestGradingAssignmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;

    /**
     * Opens the assignment for ONE attempt that has just landed in {@code PENDING_REVIEW}.
     *
     * <p>{@code REQUIRED} and not {@code MANDATORY}: the main hook
     * ({@code RecordAssessmentAttemptEvaluationUseCase}) deliberately does NOT run inside an outer
     * transaction -- it splits itself into phases with {@code RequiresNewTransactionPort}. Switching
     * to {@code MANDATORY} would make it throw at run time right there.
     *
     * <p>Consequence: the assignment may commit in its own transaction, separate from the transaction
     * that writes the result. This is acceptable because the function is idempotent, and the catch-up sweep when closing an assessment
     * ({@link #ensureAssignmentsForAssessment}) is the safety net if the process dies in the middle.
     */
    @Transactional
    public void ensureAssignmentForResult(AssessmentCandidateResult result) {
        if (result == null || result.getStatus() != AssessmentCandidateResultStatus.PENDING_REVIEW) {
            return;
        }
        UUID chairId = findClassTestChairId(result.getAssessmentId());
        if (chairId == null) {
            return;
        }
        if (assessmentGradingAssignmentRepository.findOpenByCandidateResultId(result.getId()).isPresent()) {
            return;
        }
        assessmentGradingAssignmentRepository.save(open(result, chairId, Instant.now()));
    }

    /**
     * Catch-up sweep over every {@code PENDING_REVIEW} attempt of one class test. Used when
     * closing the assessment, where results produced late outside the normal flow still exist: attempts
     * from the late-grading job, attempts that were just un-invalidated.
     *
     * <p>Attempts NOT in {@code PENDING_REVIEW} are deliberately outside this sweep: attempts the AI graded
     * cleanly and absent/empty attempts given a score of 0 all go straight to {@code RELEASED}, and opening assignments
     * automatically for them would stuff the queue with a mass of attempts nobody needs to grade. The teacher
     * can still see and claim them on the attempt list screen
     * ({@code classTestGradingResults} → {@code ClaimClassTestGradingUseCase}), through the
     * spot-check round instead of the initial round.
     *
     * @return the number of assignments just opened
     */
    @Transactional
    public int ensureAssignmentsForAssessment(UUID assessmentId) {
        if (assessmentId == null) {
            return 0;
        }
        UUID chairId = findClassTestChairId(assessmentId);
        if (chairId == null) {
            return 0;
        }
        List<AssessmentCandidateResult> pending = assessmentCandidateResultRepository.findByAssessmentId(assessmentId).stream()
            .filter(result -> result.getStatus() == AssessmentCandidateResultStatus.PENDING_REVIEW)
            .toList();
        if (pending.isEmpty()) {
            return 0;
        }
        return openMissingAssignments(pending, chairId);
    }

    private int openMissingAssignments(List<AssessmentCandidateResult> pending, UUID chairId) {
        // One query for the whole batch instead of asking per attempt -- the attempt count equals the class size.
        Set<UUID> alreadyOpen = assessmentGradingAssignmentRepository
            .findOpenByCandidateResultIdIn(pending.stream().map(result -> result.getId()).toList()).stream()
            .map(assignment -> assignment.getCandidateResultId())
            .collect(Collectors.toSet());

        Instant now = Instant.now();
        List<AssessmentGradingAssignment> toOpen = pending.stream()
            .filter(result -> !alreadyOpen.contains(result.getId()))
            .map(result -> open(result, chairId, now))
            .toList();
        if (toOpen.isEmpty()) {
            return 0;
        }
        return assessmentGradingAssignmentRepository.saveAll(toOpen).size();
    }

    /** {@code null} = not a class test, or the assessment has no CHAIR. */
    private UUID findClassTestChairId(UUID assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId).orElse(null);
        if (assessment == null || assessment.getKind() != AssessmentKind.CLASS_TEST) {
            return null;
        }
        return assessmentMemberRepository.findByAssessmentId(assessmentId).stream()
            .filter(member -> member.getRole() == AssessmentMemberRole.CHAIR)
            .map(member -> member.getUserId())
            .findFirst()
            .orElse(null);
    }

    /**
     * {@code deadlineAt = null} — a class test has no administrative grading deadline, and
     * {@code GradingDeadlineReminderJob} only sends mail for rows that HAVE a deadline.
     *
     * <p>{@code assignedBy = chairId}: the system opens it on their behalf, but the person responsible
     * is themselves.
     *
     * <p>{@code scoreBefore} is captured AT OPENING TIME — this is the reference point for measuring the AI ↔ human gap; taken
     * later, it would already have been changed by the teacher.
     */
    private AssessmentGradingAssignment open(AssessmentCandidateResult result, UUID chairId, Instant now) {
        return AssessmentGradingAssignment.open(
            result.getId(),
            chairId,
            GradingRoundType.INITIAL,
            null,
            result.getTotalScore(),
            now,
            chairId,
            null
        );
    }
}
