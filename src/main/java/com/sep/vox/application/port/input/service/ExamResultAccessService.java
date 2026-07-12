package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateRepository;
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
    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ExamResultAccessService(
            ExamSessionRepository examSessionRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examSessionRepository = examSessionRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Transactional(readOnly = true)
    public ExamSession getAuthorizedSession(UUID sessionId) {
        var session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        authorize(session.getExamId(), session.getCandidateId());
        return session;
    }

    @Transactional(readOnly = true)
    public ExamItemResponse getAuthorizedResponse(UUID answerId) {
        var response = examItemResponseRepository.findById(answerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu trả lời của thí sinh"));
        getAuthorizedSession(response.getSessionId());
        return response;
    }

    private void authorize(UUID examId, UUID candidateId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (userContextPort.isSystemAdmin()) {
            return;
        }

        var candidate = examCandidateRepository.findById(candidateId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        if (candidate.getStudentId().equals(currentUserId)) {
            return;
        }

        var exam = examRepository.findById(examId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return;
        }
        var isExamMemberWithAccess = examMemberRepository.findByExamIdAndUserId(examId, currentUserId)
            .map(member -> member.getRole() == ExamMemberRole.CHAIR
                || member.getRole() == ExamMemberRole.AUTHOR
                || member.getRole() == ExamMemberRole.REVIEWER)
            .orElse(false);
        if (isExamMemberWithAccess) {
            return;
        }

        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
