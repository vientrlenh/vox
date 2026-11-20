package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ExamResultAccessService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ExamResultAccessService(
            ExamSessionRepository examSessionRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examSessionRepository = examSessionRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * Phiên thi kèm câu trả lời "người gọi có phải chính thí sinh không". Luật hiển thị
     * theo trạng thái chỉ áp cho chính chủ, nên use case nào cần phân biệt thì dùng bản này
     * thay vì tự tra lại {@code ExamCandidate} một lần nữa.
     */
    public record SessionAccess(ExamSession session, boolean candidateOwner) {
    }

    public record ResponseAccess(ExamItemResponse response, SessionAccess sessionAccess) {
    }

    @Transactional(readOnly = true)
    public SessionAccess authorizeSession(UUID sessionId) {
        var session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        var candidateOwner = authorize(session.getExamId(), session.getCandidateId());
        return new SessionAccess(session, candidateOwner);
    }

    @Transactional(readOnly = true)
    public ResponseAccess authorizeResponse(UUID answerId) {
        var response = examItemResponseRepository.findById(answerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu trả lời của thí sinh"));
        return new ResponseAccess(response, authorizeSession(response.getSessionId()));
    }

    @Transactional(readOnly = true)
    public ExamSession getAuthorizedSession(UUID sessionId) {
        return authorizeSession(sessionId).session();
    }

    @Transactional(readOnly = true)
    public ExamItemResponse getAuthorizedResponse(UUID answerId) {
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
        var status = examCandidateResultRepository.findBySessionId(access.session().getId())
            .map(result -> result.getStatus())
            .orElse(null);
        if (!ExamCandidateResultStatus.isVisibleToCandidate(status)) {
            throw new ForbiddenException("Kết quả bài thi chưa được công bố");
        }
    }

    /** @return true khi người gọi chính là thí sinh của bài — mọi nhánh khác đều false. */
    private boolean authorize(UUID examId, UUID candidateId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (userContextPort.isSystemAdmin()) {
            return false;
        }

        var candidate = examCandidateRepository.findById(candidateId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        if (candidate.getStudentId().equals(currentUserId)) {
            return true;
        }

        var exam = examRepository.findById(examId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return false;
        }
        var isExamMemberWithAccess = examMemberRepository.findByExamIdAndUserId(examId, currentUserId)
            .map(member -> member.getRole() == ExamMemberRole.CHAIR
                || member.getRole() == ExamMemberRole.AUTHOR
                || member.getRole() == ExamMemberRole.REVIEWER)
            .orElse(false);
        if (isExamMemberWithAccess) {
            return false;
        }

        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
