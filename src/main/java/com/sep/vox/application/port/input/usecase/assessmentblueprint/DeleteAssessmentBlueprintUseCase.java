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
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (assessmentBlueprintRepository.existsUsedByAssessment(blueprint.getId())) {
            throw new IllegalArgumentException(
                "Blueprint vẫn đang được ít nhất 1 bài kiểm tra tham chiếu, không thể xóa — hãy gỡ blueprint khỏi bài kiểm tra đó trước");
        }

        if (blueprint.isActive()) {
            throw new IllegalArgumentException("Chỉ được xóa blueprint khi isActive=false");
        }

        assessmentBlueprintRepository.deleteById(blueprint.getId());
        return null;
    }
}
