package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
public class AssessmentDirectoryAccessService {

    /** Trần số lớp gom được khi giải phạm vi "lớp của tôi" — khớp các use case nhập thí sinh. */
    private static final int MAX_CLASSES = 200;

    private final AssessmentRepository assessmentRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public AssessmentDirectoryAccessService(
            AssessmentRepository assessmentRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolClassRepository schoolClassRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * @param schoolWide true = toàn bộ trường của kỳ thi; false = chỉ những lớp mà
     *                   {@code callerId} là thành viên.
     */
    public record AssessmentDirectoryScope(UUID callerId, UUID schoolId, boolean schoolWide) {
    }

    public Assessment requireAssessment(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
    }

    public AssessmentDirectoryScope resolveByAssessmentId(UUID assessmentId) {
        return resolve(requireAssessment(assessmentId));
    }

    public AssessmentDirectoryScope resolve(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return new AssessmentDirectoryScope(currentUserId, assessment.getSchoolId(), true);
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return new AssessmentDirectoryScope(
                currentUserId, assessment.getSchoolId(), assessment.getKind() != AssessmentKind.CLASS_TEST);
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    /**
     * Các lớp đang hoạt động mà người gọi là thành viên, trong trường của kỳ thi.
     * Chỉ có nghĩa khi {@code scope.schoolWide()} là false.
     */
    public List<UUID> callerClassIds(AssessmentDirectoryScope scope) {
        // findByUserId là 1-based (PageRequest.of(page - 1, size)) → trang đầu là 1, KHÔNG phải 0.
        return schoolClassRepository
            .findByUserId(scope.schoolId(), scope.callerId(), null, SchoolClassStatus.ACTIVE, 1, MAX_CLASSES)
            .content()
            .stream()
            .map(schoolClass -> schoolClass.getId())
            .toList();
    }
}
