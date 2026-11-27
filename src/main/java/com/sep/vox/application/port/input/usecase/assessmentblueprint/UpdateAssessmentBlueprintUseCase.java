package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.service.AssessmentBlueprintEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentBlueprintUseCase implements IUseCase<UpdateAssessmentBlueprintCommand, AssessmentBlueprintDto> {

    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintEditAccessService assessmentBlueprintEditAccessService;
    private final AssessmentRepository assessmentRepository;

    @Override
    @Transactional
    public AssessmentBlueprintDto execute(UpdateAssessmentBlueprintCommand input) {
        UpdateAssessmentBlueprintCommand command = new UpdateAssessmentBlueprintCommand(
            input.blueprintId(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
        AssessmentBlueprint blueprint = assessmentBlueprintEditAccessService.requireEditableBlueprint(command.blueprintId());
        if (assessmentRepository.existsByBlueprintIdAndKindAndStatusNot(blueprint.getId(), AssessmentKind.CENTRALIZED, AssessmentStatus.DRAFT)) {
            throw new IllegalArgumentException(
                "The blueprint is used by a centralized assessment and is locked, so it cannot be edited or deleted directly: duplicate it as a new version");
        }

        if (command.name() != null) {
            blueprint.setName(command.name());
        }
        if (command.description() != null) {
            blueprint.setDescription(command.description());
        }
        return AssessmentBlueprintDtoMapper.toDto(assessmentBlueprintRepository.save(blueprint));
    }
}
