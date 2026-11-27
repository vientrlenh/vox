package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentblueprint.DeleteAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.service.AssessmentBlueprintEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;

@Service
@RequiredArgsConstructor
public class DeleteAssessmentBlueprintVersionUseCase implements IUseCase<DeleteAssessmentBlueprintVersionCommand, Void> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintEditAccessService assessmentBlueprintEditAccessService;

    @Override
    @Transactional
    public Void execute(DeleteAssessmentBlueprintVersionCommand input) {
        AssessmentBlueprintVersion version =
            assessmentBlueprintEditAccessService.requireEditableVersion(input.versionId()).version();
        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được xóa phiên bản khi đang ở trạng thái Bản nháp");
        }
        if (assessmentBlueprintVersionRepository.existsUsedByVersion(version.getId())) {
            throw new IllegalArgumentException(
                "Phiên bản vẫn đang được ít nhất 1 bài kiểm tra hoặc mã đề tham chiếu, không thể xóa");
        }

        deleteSectionsWithSlots(version);
        assessmentBlueprintVersionRepository.deleteById(version.getId());
        return null;
    }

    private void deleteSectionsWithSlots(AssessmentBlueprintVersion version) {
        for (AssessmentBlueprintSection section : assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId())) {
            for (AssessmentBlueprintSlot slot : assessmentBlueprintSlotRepository.findBySectionId(section.getId())) {
                assessmentBlueprintSlotRepository.deleteById(slot.getId());
            }
            assessmentBlueprintSectionRepository.deleteById(section.getId());
        }
    }
}
