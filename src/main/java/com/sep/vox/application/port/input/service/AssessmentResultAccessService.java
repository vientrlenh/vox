package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentItemAnswer;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemAnswerRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Service
public class AssessmentResultAccessService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentItemAnswerRepository assessmentItemAnswerRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public AssessmentResultAccessService(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentItemAnswerRepository assessmentItemAnswerRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentItemAnswerRepository = assessmentItemAnswerRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * Phiên thi kèm câu trả lời "người gọi có phải chính thí sinh không". Luật hiển thị
     * theo trạng thái chỉ áp cho chính chủ, nên use case nào cần phân biệt thì dùng bản này
     * thay vì tự tra lại {@code AssessmentCandidate} một lần nữa.
     */
    public record SessionAccess(AssessmentSession session, boolean candidateOwner) {
    }

    public record ResponseAccess(AssessmentItemAnswer response, SessionAccess sessionAccess) {
    }

    @Transactional(readOnly = true)
    public SessionAccess authorizeSession(UUID sessionId) {
        var session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
        var candidateOwner = authorize(session.getAssessmentId(), session.getCandidateId());
        return new SessionAccess(session, candidateOwner);
    }

    @Transactional(readOnly = true)
    public ResponseAccess authorizeResponse(UUID answerId) {
        var response = assessmentItemAnswerRepository.findById(answerId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu trả lời của thí sinh"));
        return new ResponseAccess(response, authorizeSession(response.getSessionId()));
    }

    @Transactional(readOnly = true)
    public AssessmentSession getAuthorizedSession(UUID sessionId) {
        return authorizeSession(sessionId).session();
    }

    @Transactional(readOnly = true)
    public AssessmentItemAnswer getAuthorizedResponse(UUID answerId) {
        return authorizeResponse(answerId).response();
    }

    /**
     * Như {@link #authorizeSession}, nhưng chính chủ chỉ qua được khi bài đã có kết luận.
     * Dùng cho các endpoint trả chi tiết chấm điểm — màn tổng kết quả thì che field chứ
     * không chặn, vì trang vẫn phải mở được để học sinh thấy bài mình đang ở đâu.
     */
    @Transactional(readOnly = true)
    public SessionAccess requireCandidateVisibleSession(UUID sessionId) {
        var access = authorizeSession(sessionId);
        requireVisibleToCaller(access);
        return access;
    }

    @Transactional(readOnly = true)
    public ResponseAccess requireCandidateVisibleResponse(UUID answerId) {
        var access = authorizeResponse(answerId);
        requireVisibleToCaller(access.sessionAccess());
        return access;
    }

    private void requireVisibleToCaller(SessionAccess access) {
        if (!access.candidateOwner()) {
            // Giáo viên / admin: quyền "ai" đã đủ, không có luật "khi nào" — họ cần thấy
            // điểm chưa công bố để có căn cứ mà chấm.
            return;
        }
        var status = assessmentCandidateResultRepository.findBySessionId(access.session().getId())
            .map(result -> result.getStatus())
            .orElse(null);
        if (!AssessmentCandidateResultStatus.isVisibleToCandidate(status)) {
            throw new ForbiddenException("Kết quả bài thi chưa được công bố");
        }
    }

    /** @return true khi người gọi chính là thí sinh của bài — mọi nhánh khác đều false. */
    private boolean authorize(UUID assessmentId, UUID candidateId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (userContextPort.isSystemAdmin()) {
            return false;
        }

        var candidate = assessmentCandidateRepository.findById(candidateId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của phiên thi"));
        if (candidate.getStudentId().equals(currentUserId)) {
            return true;
        }

        var assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return false;
        }
        var isAssessmentMemberWithAccess = assessmentMemberRepository.findByAssessmentIdAndUserId(assessmentId, currentUserId)
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
