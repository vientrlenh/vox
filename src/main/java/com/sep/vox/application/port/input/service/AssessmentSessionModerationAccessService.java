package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;

@Service
public class AssessmentSessionModerationAccessService {

    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;

    public AssessmentSessionModerationAccessService(
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository) {
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
    }

    public UUID getCurrentUserId() {
        return userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
    }

    public void authorize(Assessment assessment, AssessmentCandidate candidate) {
        var currentUserId = getCurrentUserId();
        if (userContextPort.isSystemAdmin()) {
            return;
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return;
        }

        var allowedAssessmentRole = assessmentMemberRepository.findByAssessmentIdAndUserId(assessment.getId(), currentUserId)
            .map(member -> member.getRole() == AssessmentMemberRole.CHAIR
                || member.getRole() == AssessmentMemberRole.AUTHOR
                || member.getRole() == AssessmentMemberRole.REVIEWER)
            .orElse(false);
        if (allowedAssessmentRole) {
            return;
        }

        if (candidate.getScheduleId() != null
                && assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(candidate.getScheduleId(), currentUserId)) {
            return;
        }

        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
