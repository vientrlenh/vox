package com.sep.vox.application.port.input.usecase.assessment;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewAssessmentDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
public class ViewAssessmentDetailsUseCase implements IUseCase<ViewAssessmentDetailsQuery, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ViewAssessmentDetailsUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentDto execute(ViewAssessmentDetailsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !userContextPort.isSystemAdmin()
            && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var assessment = assessmentRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        if (!hasAccess(assessment.getId(), assessment.getSchoolId(), assessment.getStatus(), currentUserId, currentSchoolId, schoolAdmin)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        return AssessmentDtoMapper.toDto(assessment);
    }

    private boolean hasAccess(
            UUID assessmentId,
            UUID assessmentSchoolId,
            AssessmentStatus assessmentStatus,
            UUID currentUserId,
            UUID currentSchoolId,
            boolean schoolAdmin) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
            return true;
        }
        // Sau khi kỳ thi đã đóng, ai cùng trường cũng xem được — không cần là member nữa.
        if ((assessmentStatus == AssessmentStatus.CLOSED || assessmentStatus == AssessmentStatus.RESULTS_PUBLISHED)
                && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
            return true;
        }
        // Giám thị được phân công ca thi CỐ Ý không có mặt ở đây, dù họ có quan hệ thật với kỳ thi.
        //
        // Đây là cửa vào màn quản lý kỳ thi, nên mở nó cho giám thị là đưa họ thẳng vào dashboard --
        // chỗ của hội đồng và nhà trường. Nhu cầu "giám thị đọc được tên kỳ thi mình gác" có đường
        // riêng: ViewMonitorableAssessmentUseCase, trả đúng vài trường mà đầu trang giám sát cần.
        return assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)
            || assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.AUTHOR)
            || assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.REVIEWER);
    }
}
