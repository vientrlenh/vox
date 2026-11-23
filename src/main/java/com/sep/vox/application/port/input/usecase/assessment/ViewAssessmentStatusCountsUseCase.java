package com.sep.vox.application.port.input.usecase.assessment;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewAssessmentStatusCountsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.AssessmentStatusCountsDto;
import com.sep.vox.application.query.repository.AssessmentStatusCountsQueryRepository;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentStatusCountsUseCase implements IUseCase<ViewAssessmentStatusCountsQuery, AssessmentStatusCountsDto> {

    private final AssessmentStatusCountsQueryRepository assessmentStatusCountsQueryRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public AssessmentStatusCountsDto execute(ViewAssessmentStatusCountsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        boolean systemAdmin = userContextPort.isSystemAdmin();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        return assessmentStatusCountsQueryRepository.countAccessibleByStatus(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.schoolId(),
            input.kind() == null ? null : input.kind().name(),
            input.createdFrom(),
            input.createdTo()
        );
    }
}
