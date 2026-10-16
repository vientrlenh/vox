package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateAssessmentBlueprintUseCase implements IUseCase<CreateAssessmentBlueprintCommand, AssessmentBlueprintDto> {

    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateAssessmentBlueprintUseCase(
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            GradeLevelRepository gradeLevelRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentBlueprintDto execute(CreateAssessmentBlueprintCommand input) {
        var command = new CreateAssessmentBlueprintCommand(
            input.languageId(),
            input.gradeLevelId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        // Khối lớp là catalog dùng chung: chỉ cần tồn tại, không còn ràng buộc thuộc trường nào.
        if (command.gradeLevelId() != null
                && gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy khối lớp");
        }

        var now = Instant.now();
        var blueprint = new AssessmentBlueprint(
            currentSchoolId,
            command.languageId(),
            command.gradeLevelId(),
            blueprintCodeOf(command.code()),
            command.name(),
            command.description(),
            true,
            now,
            now,
            currentUserId,
            currentUserId
        );
        return AssessmentBlueprintDto.toDto(assessmentBlueprintRepository.save(blueprint));
    }

    private String blueprintCodeOf(String code) {
        if (code != null && !code.isBlank()) {
            return code;
        }
        return "BP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
