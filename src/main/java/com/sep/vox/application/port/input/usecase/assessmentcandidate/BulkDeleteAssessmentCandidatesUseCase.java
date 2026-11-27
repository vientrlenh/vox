package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentcandidate.BulkDeleteAssessmentCandidatesCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

/**
 * Deletes a whole group of candidates from an assessment in exactly ONE transaction.
 *
 * <p>The same rule as {@link DeleteAssessmentCandidateUseCase} for one person, only it checks the whole batch
 * with two queries before touching any data. If the UI called the single-person delete endpoint N times, deleting
 * 40 candidates and failing at the 25th would leave a half-finished list -- the same reason
 * {@code BulkAssignAssessmentCandidateScheduleUseCase} exists.
 *
 * <p>All-or-nothing: as soon as ONE candidate in the batch already has an attempt, the whole batch is rejected, with the names
 * of those in the way. Deleting part of them and then reporting a generic error would leave the user not knowing
 * who was deleted and who was not, and this operation cannot be undone.
 */
@Service
@RequiredArgsConstructor
public class BulkDeleteAssessmentCandidatesUseCase implements IUseCase<BulkDeleteAssessmentCandidatesCommand, Void> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(BulkDeleteAssessmentCandidatesCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        authorize(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        List<UUID> candidateIds = input.candidateIds() == null ? List.<UUID>of() : input.candidateIds();
        if (candidateIds.isEmpty()) {
            return null;
        }

        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByIdInAndAssessmentId(candidateIds, assessment.getId());
        // Looked up by (id, assessmentId) so a missing row means an id that does not exist or belongs to another assessment --
        // the whole batch fails instead of silently deleting only part of it.
        if (candidates.size() != candidateIds.stream().distinct().count()) {
            throw new ResourceNotFoundException("Candidate not found");
        }

        requireNoCandidateHasAttempt(candidates);
        assessmentCandidateRepository.deleteByIdIn(candidateIds);
        return null;
    }

    private void requireNoCandidateHasAttempt(List<AssessmentCandidate> candidates) {
        // One query for the whole batch: soft-deleted sessions do not count (findByCandidateIdIn already excludes
        // DELETED), so a candidate whose only attempts were deleted can still be removed from the assessment.
        Set<UUID> busyCandidateIds = assessmentSessionRepository
            .findByCandidateIdIn(candidates.stream().map(candidate -> candidate.getId()).toList())
            .stream()
            .map(session -> session.getCandidateId())
            .collect(Collectors.toSet());

        if (!busyCandidateIds.isEmpty()) {
            // Only the COUNT is named: at this layer only the studentId (a UUID) is known, and printing it
            // would not tell the user who it is. The UI already filters this group out of the delete
            // request, so this is a safety net for a list that just changed, and the user only needs to
            // know they must select again.
            throw new IllegalArgumentException(busyCandidateIds.size()
                + " candidates in the list already have a submission and cannot be removed from the assessment."
                + " Reload the list and select again.");
        }
    }

    /** The same rule as {@link DeleteAssessmentCandidateUseCase}: the school administrator of the school or the council chair. */
    private void authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Access denied");
    }
}
