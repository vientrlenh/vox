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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
        boolean candidateOwner = authorize(session.getAssessmentId(), session.getCandidateId());
        return new SessionAccess(session, candidateOwner);
    }

    @Transactional(readOnly = true)
    public ResponseAccess authorizeResponse(UUID answerId) {
        AssessmentItemResponse response = assessmentItemResponseRepository.findById(answerId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu trả lời của thí sinh"));
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
     * Như {@link #authorizeSession}, nhưng chính chủ chỉ qua được khi bài đã có kết luận.
     * Dùng cho các endpoint trả chi tiết chấm điểm — màn tổng kết quả thì che field chứ
     * không chặn, vì trang vẫn phải mở được để học sinh thấy bài mình đang ở đâu.
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
            // Giáo viên / admin: quyền "ai" đã đủ, không có luật "khi nào" — họ cần thấy
            // điểm chưa công bố để có căn cứ mà chấm.
            return;
        }
        AssessmentCandidateResultStatus status = assessmentCandidateResultRepository.findBySessionId(access.session().getId())
            .map(result -> result.getStatus())
            .orElse(null);
        if (!AssessmentCandidateResultStatus.isVisibleToCandidate(status)) {
            throw new ForbiddenException("Kết quả bài thi chưa được công bố");
        }
    }

    /** @return true khi người gọi chính là thí sinh của bài — mọi nhánh khác đều false. */
    private boolean authorize(UUID assessmentId, UUID candidateId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (userContextPort.isSystemAdmin()) {
            return false;
        }

        AssessmentCandidate candidate = assessmentCandidateRepository.findById(candidateId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của phiên thi"));
        if (candidate.getStudentId().equals(currentUserId)) {
            return true;
        }

        Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return false;
        }
        Boolean isAssessmentMemberWithAccess = assessmentMemberRepository.findByAssessmentIdAndUserId(assessmentId, currentUserId)
            .map(member -> member.getRole() == AssessmentMemberRole.CHAIR
                || member.getRole() == AssessmentMemberRole.AUTHOR
                || member.getRole() == AssessmentMemberRole.REVIEWER)
            .orElse(false);
        if (isAssessmentMemberWithAccess) {
            return false;
        }

        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
