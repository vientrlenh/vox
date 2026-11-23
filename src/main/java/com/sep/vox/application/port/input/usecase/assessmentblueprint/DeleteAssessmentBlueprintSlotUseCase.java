package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteAssessmentBlueprintSlotCommand;
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
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        AssessmentBlueprintSlot slot = assessmentBlueprintSlotRepository.findById(input.slotId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy slot"));
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(slot.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
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
