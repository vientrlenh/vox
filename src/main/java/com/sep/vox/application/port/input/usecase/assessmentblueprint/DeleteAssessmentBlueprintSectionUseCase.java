package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class DeleteAssessmentBlueprintSectionUseCase implements IUseCase<DeleteAssessmentBlueprintSectionCommand, Void> {

    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteAssessmentBlueprintSectionCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        AssessmentBlueprintSection section = assessmentBlueprintSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section"));
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(section.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được xoá section khi version đang DRAFT");
        }

        for (AssessmentBlueprintSlot slot : assessmentBlueprintSlotRepository.findBySectionId(section.getId())) {
            assessmentBlueprintSlotRepository.deleteById(slot.getId());
        }
        assessmentBlueprintSectionRepository.deleteById(section.getId());
        recalculateBlueprintVersionTimeLimitService.recalculate(version.getId());
        return null;
    }
}
