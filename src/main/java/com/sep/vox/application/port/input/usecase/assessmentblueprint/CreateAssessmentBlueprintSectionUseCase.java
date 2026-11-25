package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.math.BigDecimal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSectionItemCommand;
import com.sep.vox.application.port.input.service.AssessmentBlueprintEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintSectionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;

@Service
@RequiredArgsConstructor
public class CreateAssessmentBlueprintSectionUseCase implements IUseCase<CreateAssessmentBlueprintSectionItemCommand, AssessmentBlueprintSectionDto> {

    private final AssessmentBlueprintEditAccessService assessmentBlueprintEditAccessService;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;

    @Override
    @Transactional
    public AssessmentBlueprintSectionDto execute(CreateAssessmentBlueprintSectionItemCommand input) {
        CreateAssessmentBlueprintSectionItemCommand command = normalize(input);
        AssessmentBlueprintVersion version =
            assessmentBlueprintEditAccessService.requireEditableVersion(command.versionId()).version();
        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được thêm section khi version đang DRAFT");
        }

        List<AssessmentBlueprintSection> siblings = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
        if (siblings.stream().anyMatch(s -> s.getOrder() == command.order())) {
            throw new IllegalArgumentException("Thứ tự section đã tồn tại trong version này");
        }

        AssessmentBlueprintSection section = AssessmentBlueprintSection.create(
            version.getId(),
            command.order(),
            command.title(),
            command.instruction(),
            command.sectionTimeLimitSeconds(),
            command.sectionWeight() == null ? BigDecimal.ONE : command.sectionWeight());
        return AssessmentBlueprintSectionDtoMapper.toDto(assessmentBlueprintSectionRepository.save(section));
    }

    private CreateAssessmentBlueprintSectionItemCommand normalize(CreateAssessmentBlueprintSectionItemCommand input) {
        return new CreateAssessmentBlueprintSectionItemCommand(
            input.versionId(),
            input.order(),
            StringNormalization.trimAndCollapseSpaces(input.title()),
            StringNormalization.trimAndCollapseSpaces(input.instruction()),
            input.sectionTimeLimitSeconds(),
            input.sectionWeight()
        );
    }
}
