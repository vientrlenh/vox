package com.sep.vox.application.port.input.usecase.assessment;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewAssessmentsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.assessment.AssessmentDto;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewAssessmentsUseCase implements IUseCase<ViewAssessmentsQuery, PageResult<AssessmentDto>> {

    private final AssessmentRepository assessmentRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public ViewAssessmentsUseCase(
            AssessmentRepository assessmentRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.assessmentRepository = assessmentRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentDto> execute(ViewAssessmentsQuery input) {
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
            input.pageRequest().repositoryPage(),
            input.pageRequest().size()
        );
        return AssessmentDto.toDtoPage(result);
    }
}
