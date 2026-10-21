package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSectionItemCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.assessment.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;

@Service
public class CreateAssessmentBlueprintSectionUseCase implements IUseCase<CreateAssessmentBlueprintSectionItemCommand, AssessmentBlueprintSectionDto> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateAssessmentBlueprintSectionUseCase(
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentBlueprintSectionDto execute(CreateAssessmentBlueprintSectionItemCommand input) {
        var command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var version = assessmentBlueprintVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        var blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm section khi version đang DRAFT");
        }

        var siblings = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
        if (siblings.stream().anyMatch(s -> s.getOrder() == command.order())) {
            throw new IllegalStateException("Thứ tự section đã tồn tại trong version này");
        }

        var now = Instant.now();
        var section = new AssessmentBlueprintSection(
            version.getId(),
            command.order(),
            command.title(),
            command.instruction(),
            command.sectionTimeLimitSeconds(),
            command.sectionWeight() == null ? BigDecimal.ONE : command.sectionWeight(),
            now,
            now,
            currentUserId,
            currentUserId
        );
        return AssessmentBlueprintSectionDto.toDto(assessmentBlueprintSectionRepository.save(section));
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
