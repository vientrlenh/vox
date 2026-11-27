package com.sep.vox.application.port.input.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Who may form the paper council of a centralized assessment, and how far.
 *
 * <p>The school admin of that very school, or the chair of that very assessment — the chair needs
 * this permission because they run the whole process on the assessment detail page, and the first step is
 * calling the paper authors and the paper reviewers into the council.
 *
 * <p>The only boundary between the two roles: <b>only the school admin may touch the CHAIR row</b>. A chair
 * revoking their own role (or another chair's) leaves the assessment without a decision-maker that
 * nobody in the teacher flow can rebuild — appointing a chair must be the school admin's job.
 *
 * <p>The council only exists on centralized assessments; a class test has exactly one CHAIR that
 * {@code CreateClassTestUseCase} assigns itself, so it does not go through here.
 */
@Service
@RequiredArgsConstructor
public class AssessmentMemberManageAccessService {

    private static final String SCHOOL_ADMIN = "SCHOOL_ADMIN";

    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    /**
     * @param schoolAdmin true when the caller is the school admin of the assessment's own school; false means
     *                    they come in here as the council chair.
     */
    public record AssessmentMemberActor(UUID userId, boolean schoolAdmin) {
    }

    /** Throws {@link ForbiddenException} if the caller may not form the council for this assessment. */
    public AssessmentMemberActor requireCanManage(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (assessment.getKind() != AssessmentKind.CENTRALIZED) {
            throw new ForbiddenException("Access denied");
        }
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> SCHOOL_ADMIN.equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return new AssessmentMemberActor(currentUserId, true);
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return new AssessmentMemberActor(currentUserId, false);
        }
        throw new ForbiddenException("Access denied");
    }

    /**
     * Call for <b>every</b> role the operation touches. Changing a member's role touches two roles:
     * the old role (removed) and the new role (assigned) — missing the old role lets a chair demote another chair.
     */
    public void requireCanTouchRole(AssessmentMemberActor actor, AssessmentMemberRole targetRole) {
        if (targetRole == AssessmentMemberRole.CHAIR && !actor.schoolAdmin()) {
            throw new ForbiddenException("Only the school admin may appoint or revoke the council chair");
        }
    }
}
