package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.math.BigDecimal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.service.AssessmentBlueprintEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintSectionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentBlueprintSectionUseCase implements IUseCase<UpdateAssessmentBlueprintSectionCommand, AssessmentBlueprintSectionDto> {

    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintEditAccessService assessmentBlueprintEditAccessService;

    @Override
    @Transactional
    public AssessmentBlueprintSectionDto execute(UpdateAssessmentBlueprintSectionCommand input) {
        UpdateAssessmentBlueprintSectionCommand command = normalize(input);
        AssessmentBlueprintEditAccessService.EditableSection editable =
            assessmentBlueprintEditAccessService.requireEditableSection(command.sectionId());
        AssessmentBlueprintSection section = editable.section();
        AssessmentBlueprintVersion version = editable.version();
        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được sửa section khi version đang DRAFT");
        }

        if (command.order() != section.getOrder()) {
            List<AssessmentBlueprintSection> siblings = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
            if (siblings.stream().anyMatch(s -> !s.getId().equals(section.getId()) && s.getOrder() == command.order())) {
                throw new IllegalArgumentException("Thứ tự section đã tồn tại trong version này");
            }
        }

        section.setOrder(command.order());
        section.setTitle(command.title());
        section.setInstruction(command.instruction());
        section.setSectionTimeLimitSeconds(command.sectionTimeLimitSeconds());
        section.setSectionWeight(command.sectionWeight() == null ? BigDecimal.ONE : command.sectionWeight());

        return AssessmentBlueprintSectionDtoMapper.toDto(assessmentBlueprintSectionRepository.save(section));
    }

    private UpdateAssessmentBlueprintSectionCommand normalize(UpdateAssessmentBlueprintSectionCommand input) {
        return new UpdateAssessmentBlueprintSectionCommand(
            input.sectionId(),
            input.order(),
            StringNormalization.trimAndCollapseSpaces(input.title()),
            StringNormalization.trimAndCollapseSpaces(input.instruction()),
            input.sectionTimeLimitSeconds(),
            input.sectionWeight()
        );
    }
}
