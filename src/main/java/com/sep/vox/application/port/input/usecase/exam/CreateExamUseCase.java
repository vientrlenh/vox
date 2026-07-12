package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamDeliveryMode;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ResultDecisionMethod;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateExamUseCase implements IUseCase<CreateExamCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public CreateExamUseCase(
            ExamRepository examRepository,
            ExamBlueprintRepository examBlueprintRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamDto execute(CreateExamCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!schoolAdmin || currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (command.blueprintId() != null) {
            var blueprint = examBlueprintRepository.findById(command.blueprintId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
            if (!currentSchoolId.equals(blueprint.getSchoolId())) {
                throw new ForbiddenException("Blueprint không thuộc trường hiện tại");
            }
        }

        validateOpenClose(command.openAt(), command.closeAt());

        var now = OffsetDateTime.now();
        var exam = new Exam(
            command.blueprintId(),
            null,
            examCodeOf(command.code()),
            command.name(),
            command.description(),
            currentSchoolId,
            command.languageId(),
            ExamKind.CENTRALIZED,
            ExamDeliveryMode.LAB,
            ExamStatus.DRAFT,
            command.maxAttempt() == null ? 1 : command.maxAttempt(),
            command.resultDecisionMethod() == null ? ResultDecisionMethod.HIGHEST : command.resultDecisionMethod(),
            parseDateTime(command.openAt()),
            parseDateTime(command.closeAt()),
            command.assessmentPolicyId(),
            command.requiresOtp() == null || command.requiresOtp(),
            now,
            now,
            currentUserId,
            currentUserId
        );
        return ExamDtoMapper.toDto(examRepository.save(exam));
    }

    private CreateExamCommand normalize(CreateExamCommand input) {
        return new CreateExamCommand(
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.languageId(),
            input.blueprintId(),
            input.openAt(),
            input.closeAt(),
            input.assessmentPolicyId(),
            input.maxAttempt(),
            input.resultDecisionMethod(),
            input.requiresOtp()
        );
    }

    private void validateOpenClose(String openAt, String closeAt) {
        if (openAt == null || closeAt == null) {
            return;
        }
        if (!OffsetDateTime.parse(openAt).isBefore(OffsetDateTime.parse(closeAt))) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
    }

    private OffsetDateTime parseDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value);
    }

    private String examCodeOf(String code) {
        if (code != null && !code.isBlank()) {
            return code;
        }
        return "EX-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
