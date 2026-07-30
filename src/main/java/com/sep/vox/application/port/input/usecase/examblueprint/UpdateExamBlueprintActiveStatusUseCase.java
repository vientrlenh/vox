package com.sep.vox.application.port.input.usecase.examblueprint;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamBlueprintActiveStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.mapper.ExamBlueprintDtoMapper;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamBlueprintActiveStatusUseCase
        implements IUseCase<UpdateExamBlueprintActiveStatusCommand, ExamBlueprintDto> {

    private final ExamBlueprintRepository examBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public UpdateExamBlueprintActiveStatusUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamBlueprintDto execute(UpdateExamBlueprintActiveStatusCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var blueprint = examBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!examBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        blueprint.setActive(input.isActive());
        blueprint.setUpdatedAt(Instant.now());
        blueprint.setUpdatedBy(currentUserId);
        return ExamBlueprintDtoMapper.toDto(examBlueprintRepository.save(blueprint));
    }
}
