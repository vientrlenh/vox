package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewAssessmentDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentDetailsUseCase implements IUseCase<ViewAssessmentDetailsQuery, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public AssessmentDto execute(ViewAssessmentDetailsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = !userContextPort.isSystemAdmin()
            && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        Assessment assessment = assessmentRepository.findById(input.id())
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
        // A proctor assigned to a session is DELIBERATELY absent here, even though they have a real relationship with the assessment.
        //
        // This is the entry to the assessment management screen, so opening it to proctors would put them straight into the dashboard --
        // the place of the council and the school. The need "a proctor can read the name of the assessment they supervise" has its own
        // path: ViewMonitorableAssessmentUseCase, which returns exactly the few fields the top of the monitoring page needs.
        return assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)
            || assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.AUTHOR)
            || assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.REVIEWER);
    }
}
