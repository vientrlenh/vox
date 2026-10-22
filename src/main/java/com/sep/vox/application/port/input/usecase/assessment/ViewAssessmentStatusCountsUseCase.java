package com.sep.vox.application.port.input.usecase.assessment;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewAssessmentStatusCountsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.dto.AssessmentStatusCountsDto;
import com.sep.vox.application.projection.repository.AssessmentStatusCountsQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewAssessmentStatusCountsUseCase implements IUseCase<ViewAssessmentStatusCountsQuery, AssessmentStatusCountsDto> {

    private final AssessmentStatusCountsQueryRepository assessmentStatusCountsQueryRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public ViewAssessmentStatusCountsUseCase(
            AssessmentStatusCountsQueryRepository assessmentStatusCountsQueryRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.assessmentStatusCountsQueryRepository = assessmentStatusCountsQueryRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentStatusCountsDto execute(ViewAssessmentStatusCountsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var systemAdmin = userContextPort.isSystemAdmin();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !systemAdmin && userContextPort.isSchoolAdmin();

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
