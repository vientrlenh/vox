package com.sep.vox.application.port.input.usecase.assessment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewAssessmentsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewAssessmentsUseCase implements IUseCase<ViewAssessmentsQuery, PageResult<AssessmentDto>> {

    private final AssessmentRepository assessmentRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ViewAssessmentsUseCase(
            AssessmentRepository assessmentRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.assessmentRepository = assessmentRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentDto> execute(ViewAssessmentsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var systemAdmin = userContextPort.isSystemAdmin();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var result = assessmentRepository.findAccessible(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.schoolId(),
            input.schoolClassId(),
            input.kind(),
            input.status(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return AssessmentDtoMapper.toDtoPage(result);
    }
}
