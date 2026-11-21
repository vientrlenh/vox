package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentCommand;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateAssessmentUseCase implements IUseCase<CreateAssessmentCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;
    private final AssessmentStreamConfigResolver assessmentStreamConfigResolver;
    private final AssessmentPolicyValidator assessmentPolicyValidator;
    private final SubscriptionPeriodGuardService subscriptionPeriodGuardService;

    public CreateAssessmentUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort,
            AssessmentStreamConfigResolver assessmentStreamConfigResolver,
            AssessmentPolicyValidator assessmentPolicyValidator,
            SubscriptionPeriodGuardService subscriptionPeriodGuardService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
        this.assessmentStreamConfigResolver = assessmentStreamConfigResolver;
        this.assessmentPolicyValidator = assessmentPolicyValidator;
        this.subscriptionPeriodGuardService = subscriptionPeriodGuardService;
    }

    @Override
    @Transactional
    public AssessmentDto execute(CreateAssessmentCommand input) {
        CreateAssessmentCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!schoolAdmin || currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (command.blueprintId() != null) {
            AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(command.blueprintId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
            if (!currentSchoolId.equals(blueprint.getSchoolId())) {
                throw new ForbiddenException("Blueprint không thuộc trường hiện tại");
            }
        }

        // Bỏ trống được ở bước tạo (kỳ thi tập trung chọn policy sau), nhưng đã gửi lên thì phải là
        // policy đã xuất bản và thuộc trường hiện tại — gắn nhầm thì bài chạy xong mới lộ ra là không
        // sinh được kết quả nào để chấm.
        assessmentPolicyValidator.validateIfPresent(command.assessmentPolicyId(), currentSchoolId);

        AssessmentStreamConfigResolver.StreamConfig streamConfig = assessmentStreamConfigResolver.resolve(
            command.requiredStreamTypes(), command.streamTypePermission());

        validateOpenClose(command.openAt(), command.closeAt(), currentSchoolId);

        Instant now = Instant.now();
        Assessment assessment = Assessment.builder()
                .blueprintId(command.blueprintId())
                .code(assessmentCodeOf(command.code()))
                .name(command.name())
                .description(command.description())
                .schoolId(currentSchoolId)
                .languageId(command.languageId())
                .kind(AssessmentKind.CENTRALIZED)
                // A centralized assessment is taken on school devices by default; a school that lets candidates
                // bring their own device into the room picks STUDENT_DEVICE when creating it.
                .deliveryMode(Objects.requireNonNullElse(command.deliveryMode(), AssessmentDeliveryMode.LAB))
                .status(AssessmentStatus.DRAFT)
                .maxAttempt(Objects.requireNonNullElse(command.maxAttempt(), 1))
                .assessmentTimeDurationSecond(command.assessmentTimeDurationSecond())
                .resultDecisionMethod(Objects.requireNonNullElse(command.resultDecisionMethod(), ResultDecisionMethod.HIGHEST))
                .openAt(command.openAt())
                .closeAt(command.closeAt())
                .assessmentPolicyId(command.assessmentPolicyId())
                .requiresOtp(command.requiresOtp() == null || command.requiresOtp())
                .requiredStreamType(streamConfig.requiredStreamType())
                .streamTypePermission(streamConfig.streamTypePermission())
                .aiConfidenceThresholdPercent(command.aiConfidenceThresholdPercent())
                .build();
        return AssessmentDtoMapper.toDto(assessmentRepository.save(assessment));
    }

    private CreateAssessmentCommand normalize(CreateAssessmentCommand input) {
        return new CreateAssessmentCommand(
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.languageId(),
            input.blueprintId(),
            input.openAt(),
            input.closeAt(),
            input.assessmentPolicyId(),
            input.maxAttempt(),
            input.assessmentTimeDurationSecond(),
            input.resultDecisionMethod(),
            input.requiresOtp(), 
            input.requiredStreamTypes() == null ? null : input.requiredStreamTypes().stream()
                .map(StringNormalization::normalizeCode)
                .toList(),
            input.streamTypePermission(),
            input.deliveryMode(),
            input.aiConfidenceThresholdPercent()
        );
    }

    /**
     * Both sides are mandatory: the open/close window of an assessment is the outer bound of every session
     * ({@code isScheduleWindowOutsideAssessmentWindow}), so leaving it empty means a session is not blocked by
     * any milestone at all -- including the plan deadline that {@code SubscriptionPeriodGuardService} has just applied.
     */
    private void validateOpenClose(Instant open, Instant close, UUID schoolId) {
        if (open == null || close == null) {
            throw new IllegalStateException("Kỳ thi phải có thời gian mở bài và đóng bài");
        }
        if (!open.isBefore(close)) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
        subscriptionPeriodGuardService.requireWithinSubscriptionPeriod(schoolId, open, close);
    }

    private String assessmentCodeOf(String code) {
        if (code != null && !code.isBlank()) {
            return code;
        }
        return "EX-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

}
