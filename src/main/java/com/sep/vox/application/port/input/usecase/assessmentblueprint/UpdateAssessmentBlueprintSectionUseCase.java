package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintSectionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateAssessmentBlueprintSectionUseCase implements IUseCase<UpdateAssessmentBlueprintSectionCommand, AssessmentBlueprintSectionDto> {

    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public UpdateAssessmentBlueprintSectionUseCase(
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentBlueprintSectionDto execute(UpdateAssessmentBlueprintSectionCommand input) {
        UpdateAssessmentBlueprintSectionCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        AssessmentBlueprintSection section = assessmentBlueprintSectionRepository.findById(command.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section"));
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(section.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được sửa section khi version đang DRAFT");
        }

        if (command.order() != section.getOrder()) {
            List<AssessmentBlueprintSection> siblings = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
            if (siblings.stream().anyMatch(s -> !s.getId().equals(section.getId()) && s.getOrder() == command.order())) {
                throw new IllegalStateException("Thứ tự section đã tồn tại trong version này");
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
