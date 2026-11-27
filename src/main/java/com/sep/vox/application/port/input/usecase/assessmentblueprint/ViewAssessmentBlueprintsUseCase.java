package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.assessmentblueprint.ViewAssessmentBlueprintsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintDtoMapper;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentBlueprintsUseCase implements IUseCase<ViewAssessmentBlueprintsQuery, PageResult<AssessmentBlueprintDto>> {

    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentBlueprintDto> execute(ViewAssessmentBlueprintsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        boolean systemAdmin = userContextPort.isSystemAdmin();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Access denied");
        }

        PageResult<AssessmentBlueprint> result = findAccessible(input, currentUserId, currentSchoolId, systemAdmin, schoolAdmin);
        return AssessmentBlueprintDtoMapper.toDtoPage(result);
    }

    private PageResult<AssessmentBlueprint> findAccessible(
            ViewAssessmentBlueprintsQuery input,
            UUID currentUserId,
            UUID currentSchoolId,
            boolean systemAdmin,
            boolean schoolAdmin) {
        return assessmentBlueprintRepository.findAccessible(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.schoolId(),
            input.isActive(),
            input.languageId(),
            input.assessmentKind() == null ? null : input.assessmentKind().name(),
            input.keyword(),
            input.page(),
            input.size()
        );
    }
}
