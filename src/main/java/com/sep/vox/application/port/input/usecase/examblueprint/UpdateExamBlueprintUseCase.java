package com.sep.vox.application.port.input.usecase.examblueprint;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.mapper.ExamBlueprintDtoMapper;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamBlueprintUseCase implements IUseCase<UpdateExamBlueprintCommand, ExamBlueprintDto> {

    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public UpdateExamBlueprintUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
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
        authorizeAuthor(currentUserId, currentSchoolId, blueprint.getSchoolId());

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

    private void authorizeAuthor(java.util.UUID currentUserId, java.util.UUID currentSchoolId, java.util.UUID schoolId) {
        if (!schoolId.equals(currentSchoolId)
                || !examMemberRepository.existsByUserIdAndRoleAndSchoolId(currentUserId, ExamMemberRole.AUTHOR, schoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }
}
