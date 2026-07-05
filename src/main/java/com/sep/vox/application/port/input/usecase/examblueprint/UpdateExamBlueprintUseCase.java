package com.sep.vox.application.port.input.usecase.examblueprint;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.mapper.ExamBlueprintDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamBlueprintUseCase implements IUseCase<UpdateExamBlueprintCommand, ExamBlueprintDto> {

    private final ExamBlueprintRepository examBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamBlueprintUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamBlueprintDto execute(UpdateExamBlueprintCommand input) {
        var command = new UpdateExamBlueprintCommand(
            input.blueprintId(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var blueprint = examBlueprintRepository.findById(command.blueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        authorizeOwner(blueprint, currentUserId, currentSchoolId);

        if (examBlueprintRepository.existsUsedByExam(blueprint.getId())) {
            throw new IllegalStateException("Không thể chỉnh sửa blueprint đã được sử dụng bởi bài kiểm tra");
        }

        if (command.name() != null) {
            blueprint.setName(command.name());
        }
        if (command.description() != null) {
            blueprint.setDescription(command.description());
        }
        blueprint.setUpdatedAt(OffsetDateTime.now());
        blueprint.setUpdatedBy(currentUserId);
        return ExamBlueprintDtoMapper.toDto(examBlueprintRepository.save(blueprint));
    }

    private void authorizeOwner(ExamBlueprint blueprint, UUID currentUserId, UUID currentSchoolId) {
        if (!blueprint.getSchoolId().equals(currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (!blueprint.getCreatedBy().equals(currentUserId) && !schoolAdmin) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }
}
