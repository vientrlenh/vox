package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentblueprint.DeleteAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class DeleteAssessmentBlueprintUseCase implements IUseCase<DeleteAssessmentBlueprintCommand, Void> {

    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteAssessmentBlueprintCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Access denied"));

        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment blueprint not found"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Access denied");
        }
        if (assessmentBlueprintRepository.existsUsedByAssessment(blueprint.getId())) {
            throw new IllegalArgumentException(
                "The blueprint is still referenced by at least 1 assessment and cannot be deleted: detach it from that assessment first");
        }

        if (blueprint.isActive()) {
            throw new IllegalArgumentException("A blueprint can only be deleted when isActive=false");
        }

        assessmentBlueprintRepository.deleteById(blueprint.getId());
        return null;
    }
}
