package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ExamSessionModerationAccessService {

    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;

    public ExamSessionModerationAccessService(
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            ExamMemberRepository examMemberRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository) {
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.examMemberRepository = examMemberRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
    }

    public UUID getCurrentUserId() {
        return userContextPort.getCurrentAuthenticatedUserId();
    }

    public void authorize(Exam exam, ExamCandidate candidate) {
        var currentUserId = getCurrentUserId();
        if (userContextPort.isSystemAdmin()) {
            return;
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return;
        }

        var allowedExamRole = examMemberRepository.findByExamIdAndUserId(exam.getId(), currentUserId)
            .map(member -> member.getRole() == ExamMemberRole.CHAIR
                || member.getRole() == ExamMemberRole.AUTHOR
                || member.getRole() == ExamMemberRole.REVIEWER)
            .orElse(false);
        if (allowedExamRole) {
            return;
        }

        if (candidate.getScheduleId() != null
                && examScheduleProctorRepository.existsByScheduleIdAndTeacherId(candidate.getScheduleId(), currentUserId)) {
            return;
        }

        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
