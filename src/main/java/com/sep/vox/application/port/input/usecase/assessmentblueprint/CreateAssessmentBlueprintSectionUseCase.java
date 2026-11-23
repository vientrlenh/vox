package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSectionItemCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintSectionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class CreateAssessmentBlueprintSectionUseCase implements IUseCase<CreateAssessmentBlueprintSectionItemCommand, AssessmentBlueprintSectionDto> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentBlueprintSectionDto execute(CreateAssessmentBlueprintSectionItemCommand input) {
        CreateAssessmentBlueprintSectionItemCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được thêm section khi version đang DRAFT");
        }

        List<AssessmentBlueprintSection> siblings = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
        if (siblings.stream().anyMatch(s -> s.getOrder() == command.order())) {
            throw new IllegalArgumentException("Thứ tự section đã tồn tại trong version này");
        }

        AssessmentBlueprintSection section = AssessmentBlueprintSection.builder()
            .blueprintVersionId(version.getId())
            .order(command.order())
            .title(command.title())
            .instruction(command.instruction())
            .sectionTimeLimitSeconds(command.sectionTimeLimitSeconds())
            .sectionWeight(command.sectionWeight() == null ? BigDecimal.ONE : command.sectionWeight())
            .build();
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
