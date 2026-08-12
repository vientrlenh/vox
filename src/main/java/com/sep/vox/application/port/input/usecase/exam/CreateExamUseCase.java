package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamCommand;
import com.sep.vox.application.port.input.service.ExamAssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.ExamStreamConfigResolver;
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
import com.sep.vox.domain.service.exam.ExamDeliveryModeSupport;

@Service
public class CreateExamUseCase implements IUseCase<CreateExamCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;
    private final ExamStreamConfigResolver examStreamConfigResolver;
    private final ExamAssessmentPolicyValidator examAssessmentPolicyValidator;

    public CreateExamUseCase(
            ExamRepository examRepository,
            ExamBlueprintRepository examBlueprintRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort,
            ExamStreamConfigResolver examStreamConfigResolver,
            ExamAssessmentPolicyValidator examAssessmentPolicyValidator) {
        this.examRepository = examRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
        this.examStreamConfigResolver = examStreamConfigResolver;
        this.examAssessmentPolicyValidator = examAssessmentPolicyValidator;
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

        // Bỏ trống được ở bước tạo (kỳ thi tập trung chọn policy sau), nhưng đã gửi lên thì phải là
        // policy đã xuất bản và thuộc trường hiện tại — gắn nhầm thì bài chạy xong mới lộ ra là không
        // sinh được kết quả nào để chấm.
        examAssessmentPolicyValidator.validateIfPresent(command.assessmentPolicyId(), currentSchoolId);

        var streamConfig = examStreamConfigResolver.resolve(
            command.requiredStreamTypes(), command.streamTypePermission());

        validateOpenClose(command.openAt(), command.closeAt());

        var now = Instant.now();
        var exam = new Exam(
            command.blueprintId(),
            null,
            examCodeOf(command.code()),
            command.name(),
            command.description(),
            currentSchoolId,
            command.languageId(),
            ExamKind.CENTRALIZED,
            // Kỳ thi tập trung mặc định thi trên thiết bị nhà trường; trường nào cho thí sinh
            // mang máy riêng vào phòng thì chọn STUDENT_DEVICE ngay lúc tạo.
            ExamDeliveryModeSupport.parseOrDefault(command.deliveryMode(), ExamDeliveryMode.LAB),
            ExamStatus.DRAFT,
            Objects.requireNonNullElse(command.maxAttempt(), 1),
            command.examTimeDurationSecond(),
            Objects.requireNonNullElse(command.resultDecisionMethod(), ResultDecisionMethod.HIGHEST),
            DateMapper.toInstant(command.openAt()),
            DateMapper.toInstant(command.closeAt()),
            command.assessmentPolicyId(),
            command.requiresOtp() == null || command.requiresOtp(),
            now,
            streamConfig.requiredStreamType(),
            streamConfig.streamTypePermission(),
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
            input.examTimeDurationSecond(),
            input.resultDecisionMethod(),
            input.requiresOtp(), 
            input.requiredStreamTypes() == null ? null : input.requiredStreamTypes().stream()
                .map(StringNormalization::normalizeCode)
                .toList(),
            input.streamTypePermission() == null ? null : StringNormalization.normalizeCode(input.streamTypePermission()),
            input.deliveryMode() == null ? null : StringNormalization.normalizeCode(input.deliveryMode())
        );
    }

    /** Chỉ so được khi có đủ cả hai; thiếu một vế thì bỏ qua, kỳ thi tập trung cho phép để trống. */
    private void validateOpenClose(String openAt, String closeAt) {
        var open = DateMapper.toInstant(openAt);
        var close = DateMapper.toInstant(closeAt);
        if (open == null || close == null) {
            return;
        }
        if (!open.isBefore(close)) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
    }

    private String examCodeOf(String code) {
        if (code != null && !code.isBlank()) {
            return code;
        }
        return "EX-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

}
