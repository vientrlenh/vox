package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class CreateAssessmentBlueprintUseCase implements IUseCase<CreateAssessmentBlueprintCommand, AssessmentBlueprintDto> {

    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentBlueprintDto execute(CreateAssessmentBlueprintCommand input) {
        CreateAssessmentBlueprintCommand command = new CreateAssessmentBlueprintCommand(
            input.languageId(),
            input.gradeLevelId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        // Grade level is a shared catalog: it only has to exist, it no longer belongs to a school.
        if (command.gradeLevelId() != null
                && gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new NotFoundException("Không tìm thấy khối lớp");
        }

        AssessmentBlueprint blueprint = AssessmentBlueprint.builder()
            .schoolId(currentSchoolId)
            .languageId(command.languageId())
            .gradeLevelId(command.gradeLevelId())
            .code(blueprintCodeOf(command.code()))
            .name(command.name())
            .description(command.description())
            .active(true)
            .build();
        return AssessmentBlueprintDtoMapper.toDto(assessmentBlueprintRepository.save(blueprint));
    }

    private String blueprintCodeOf(String code) {
        if (code != null && !code.isBlank()) {
            return code;
        }
        return "BP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
