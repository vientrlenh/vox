package com.sep.vox.application.port.input.usecase.examblueprint;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.mapper.ExamBlueprintDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateExamBlueprintUseCase implements IUseCase<CreateExamBlueprintCommand, ExamBlueprintDto> {

    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateExamBlueprintUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            ExamMemberRepository examMemberRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamBlueprintDto execute(CreateExamBlueprintCommand input) {
        var command = new CreateExamBlueprintCommand(
            input.languageId(),
            input.schoolGradeLevelId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        if (!examMemberRepository.existsByUserIdAndRoleAndSchoolId(currentUserId, ExamMemberRole.AUTHOR, currentSchoolId)) {
            throw new ForbiddenException("Chỉ AUTHOR của ít nhất một exam trong trường mới được tạo blueprint");
        }
        if (command.schoolGradeLevelId() != null) {
            var gradeLevel = schoolGradeLevelRepository.findById(command.schoolGradeLevelId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khối lớp"));
            if (!currentSchoolId.equals(gradeLevel.getSchoolId())) {
                throw new IllegalStateException("Khối lớp không thuộc trường hiện tại");
            }
        }

        var now = OffsetDateTime.now();
        var blueprint = new ExamBlueprint(
            currentSchoolId,
            command.languageId(),
            command.schoolGradeLevelId(),
            blueprintCodeOf(command.code()),
            command.name(),
            command.description(),
            true,
            now,
            now,
            currentUserId,
            currentUserId
        );
        return ExamBlueprintDtoMapper.toDto(examBlueprintRepository.save(blueprint));
    }

    private String blueprintCodeOf(String code) {
        if (code != null && !code.isBlank()) {
            return code;
        }
        return "BP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
