package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.time.Instant;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentUseCase implements IUseCase<UpdateAssessmentCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentPolicyValidator assessmentPolicyValidator;
    private final AssessmentStreamConfigResolver assessmentStreamConfigResolver;
    private final UserContextPort userContextPort;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private final AssessmentScheduleRoomValidator assessmentScheduleRoomValidator;
    private final AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator;
    private final AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator;
    private final SubscriptionPeriodGuardService subscriptionPeriodGuardService;

    @Override
    @Transactional
    public AssessmentDto execute(UpdateAssessmentCommand input) {
        UpdateAssessmentCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        Assessment assessment = assessmentRepository.findById(command.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        authorizeMutation(assessment.getId(), assessment.getSchoolId(), assessment.getKind(), currentUserId, currentSchoolId, schoolAdmin);
        requireEditableForCurrentStatus(assessment);

        applyAndValidateChanges(assessment, command);
        Instant now = Instant.now();
        if (assessment.getKind() == AssessmentKind.CLASS_TEST && (command.openAt() != null || command.closeAt() != null)) {
            syncClassTestDraftSchedules(assessment, currentUserId, now);
        }
        return AssessmentDtoMapper.toDto(assessmentRepository.save(assessment));
    }

    private void applyAndValidateChanges(Assessment assessment, UpdateAssessmentCommand command) {
        applyBasicFields(assessment, command);
        applyPolicyAndAttemptFields(assessment, command);
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED
                && (command.maxAttempt() != null || command.assessmentTimeDurationSecond() != null)) {
            requireTokenQuotaStillWithinPlan(assessment);
        }
        applyGradingAndProctoringFields(assessment, command);
        requireWindowStillValid(assessment, command);
        // Checked against the school of the assessment itself, not the caller's school -- same reason as in the
        // assessmentPolicy branch of applyPolicyAndAttemptFields.
        subscriptionPeriodGuardService.requireWithinSubscriptionPeriod(
            assessment.getSchoolId(), assessment.getOpenAt(), assessment.getCloseAt());
    }

    private void requireEditableForCurrentStatus(Assessment assessment) {
        if (assessment.getKind() == AssessmentKind.CLASS_TEST
                && assessment.getStatus() != AssessmentStatus.DRAFT
                && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("A class test may only be updated before it starts");
        }
        // Regular assessment: candidates are already in the exam room, so any change to the information would put the running data
        // out of step (time window, attempt time, how the score is decided).
        AssessmentEditingGuard.requireAssessmentEditable(assessment);
    }

    private void applyBasicFields(Assessment assessment, UpdateAssessmentCommand command) {
        if (command.name() != null) {
            assessment.setName(command.name());
        }
        if (command.description() != null) {
            assessment.setDescription(command.description());
        }
        if (command.openAt() != null) {
            assessment.setOpenAt(command.openAt());
        }
        if (command.closeAt() != null) {
            assessment.setCloseAt(command.closeAt());
        }
    }

    private void applyPolicyAndAttemptFields(Assessment assessment, UpdateAssessmentCommand command) {
        if (command.assessmentPolicyId() != null) {
            // Checked against the school of the assessment itself, not the caller's school: the assessment already exists,
            // so its schoolId is the right scope.
            assessmentPolicyValidator.validateIfPresent(command.assessmentPolicyId(), assessment.getSchoolId());
            assessment.setAssessmentPolicyId(command.assessmentPolicyId());
        }
        if (command.maxAttempt() != null) {
            assessment.setMaxAttempt(command.maxAttempt());
        }
        if (command.assessmentTimeDurationSecond() != null) {
            assessment.setAssessmentTimeDurationSecond(command.assessmentTimeDurationSecond());
        }
    }

    /**
     * A published (SCHEDULED) assessment -- centralized or class test -- can still have its duration/maxAttempt edited. If it is
     * edited without re-checking, the token estimate checked at publish time
     * ({@code UpdateAssessmentStatusUseCase.validatePlanLimits}) becomes meaningless, so exactly the same guard has to run
     * again here, not only for CLASS_TEST.
     *
     * <p>Since the cost-generating seconds were separated from the assessment duration (see PaperTimeCalculator),
     * the estimate NO LONGER reads assessmentTimeDurationSecond but walks the paper variants itself, so only maxAttempt
     * really changes it. It is still re-checked when the duration changes because it is cheap, and because the REST path that writes that
     * column by hand may diverge from the real paper variants.
     */
    private void requireTokenQuotaStillWithinPlan(Assessment assessment) {
        classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
    }

    private void applyGradingAndProctoringFields(Assessment assessment, UpdateAssessmentCommand command) {
        if (command.resultDecisionMethod() != null) {
            assessment.setResultDecisionMethod(command.resultDecisionMethod());
        }
        if (command.requiresOtp() != null) {
            assessment.setRequiresOtp(command.requiresOtp());
        }
        if (command.aiConfidenceThresholdPercent() != null) {
            assessment.setAiConfidenceThresholdPercent(command.aiConfidenceThresholdPercent());
        }
        // null = keep as is (patch semantics like every other field); an EMPTY list = proctoring off.
        // The distinction is required: at creation null means "no proctoring", but here null must mean "do not touch",
        // otherwise renaming an assessment would silently wipe its proctoring configuration.
        if (command.requiredStreamTypes() != null) {
            AssessmentStreamConfigResolver.StreamConfig streamConfig = assessmentStreamConfigResolver.resolve(
                command.requiredStreamTypes(), command.streamTypePermission());
            assessment.setRequiredStreamType(streamConfig.requiredStreamType());
            assessment.setStreamTypePermission(streamConfig.streamTypePermission());
        }
    }

    private void requireWindowStillValid(Assessment assessment, UpdateAssessmentCommand command) {
        requireAssessmentWindow(assessment.getOpenAt(), assessment.getCloseAt(), assessment.getKind());
        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            // The open/close window is synced straight down to the session below, so it must be long enough for the attempt.
            if (touchesScheduleTiming(command)
                    && assessment.isScheduleWindowShorterThanAssessmentTime(assessment.getOpenAt(), assessment.getCloseAt())) {
                throw new IllegalArgumentException(AssessmentScheduleWindowMessages.schedulesNoLongerFit(1, assessment));
            }
        } else {
            requireExistingSchedulesStillFit(assessment, command);
        }
    }

    /**
     * Changing the open/close window of a class test writes the new time DIRECTLY down to the session, without going through
     * {@code UpdateAssessmentScheduleUseCase}. So it has to check exactly the rules that the other use case checks -- otherwise,
     * editing the time on the assessment screen becomes a detour around all three anti-overlap rules (a room booked
     * for two sessions, a teacher proctoring two sessions, a student placed in two sessions).
     */
    private void syncClassTestDraftSchedules(com.sep.vox.domain.model.assessment.Assessment assessment, java.util.UUID currentUserId, Instant now) {
        for (AssessmentSchedule schedule : assessmentScheduleRepository.findByAssessmentId(assessment.getId())) {
            if (schedule.getStatus() == AssessmentScheduleStatus.DRAFT || schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED) {
                assessmentScheduleRoomValidator.requireNoOverlap(
                    schedule.getSchoolRoomId(), assessment.getOpenAt(), assessment.getCloseAt(), schedule.getId());
                assessmentScheduleProctorConflictValidator.requireProctorsFreeForNewWindow(
                    schedule.getId(), assessment.getOpenAt(), assessment.getCloseAt());
                assessmentScheduleCandidateConflictValidator.requireCandidatesFreeForNewWindow(
                    schedule.getId(), assessment.getOpenAt(), assessment.getCloseAt());

                schedule.setStartDate(assessment.getOpenAt());
                schedule.setEndDate(assessment.getCloseAt());
                assessmentScheduleRepository.save(schedule);
            }
        }
    }

    private UpdateAssessmentCommand normalize(UpdateAssessmentCommand input) {
        return new UpdateAssessmentCommand(
            input.assessmentId(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
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
            input.aiConfidenceThresholdPercent()
        );
    }

    private void authorizeMutation(
            java.util.UUID assessmentId,
            java.util.UUID assessmentSchoolId,
            AssessmentKind kind,
            java.util.UUID currentUserId,
            java.util.UUID currentSchoolId,
            boolean schoolAdmin) {
        if (kind == AssessmentKind.CENTRALIZED) {
            if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
                return;
            }
            // A CHAIR runs the whole centralized assessment workflow on the detail page (set up the committee ->
            // lock the paper framework -> author the papers -> schedule), so they must be able to edit the assessment like a school admin.
            if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
                return;
            }
            throw new ForbiddenException("Access denied");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Access denied");
        }
        if (assessmentRepository.existsSubmittedSessionByAssessmentId(assessmentId)) {
            throw new IllegalArgumentException("The assessment cannot be updated after a submission exists");
        }
    }

    /**
     * A regular assessment: the session is an independent schedule (with a room, proctors and assigned candidates) so it must not be moved
     * along with the assessment by itself -- instead it blocks if the new time window or attempt time makes an already scheduled
     * session no longer valid. Only sessions that will still take place are considered; COMPLETED/MOVED/CANCELLED are the past
     * (findByAssessmentId has already excluded DELETED).
     */
    /**
     * Re-checks the assessment schedule invariant only when the operation really touches the time; editing the name
     * or description must not be blocked just because the existing data is already inconsistent.
     */
    private boolean touchesScheduleTiming(UpdateAssessmentCommand command) {
        return command.openAt() != null
            || command.closeAt() != null
            || command.assessmentTimeDurationSecond() != null;
    }

    private void requireExistingSchedulesStillFit(com.sep.vox.domain.model.assessment.Assessment assessment, UpdateAssessmentCommand command) {
        if (!touchesScheduleTiming(command)) {
            return;
        }
        List<AssessmentSchedule> activeSchedules = assessmentScheduleRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT
                || schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED)
            .toList();

        long outsideCount = activeSchedules.stream()
            .filter(schedule -> assessment.isScheduleWindowOutsideAssessmentWindow(schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (outsideCount > 0) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.schedulesOutsideNewWindow((int) outsideCount));
        }

        long tooShortCount = activeSchedules.stream()
            .filter(schedule -> assessment.isScheduleWindowShorterThanAssessmentTime(schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (tooShortCount > 0) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.schedulesNoLongerFit((int) tooShortCount, assessment));
        }
    }


    /**
     * Both kinds require an opening/closing window: for a class test because the window is the assessment schedule
     * itself, for a centralized assessment because it is the outer bound of every assessment schedule -- left empty,
     * the schedules have no limit at all, including the subscription plan's term.
     *
     * <p>An assessment created before this rule took effect may still have nulls; then both boundaries must be
     * entered before it can be saved, exactly like a newly created one.
     */
    private void requireAssessmentWindow(Instant openAt, Instant closeAt, AssessmentKind kind) {
        if (openAt == null || closeAt == null) {
            throw new IllegalArgumentException(kind == AssessmentKind.CLASS_TEST
                ? "A class test must have an opening time and a closing time"
                : "An assessment must have an opening time and a closing time");
        }
        if (!openAt.isBefore(closeAt)) {
            throw new IllegalArgumentException("The opening time must be earlier than the closing time");
        }
    }
}
