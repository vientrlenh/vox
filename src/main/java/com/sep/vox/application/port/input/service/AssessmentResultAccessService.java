package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentResultAccessService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    /**
     * The assessment session together with the answers and "is the caller the candidate themself". The status-based visibility
     * rule applies only to the owner, so any use case that needs to tell the difference should use this version
     * instead of looking up {@code AssessmentCandidate} yet again.
     */
    public record SessionAccess(AssessmentSession session, boolean candidateOwner) {
    }

    public record ResponseAccess(AssessmentItemResponse response, SessionAccess sessionAccess) {
    }

    @Transactional(readOnly = true)
    public SessionAccess authorizeSession(UUID sessionId) {
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found"));
        boolean candidateOwner = authorize(session.getAssessmentId(), session.getCandidateId());
        return new SessionAccess(session, candidateOwner);
    }

    @Transactional(readOnly = true)
    public ResponseAccess authorizeResponse(UUID answerId) {
        AssessmentItemResponse response = assessmentItemResponseRepository.findById(answerId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidate's response not found"));
        return new ResponseAccess(response, authorizeSession(response.getSessionId()));
    }

    @Transactional(readOnly = true)
    public AssessmentSession getAuthorizedSession(UUID sessionId) {
        return authorizeSession(sessionId).session();
    }

    @Transactional(readOnly = true)
    public AssessmentItemResponse getAuthorizedResponse(UUID answerId) {
        return authorizeResponse(answerId).response();
    }

    /**
     * Like {@link #authorizeSession}, but the owner only gets through once the assessment has a conclusion.
     * Used for endpoints that return grading detail — the results summary screen hides fields
     * instead of blocking, because the page must still open so the student can see where their assessment stands.
     */
    @Transactional(readOnly = true)
    public SessionAccess requireCandidateVisibleSession(UUID sessionId) {
        SessionAccess access = authorizeSession(sessionId);
        requireVisibleToCaller(access);
        return access;
    }

    @Transactional(readOnly = true)
    public ResponseAccess requireCandidateVisibleResponse(UUID answerId) {
        ResponseAccess access = authorizeResponse(answerId);
        requireVisibleToCaller(access.sessionAccess());
        return access;
    }

    private void requireVisibleToCaller(SessionAccess access) {
        if (!access.candidateOwner()) {
            // Teacher / admin: the "who" permission is enough, there is no "when" rule — they need to see
            // unpublished scores to have a basis for grading.
            return;
        }
        AssessmentCandidateResultStatus status = assessmentCandidateResultRepository.findBySessionId(access.session().getId())
            .map(result -> result.getStatus())
            .orElse(null);
        if (!AssessmentCandidateResultStatus.isVisibleToCandidate(status)) {
            throw new ForbiddenException("The assessment result has not been published");
        }
    }

    /** @return true when the caller is the candidate of the assessment -- every other branch returns false. */
    private boolean authorize(UUID assessmentId, UUID candidateId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (userContextPort.isSystemAdmin()) {
            return false;
        }

        AssessmentCandidate candidate = assessmentCandidateRepository.findById(candidateId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidate of the assessment session not found"));
        if (candidate.getStudentId().equals(currentUserId)) {
            return true;
        }

        Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        if (isSchoolAdminOfAssessmentSchool(currentUserId, assessment) || isMemberWithAccess(assessmentId, currentUserId)) {
            return false;
        }

        throw new ForbiddenException("Access denied");
    }

    private boolean isSchoolAdminOfAssessmentSchool(UUID currentUserId, Assessment assessment) {
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId());
    }

    private boolean isMemberWithAccess(UUID assessmentId, UUID currentUserId) {
        return assessmentMemberRepository.findByAssessmentIdAndUserId(assessmentId, currentUserId)
            .map(member -> member.getRole() == AssessmentMemberRole.CHAIR
                || member.getRole() == AssessmentMemberRole.AUTHOR
                || member.getRole() == AssessmentMemberRole.REVIEWER)
            .orElse(false);
    }
}
