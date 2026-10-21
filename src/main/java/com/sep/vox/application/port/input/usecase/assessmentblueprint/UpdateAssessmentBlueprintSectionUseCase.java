package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.assessment.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;

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
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var section = assessmentBlueprintSectionRepository.findById(command.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy section"));
        var version = assessmentBlueprintVersionRepository.findById(section.getBlueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        var blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được sửa section khi version đang DRAFT");
        }

        if (command.order() != section.getOrder()) {
            var siblings = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
            if (siblings.stream().anyMatch(s -> !s.getId().equals(section.getId()) && s.getOrder() == command.order())) {
                throw new IllegalStateException("Thứ tự section đã tồn tại trong version này");
            }
        }

        section.setOrder(command.order());
        section.setTitle(command.title());
        section.setInstruction(command.instruction());
        section.setSectionTimeLimitSeconds(command.sectionTimeLimitSeconds());
        section.setSectionWeight(command.sectionWeight() == null ? BigDecimal.ONE : command.sectionWeight());
        section.setUpdatedAt(Instant.now());
        section.setUpdatedBy(currentUserId);

        return AssessmentBlueprintSectionDto.toDto(assessmentBlueprintSectionRepository.save(section));
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
