package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentCommand;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.application.shared.DateMapper;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultDecisionMethod;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.service.assessment.AssessmentDeliveryModeSupport;

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
            var blueprint = assessmentBlueprintRepository.findById(command.blueprintId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
            if (!currentSchoolId.equals(blueprint.getSchoolId())) {
                throw new ForbiddenException("Blueprint không thuộc trường hiện tại");
            }
        }

        // Bỏ trống được ở bước tạo (kỳ thi tập trung chọn policy sau), nhưng đã gửi lên thì phải là
        // policy đã xuất bản và thuộc trường hiện tại — gắn nhầm thì bài chạy xong mới lộ ra là không
        // sinh được kết quả nào để chấm.
        assessmentPolicyValidator.validateIfPresent(command.assessmentPolicyId(), currentSchoolId);

        var streamConfig = assessmentStreamConfigResolver.resolve(
            command.requiredStreamTypes(), command.streamTypePermission());

        validateOpenClose(command.openAt(), command.closeAt(), currentSchoolId);

        var now = Instant.now();
        var assessment = new Assessment(
            command.blueprintId(),
            null,
            assessmentCodeOf(command.code()),
            command.name(),
            command.description(),
            currentSchoolId,
            command.languageId(),
            AssessmentKind.CENTRALIZED,
            // Kỳ thi tập trung mặc định thi trên thiết bị nhà trường; trường nào cho thí sinh
            // mang máy riêng vào phòng thì chọn STUDENT_DEVICE ngay lúc tạo.
            AssessmentDeliveryModeSupport.parseOrDefault(command.deliveryMode(), AssessmentDeliveryMode.LAB),
            AssessmentStatus.DRAFT,
            Objects.requireNonNullElse(command.maxAttempt(), 1),
            command.assessmentTimeDurationSecond(),
            Objects.requireNonNullElse(command.resultDecisionMethod(), AssessmentResultDecisionMethod.HIGHEST),
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
        // Gán qua setter: constructor Assessment không nhận trường này (xem AssessmentMapper).
        assessment.setAiConfidenceThresholdPercent(command.aiConfidenceThresholdPercent());
        return AssessmentDto.toDto(assessmentRepository.save(assessment));
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
            input.streamTypePermission() == null ? null : StringNormalization.normalizeCode(input.streamTypePermission()),
            input.deliveryMode() == null ? null : StringNormalization.normalizeCode(input.deliveryMode()),
            input.aiConfidenceThresholdPercent()
        );
    }

    /**
     * Bắt buộc cả hai vế: khung mở/đóng của kỳ thi là ràng buộc ngoài của mọi ca thi
     * ({@code isScheduleWindowOutsideAssessmentWindow}), nên để trống đồng nghĩa ca thi không bị chặn bởi
     * bất cứ mốc nào -- kể cả hạn gói dịch vụ mà {@code SubscriptionPeriodGuardService} vừa áp.
     */
    private void validateOpenClose(String openAt, String closeAt, UUID schoolId) {
        var open = DateMapper.toInstant(openAt);
        var close = DateMapper.toInstant(closeAt);
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
