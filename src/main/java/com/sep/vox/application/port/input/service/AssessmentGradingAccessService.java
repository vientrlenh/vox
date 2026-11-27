package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.user.User;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Resolves the chain assignment -> candidate result -> session -> assessment -> school and authorizes
 * along it.
 *
 * <p>Kept apart from {@link AssessmentSessionModerationAccessService}: the permission to grade by hand does not come
 * from the proctoring role but from <em>the assignment row itself</em>. A teacher can operate on exactly the attempt
 * they were assigned -- including removing a suspicion flag -- and does not need to be a
 * member of the assessment.
 */
@Service
@RequiredArgsConstructor
public class AssessmentGradingAccessService {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    /**
     * The resolved context of a grading assignment.
     *
     * @param assessmentKind taken ready-made from the Assessment loaded in {@link #loadFromCandidateResult}. The events
     *        published from the grading flow carry this field so that notifications know whether to open the screen of a
     *        centralized assessment or a class test -- the two branches have different routes, and the place
     *        that publishes the event no longer holds the Assessment in hand.
     */
    public record GradingContext(
        AssessmentGradingAssignment assignment,
        AssessmentCandidateResult candidateResult,
        AssessmentSession session,
        UUID schoolId,
        String assessmentName,
        AssessmentKind assessmentKind
    ) {
    }

    public UUID requireActiveUserId() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new UnauthorizedException("Account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is locked.");
        }
        return currentUserId;
    }

    @Transactional(readOnly = true)
    public GradingContext load(UUID assignmentId) {
        AssessmentGradingAssignment assignment = assessmentGradingAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Grading assignment not found."));
        return loadFromCandidateResult(assignment, assignment.getCandidateResultId());
    }

    /**
     * Like {@link #load} but locks the assignment row for writing.
     *
     * <p>Use for every read-modify-write flow on an assignment. Without the lock, two requests
     * reading the same ASSIGNED row both pass checks like {@code isCompleted()}
     * and both write -- and {@code save} here is a {@code merge} on a detached POJO so it overwrites
     * the whole row. The lock makes the later flow wait, re-read the updated state, and throw the proper
     * business message instead of silently grading over it.
     *
     * <p>{@code MANDATORY} because the lock only means something inside the write transaction of the use
     * case: if it opened its own transaction, the lock would be released as soon as this function returns.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public GradingContext loadForUpdate(UUID assignmentId) {
        AssessmentGradingAssignment assignment = assessmentGradingAssignmentRepository.findByIdForUpdate(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Grading assignment not found."));
        return loadFromCandidateResult(assignment, assignment.getCandidateResultId());
    }

    @Transactional(readOnly = true)
    public GradingContext loadByCandidateResultId(UUID candidateResultId) {
        AssessmentGradingAssignment assignment = assessmentGradingAssignmentRepository.findOpenByCandidateResultId(candidateResultId)
            .orElse(null);
        return loadFromCandidateResult(assignment, candidateResultId);
    }

    private GradingContext loadFromCandidateResult(AssessmentGradingAssignment assignment, UUID candidateResultId) {
        AssessmentCandidateResult candidateResult = assessmentCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment result not found."));
        AssessmentSession session = assessmentSessionRepository.findById(candidateResult.getSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found."));
        Assessment assessment = assessmentRepository.findById(candidateResult.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found."));
        return new GradingContext(
            assignment, candidateResult, session, assessment.getSchoolId(), assessment.getName(), assessment.getKind());
    }

    /** School admin of the same school as the assessment. Used for assigning / changing / removing assignments. */
    public void authorizeSchoolAdmin(UUID schoolId, UUID currentUserId) {
        if (isSchoolAdminOfSchool(schoolId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("SECURITY: You do not have permission to assign grading in this school.");
    }

    /**
     * The very teacher assigned to this assessment. This is the one authorization gate for every
     * grading action -- including clearing a flag and marking INVALID.
     *
     * <p>School admin may NOT go through this door: the right to grade comes from the assignment row itself,
     * and the grading round ({@code roundType}) -- which decides which actions are valid and which
     * state the assessment moves to -- lives there too. A school that wants to grade assigns it to
     * itself through {@code authorizeSchoolAdmin}, leaving an assignment row with a clear round.
     */
    public void authorizeAssignedTeacher(GradingContext context, UUID currentUserId) {
        AssessmentGradingAssignment assignment = context.assignment();
        if (assignment == null || !currentUserId.equals(assignment.getTeacherId())) {
            throw new ForbiddenException("SECURITY: You are not assigned to grade this assessment.");
        }
    }

    /**
     * The CHAIR of a class test — the ONE authorization gate for every grading action
     * on a class test.
     *
     * <p>A {@code CENTRALIZED} assessment always returns {@code false} even when the caller is its CHAIR:
     * a centralized assessment is coordinated by the school, and the right to grade comes from the assignment row
     * and not from a role in the assessment.
     */
    public boolean isClassTestChair(UUID assessmentId, UUID currentUserId) {
        if (assessmentId == null || currentUserId == null) {
            return false;
        }
        Assessment assessment = assessmentRepository.findById(assessmentId).orElse(null);
        if (assessment == null || assessment.getKind() != AssessmentKind.CLASS_TEST) {
            return false;
        }
        return assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR);
    }

    public void authorizeClassTestChair(UUID assessmentId, UUID currentUserId) {
        if (isClassTestChair(assessmentId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("SECURITY: You are not the teacher who created this class test.");
    }

    /** School admin of the same school, OR the teacher who created that class test. */
    public void authorizeSchoolAdminOrClassTestChair(UUID schoolId, UUID assessmentId, UUID currentUserId) {
        if (isSchoolAdminOfSchool(schoolId, currentUserId) || isClassTestChair(assessmentId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("SECURITY: You do not have permission to act on this assessment.");
    }

    /**
     * The CHAIR of this assessment, REGARDLESS of the assessment kind -- fixes the permission for the READ paths
     * that are scoped to exactly one assessment.
     *
     * <p>Different from {@link #isClassTestChair} in exactly one point: there is no {@code CLASS_TEST} gate.
     * That gate protects the WRITE permission of the grading flow -- who may grade which attempt, which
     * status an attempt moves to -- and the reason it blocks the chair of a centralized assessment is that a centralized attempt is coordinated by
     * the school. That reason does not apply to reading: the chair can press the publish-results button
     * ({@code UpdateAssessmentStatusUseCase.authorizeMutation}) while
     * {@code requirePublishReadiness} requires every attempt to be RELEASED or INVALID, so not
     * letting them see what is blocking that button would force them to press blind with no way to fix it themselves.
     *
     * <p>⚠️ ONLY for READ use cases. Every grading coordination action (assign / change / remove an
     * assignment, close the books) must still go through {@link #isClassTestChair}: the teacher queue is a
     * resource shared by the whole school and balanced by the school-wide load, so a chair must not
     * spend it on their own assessment.
     */
    public boolean isAssessmentChair(UUID assessmentId, UUID currentUserId) {
        if (assessmentId == null || currentUserId == null) {
            return false;
        }
        return assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR);
    }

    /** A school admin of the same school, OR the chair of that very assessment. READ paths only -- see {@link #isAssessmentChair}. */
    public void authorizeSchoolAdminOrAssessmentChair(UUID schoolId, UUID assessmentId, UUID currentUserId) {
        if (isSchoolAdminOfSchool(schoolId, currentUserId) || isAssessmentChair(assessmentId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("SECURITY: You do not have permission to view the grading data of this assessment.");
    }

    /**
     * Does the result belong to a {@code CLASS_TEST} assessment? Used to BLOCK the school admin from coordinating --
     * a class test is graded by the teacher who created it, and two ownership models overlapping means
     * two parties fighting over the same {@code active_result_id}.
     */
    public boolean isClassTestResult(UUID candidateResultId) {
        if (candidateResultId == null) {
            return false;
        }
        return assessmentCandidateResultRepository.findById(candidateResultId)
            .map(result -> isClassTestAssessment(result.getAssessmentId()))
            .orElse(false);
    }

    /** Like {@link #isClassTestResult} but in batch — two queries for the whole list, no N+1. */
    public Set<UUID> classTestResultIds(Collection<UUID> candidateResultIds) {
        if (candidateResultIds == null || candidateResultIds.isEmpty()) {
            return Set.of();
        }
        List<AssessmentCandidateResult> results = assessmentCandidateResultRepository.findByIdIn(candidateResultIds);
        Set<UUID> classTestAssessmentIds = assessmentRepository
            .findByIdIn(results.stream().map(result -> result.getAssessmentId()).distinct().toList()).stream()
            .filter(assessment -> assessment.getKind() == AssessmentKind.CLASS_TEST)
            .map(assessment -> assessment.getId())
            .collect(Collectors.toSet());
        return results.stream()
            .filter(result -> classTestAssessmentIds.contains(result.getAssessmentId()))
            .map(result -> result.getId())
            .collect(Collectors.toSet());
    }

    public boolean anyClassTestResult(Collection<UUID> candidateResultIds) {
        return !classTestResultIds(candidateResultIds).isEmpty();
    }

    public static final String CLASS_TEST_COORDINATION_REJECTION =
        "A class test is graded by the teacher who created it; the school does not assign graders.";

    /** Blocks the school from coordinating a class test. See {@link #isClassTestResult}. */
    public void rejectClassTestCoordination(UUID candidateResultId) {
        if (isClassTestResult(candidateResultId)) {
            throw new ForbiddenException(CLASS_TEST_COORDINATION_REJECTION);
        }
    }

    /** Same as above but for a whole batch — blocks the whole batch rather than filtering individually, so the result does not depend on hidden data. */
    public void rejectClassTestCoordination(Collection<UUID> candidateResultIds) {
        if (anyClassTestResult(candidateResultIds)) {
            throw new ForbiddenException(CLASS_TEST_COORDINATION_REJECTION);
        }
    }

    public boolean isClassTestAssessment(UUID assessmentId) {
        if (assessmentId == null) {
            return false;
        }
        return assessmentRepository.findById(assessmentId)
            .map(assessment -> assessment.getKind() == AssessmentKind.CLASS_TEST)
            .orElse(false);
    }

    private boolean isSchoolAdminOfSchool(UUID schoolId, UUID currentUserId) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }
        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(schoolId);
    }

    /** The school of the logged-in user — every assignment screen is restricted to it. */
    public UUID requireCurrentSchoolId(UUID currentUserId) {
        return schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("The account does not belong to any school."));
    }

    /** Teacher of the same school — checked before assigning an assessment to them. */
    public boolean isTeacherOfSchool(UUID userId, UUID schoolId) {
        boolean isTeacher = userRoleQueryRepository.findByUserIdWithRoleInfo(userId).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));
        if (!isTeacher) {
            return false;
        }
        return schoolUserRepository.findByUserId(userId)
            .map(schoolUser -> schoolId.equals(schoolUser.getSchoolId()))
            .orElse(false);
    }
}
