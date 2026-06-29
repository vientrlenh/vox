package com.sep.vox.application.port.input.usecase.examblueprint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewExamBlueprintsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.mapper.ExamBlueprintDtoMapper;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewExamBlueprintsUseCase implements IUseCase<ViewExamBlueprintsQuery, PageResult<ExamBlueprintDto>> {

    private final ExamBlueprintRepository examBlueprintRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ViewExamBlueprintsUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamBlueprintDto> execute(ViewExamBlueprintsQuery input) {
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

        var result = examBlueprintRepository.findAccessible(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.schoolId(),
            input.isActive(),
            input.languageId(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return ExamBlueprintDtoMapper.toDtoPage(result);
    }
}
