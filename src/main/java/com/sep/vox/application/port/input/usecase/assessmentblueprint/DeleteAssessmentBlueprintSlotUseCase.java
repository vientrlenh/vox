package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentblueprint.DeleteAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class DeleteAssessmentBlueprintSlotUseCase implements IUseCase<DeleteAssessmentBlueprintSlotCommand, Void> {

    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteAssessmentBlueprintSlotCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Access denied"));

        AssessmentBlueprintSlot slot = assessmentBlueprintSlotRepository.findById(input.slotId())
            .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(slot.getBlueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Blueprint version not found"));
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment blueprint not found"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Access denied");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("A slot can only be deleted while the version is DRAFT");
        }

        assessmentBlueprintSlotRepository.deleteById(slot.getId());
        recalculateBlueprintVersionTimeLimitService.recalculate(version.getId());
        return null;
    }
}
