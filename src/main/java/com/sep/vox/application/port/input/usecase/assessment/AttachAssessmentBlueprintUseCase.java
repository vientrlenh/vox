package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.AttachAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.command.assessment.CreateBlueprintInlineCommand;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;

@Service
@RequiredArgsConstructor
public class AttachAssessmentBlueprintUseCase implements IUseCase<AttachAssessmentBlueprintCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentDto execute(AttachAssessmentBlueprintCommand input) {
        requireValidBlueprintSelection(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        if (assessment.getKind() != AssessmentKind.CENTRALIZED) {
            throw new ForbiddenException("Only applies to a centralized assessment");
        }

        applyBlueprintSelection(input, assessment, currentUserId);

        return AssessmentDtoMapper.toDto(assessmentRepository.save(assessment));
    }

    private void applyBlueprintSelection(AttachAssessmentBlueprintCommand input, Assessment assessment, UUID currentUserId) {
        // Attaching or changing the blueprint: school admin, the question author, or the council chair
        // (the chair runs the whole process, so the chair must be able to choose the blueprint).
        // Pinning the version stays limited to the school admin and the chair.
        boolean canAttach = assessmentMemberRepository.canAttachBlueprint(assessment.getId(), currentUserId);
        boolean isChair = assessmentMemberRepository.canApproveBlueprintVersion(assessment.getId(), currentUserId);

        if (input.blueprintId() != null) {
            attachExistingBlueprint(input, assessment, canAttach);
        }
        if (input.newBlueprint() != null) {
            attachNewBlueprint(input, assessment, canAttach);
        }
        if (input.blueprintVersionId() != null) {
            pinBlueprintVersion(input, assessment, isChair);
        }
    }

    private void requireValidBlueprintSelection(AttachAssessmentBlueprintCommand input) {
        if (input.blueprintId() == null && input.blueprintVersionId() == null && input.newBlueprint() == null) {
            throw new IllegalArgumentException("blueprintId, blueprintVersionId or newBlueprint must be provided");
        }
        if (input.blueprintId() != null && input.newBlueprint() != null) {
            throw new IllegalArgumentException("Only one of blueprintId or newBlueprint may be provided, not both");
        }
    }

    private void attachExistingBlueprint(AttachAssessmentBlueprintCommand input, Assessment assessment, boolean canAttach) {
        if (!canAttach) {
            throw new ForbiddenException("Access denied");
        }

        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment blueprint not found"));
        if (!assessment.getSchoolId().equals(blueprint.getSchoolId())) {
            throw new IllegalArgumentException("The blueprint does not belong to the same school as the assessment");
        }

        if (!input.blueprintId().equals(assessment.getBlueprintId())) {
            requireNoExistingPapers(assessment.getId());
            assessment.setBlueprintVersionId(null);
        }
        assessment.setBlueprintId(input.blueprintId());
    }

    private void attachNewBlueprint(AttachAssessmentBlueprintCommand input, Assessment assessment, boolean canAttach) {
        if (!canAttach) {
            throw new ForbiddenException("Access denied");
        }
        requireNoExistingPapers(assessment.getId());
        AssessmentBlueprint createdBlueprint = createInlineBlueprint(input.newBlueprint(), assessment.getSchoolId(), assessment.getLanguageId());
        assessment.setBlueprintId(createdBlueprint.getId());
        assessment.setBlueprintVersionId(null);
    }

    private void pinBlueprintVersion(AttachAssessmentBlueprintCommand input, Assessment assessment, boolean isChair) {
        if (!isChair) {
            throw new ForbiddenException("Only the CHAIR may lock the blueprint version");
        }
        if (assessment.getBlueprintId() == null) {
            throw new IllegalArgumentException("The assessment has no blueprint attached");
        }

        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(input.blueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Blueprint version not found"));
        requirePublishedVersionOfAttachedBlueprint(version, assessment);
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            version.getTotalTimeLimitSeconds(),
            "Blueprint version " + version.getCode()
        );

        if (!input.blueprintVersionId().equals(assessment.getBlueprintVersionId())) {
            requireNoExistingPapers(assessment.getId());
        }
        assessment.setBlueprintVersionId(input.blueprintVersionId());
    }

    private void requirePublishedVersionOfAttachedBlueprint(AssessmentBlueprintVersion version, Assessment assessment) {
        if (!version.getBlueprintId().equals(assessment.getBlueprintId())) {
            throw new IllegalArgumentException("The version does not belong to the blueprint attached to the assessment");
        }
        if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only a PUBLISHED version may be locked");
        }
    }

    private void requireNoExistingPapers(UUID assessmentId) {
        if (assessmentPaperRepository.existsByAssessmentId(assessmentId)) {
            throw new IllegalArgumentException(
                "The assessment already has papers: delete every existing paper before changing the blueprint or locking another version");
        }
    }

    private AssessmentBlueprint createInlineBlueprint(
            CreateBlueprintInlineCommand command,
            UUID schoolId,
            UUID assessmentLanguageId) {
        String name = StringNormalization.trimAndCollapseSpaces(command.name());
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The blueprint name is required");
        }
        // Grade level is a shared catalog: it only has to exist, it no longer belongs to a school.
        if (command.gradeLevelId() != null
                && gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new ResourceNotFoundException("Grade level not found");
        }
        AssessmentBlueprint blueprint = AssessmentBlueprint.create(
            schoolId,
            command.languageId() == null ? assessmentLanguageId : command.languageId(),
            command.gradeLevelId(),
            blueprintCodeOf(command.code()),
            name,
            StringNormalization.trimAndCollapseSpaces(command.description()));
        return assessmentBlueprintRepository.save(blueprint);
    }

    private String blueprintCodeOf(String code) {
        String normalized = StringNormalization.normalizeCode(code);
        if (normalized != null && !normalized.isBlank()) {
            return normalized;
        }
        return "BP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
