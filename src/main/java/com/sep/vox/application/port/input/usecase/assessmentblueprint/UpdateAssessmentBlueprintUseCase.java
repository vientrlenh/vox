package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateAssessmentBlueprintUseCase implements IUseCase<UpdateAssessmentBlueprintCommand, AssessmentBlueprintDto> {

    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentRepository assessmentRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public UpdateAssessmentBlueprintUseCase(
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            AssessmentRepository assessmentRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.assessmentRepository = assessmentRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentBlueprintDto execute(UpdateAssessmentBlueprintCommand input) {
        UpdateAssessmentBlueprintCommand command = new UpdateAssessmentBlueprintCommand(
            input.blueprintId(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(command.blueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (assessmentRepository.existsByBlueprintIdAndKindAndStatusNot(blueprint.getId(), AssessmentKind.CENTRALIZED, AssessmentStatus.DRAFT)) {
            throw new IllegalStateException(
                "Blueprint đã được dùng cho kỳ thi tập trung và đã khóa, không thể sửa/xóa trực tiếp — hãy nhân bản phiên bản mới");
        }

        if (command.name() != null) {
            blueprint.setName(command.name());
        }
        if (command.description() != null) {
            blueprint.setDescription(command.description());
        }
        return AssessmentBlueprintDtoMapper.toDto(assessmentBlueprintRepository.save(blueprint));
    }
}
