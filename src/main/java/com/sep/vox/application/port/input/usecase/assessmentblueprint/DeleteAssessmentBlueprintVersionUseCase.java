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
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentblueprint.DeleteAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class DeleteAssessmentBlueprintVersionUseCase implements IUseCase<DeleteAssessmentBlueprintVersionCommand, Void> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteAssessmentBlueprintVersionCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(input.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được xóa phiên bản khi đang ở trạng thái Bản nháp");
        }
        if (assessmentBlueprintVersionRepository.existsUsedByVersion(version.getId())) {
            throw new IllegalArgumentException(
                "Phiên bản vẫn đang được ít nhất 1 bài kiểm tra hoặc mã đề tham chiếu, không thể xóa");
        }

        for (AssessmentBlueprintSection section : assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId())) {
            for (AssessmentBlueprintSlot slot : assessmentBlueprintSlotRepository.findBySectionId(section.getId())) {
                assessmentBlueprintSlotRepository.deleteById(slot.getId());
            }
            assessmentBlueprintSectionRepository.deleteById(section.getId());
        }
        assessmentBlueprintVersionRepository.deleteById(version.getId());
        return null;
    }
}
