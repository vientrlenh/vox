package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentblueprint.DeleteAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.service.AssessmentBlueprintEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;

@Service
@RequiredArgsConstructor
public class DeleteAssessmentBlueprintSectionUseCase implements IUseCase<DeleteAssessmentBlueprintSectionCommand, Void> {

    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintEditAccessService assessmentBlueprintEditAccessService;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;

    @Override
    @Transactional
    public Void execute(DeleteAssessmentBlueprintSectionCommand input) {
        AssessmentBlueprintEditAccessService.EditableSection editable =
            assessmentBlueprintEditAccessService.requireEditableSection(input.sectionId());
        AssessmentBlueprintSection section = editable.section();
        AssessmentBlueprintVersion version = editable.version();
        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được xoá section khi version đang DRAFT");
        }

        for (AssessmentBlueprintSlot slot : assessmentBlueprintSlotRepository.findBySectionId(section.getId())) {
            assessmentBlueprintSlotRepository.deleteById(slot.getId());
        }
        assessmentBlueprintSectionRepository.deleteById(section.getId());
        recalculateBlueprintVersionTimeLimitService.recalculate(version.getId());
        return null;
    }
}
