package com.sep.vox.application.port.input.usecase.examblueprint;

import java.util.UUID;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamBlueprintDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.mapper.ExamBlueprintDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewExamBlueprintDetailsUseCase implements IUseCase<ViewExamBlueprintDetailsQuery, ExamBlueprintDto> {


    private final ExamBlueprintRepository examBlueprintRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public ViewExamBlueprintDetailsUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    public ExamBlueprintDto execute(ViewExamBlueprintDetailsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var blueprint = examBlueprintRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi với id: " + input.id()));
        if (!hasAccess(blueprint, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối: không có quyền truy cập blueprint đề thi này");
        }
        return ExamBlueprintDtoMapper.toDto(blueprint);
    }

    private boolean hasAccess(ExamBlueprint blueprint, UUID currentSchoolId) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }
        return currentSchoolId != null && blueprint.getSchoolId().equals(currentSchoolId);
    }
}
