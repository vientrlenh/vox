package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;

@Service
public class DeleteAssessmentBlueprintSlotUseCase implements IUseCase<DeleteAssessmentBlueprintSlotCommand, Void> {

    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;
    private final UserContextPort userContextPort;

    public DeleteAssessmentBlueprintSlotUseCase(
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            SchoolUserRepository schoolUserRepository,
            RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService,
            UserContextPort userContextPort) {
        this.assessmentBlueprintSlotRepository = assessmentBlueprintSlotRepository;
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.recalculateBlueprintVersionTimeLimitService = recalculateBlueprintVersionTimeLimitService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteAssessmentBlueprintSlotCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var slot = assessmentBlueprintSlotRepository.findById(input.slotId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy slot"));
        var version = assessmentBlueprintVersionRepository.findById(slot.getBlueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        var blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được xoá slot khi version đang DRAFT");
        }

        assessmentBlueprintSlotRepository.deleteById(slot.getId());
        recalculateBlueprintVersionTimeLimitService.recalculate(version.getId());
        return null;
    }
}
