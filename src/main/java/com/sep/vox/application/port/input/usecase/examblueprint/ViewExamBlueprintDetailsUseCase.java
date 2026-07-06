package com.sep.vox.application.port.input.usecase.examblueprint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public ExamBlueprintDto execute(ViewExamBlueprintDetailsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var blueprint = examBlueprintRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y blueprint Ä‘á» thi"));
        if (!hasAccess(blueprint, currentSchoolId)) {
            throw new ForbiddenException("Quyá»n truy cáº­p bá»‹ tá»« chá»‘i");
        }
        return ExamBlueprintDtoMapper.toDto(blueprint);
    }

    private boolean hasAccess(ExamBlueprint blueprint, java.util.UUID currentSchoolId) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }
        return currentSchoolId != null && blueprint.getSchoolId().equals(currentSchoolId);
    }
}
