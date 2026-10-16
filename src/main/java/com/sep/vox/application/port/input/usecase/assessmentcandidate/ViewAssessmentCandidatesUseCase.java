package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewAssessmentCandidatesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;

@Service
public class ViewAssessmentCandidatesUseCase implements IUseCase<ViewAssessmentCandidatesQuery, List<AssessmentCandidateDto>> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ViewAssessmentCandidatesUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentCandidateDto> execute(ViewAssessmentCandidatesQuery input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        authorize(assessment, input.scheduleId());

        var candidates = assessmentCandidateRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(candidate -> input.scheduleId() == null
                || input.scheduleId().equals(candidate.getScheduleId()))
            .filter(candidate -> input.status() == null
                || input.status().equals(candidate.getStatus()))
            .toList();
        return AssessmentCandidateDto.toDtoList(candidates);
    }

    // Nhà trường (SCHOOL_ADMIN) và CHAIR của kỳ thi xem được toàn bộ candidate. Giáo viên
    // KHÔNG phải CHAIR chỉ xem được khi đang là giám thị (proctor) của đúng 1 ca thi cụ thể --
    // bắt buộc phải truyền scheduleId, không cho xem toàn bộ candidate của cả kỳ thi qua
    // đường này (tránh giám thị 1 ca nhìn thấy dữ liệu của ca khác).
    private void authorize(Assessment assessment, java.util.UUID scheduleId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return;
        }
        if (scheduleId != null && assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
