package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewAssessmentBlueprintDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentBlueprintDetailsUseCase implements IUseCase<ViewAssessmentBlueprintDetailsQuery, AssessmentBlueprintDto> {


    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    public AssessmentBlueprintDto execute(ViewAssessmentBlueprintDetailsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi với id: " + input.id()));
        if (!hasAccess(blueprint, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối: không có quyền truy cập blueprint đề thi này");
        }
        return AssessmentBlueprintDtoMapper.toDto(blueprint);
    }

    private boolean hasAccess(AssessmentBlueprint blueprint, UUID currentSchoolId) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }
        return currentSchoolId != null && blueprint.getSchoolId().equals(currentSchoolId);
    }
}
