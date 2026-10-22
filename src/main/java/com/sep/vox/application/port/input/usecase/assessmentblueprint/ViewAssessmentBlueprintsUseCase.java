package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewAssessmentBlueprintsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.dto.assessment.AssessmentBlueprintDto;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewAssessmentBlueprintsUseCase implements IUseCase<ViewAssessmentBlueprintsQuery, PageResult<AssessmentBlueprintDto>> {

    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public ViewAssessmentBlueprintsUseCase(
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentBlueprintDto> execute(ViewAssessmentBlueprintsQuery input) {
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

        var result = assessmentBlueprintRepository.findAccessible(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.schoolId(),
            input.isActive(),
            input.languageId(),
            input.assessmentKind(),
            input.keyword(),
            input.pageRequest().repositoryPage(),
            input.pageRequest().size()
        );
        return AssessmentBlueprintDto.toDtoPage(result);
    }
}
