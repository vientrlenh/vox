package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.CreateAssessmentCommand;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class CreateAssessmentUseCase implements IUseCase<CreateAssessmentCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;
    private final AssessmentStreamConfigResolver assessmentStreamConfigResolver;
    private final AssessmentPolicyValidator assessmentPolicyValidator;
    private final SubscriptionPeriodGuardService subscriptionPeriodGuardService;

    @Override
    @Transactional
    public AssessmentDto execute(CreateAssessmentCommand input) {
        CreateAssessmentCommand command = normalize(input);
        UUID currentSchoolId = requireSchoolAdminSchoolId(userContextPort.getCurrentAuthenticatedUserId());

        if (command.blueprintId() != null) {
            requireBlueprintInSchool(command.blueprintId(), currentSchoolId);
        }

        // May be left empty at creation (a centralized assessment chooses its policy later), but once sent it must be a
        // published policy of the current school -- a wrong one only shows up after the assessment has run, as no result
        // can be generated for grading.
        assessmentPolicyValidator.validateIfPresent(command.assessmentPolicyId(), currentSchoolId);

        AssessmentStreamConfigResolver.StreamConfig streamConfig = assessmentStreamConfigResolver.resolve(
            command.requiredStreamTypes(), command.streamTypePermission());

        validateOpenClose(command.openAt(), command.closeAt(), currentSchoolId);

        return AssessmentDtoMapper.toDto(assessmentRepository.save(
            buildCentralizedDraft(command, currentSchoolId, streamConfig)));
    }

    private Assessment buildCentralizedDraft(
            CreateAssessmentCommand command,
            UUID currentSchoolId,
            AssessmentStreamConfigResolver.StreamConfig streamConfig) {
        Assessment assessment = Assessment.centralizedDraft(command.blueprintId(), assessmentCodeOf(command.code()),
            command.name(), command.description(), currentSchoolId, command.languageId());
        assessment.configure(command.deliveryMode(), command.maxAttempt(), command.assessmentTimeDurationSecond(),
            command.resultDecisionMethod(), command.requiresOtp());
        assessment.scheduleBetween(command.openAt(), command.closeAt(), command.assessmentPolicyId());
        assessment.useStreamProctoring(streamConfig.requiredStreamType(), streamConfig.streamTypePermission(),
            command.aiConfidenceThresholdPercent());
        return assessment;
    }

    private UUID requireSchoolAdminSchoolId(UUID currentUserId) {
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!schoolAdmin || currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return currentSchoolId;
    }

    private void requireBlueprintInSchool(UUID blueprintId, UUID currentSchoolId) {
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(blueprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!currentSchoolId.equals(blueprint.getSchoolId())) {
            throw new ForbiddenException("Blueprint không thuộc trường hiện tại");
        }
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
            throw new IllegalArgumentException("Kỳ thi phải có thời gian mở bài và đóng bài");
        }
        if (!open.isBefore(close)) {
            throw new IllegalArgumentException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
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
