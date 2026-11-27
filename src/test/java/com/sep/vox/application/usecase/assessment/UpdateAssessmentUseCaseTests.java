package com.sep.vox.application.usecase.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentCommand;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class UpdateAssessmentUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private SubscriptionPeriodGuardService subscriptionPeriodGuardService;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private UpdateAssessmentUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    private final Instant open = OffsetDateTime.parse("2026-07-10T08:00:00+07:00").toInstant();
    private final Instant close = OffsetDateTime.parse("2026-07-10T11:00:00+07:00").toInstant();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        classTestTokenQuotaGuardService = mock(ClassTestTokenQuotaGuardService.class);
        subscriptionPeriodGuardService = mock(SubscriptionPeriodGuardService.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        useCase = new UpdateAssessmentUseCase(
            assessmentRepository, assessmentMemberRepository, assessmentScheduleRepository,
            schoolUserRepository, userRoleQueryRepository,
            new AssessmentPolicyValidator(mock(AssessmentPolicyRepository.class)),
            new AssessmentStreamConfigResolver(),
            userContextPort,
            classTestTokenQuotaGuardService,
            // Three real validators on a mocked repository: editing the time of a class test writes straight down to the session
            // so it has to check exactly the rules that UpdateAssessmentScheduleUseCase checks.
            new AssessmentScheduleRoomValidator(mock(SchoolRoomRepository.class), assessmentScheduleRepository),
            new AssessmentScheduleProctorConflictValidator(assessmentScheduleProctorRepository),
            new AssessmentScheduleCandidateConflictValidator(assessmentCandidateRepository, mock(UserRepository.class)),
            subscriptionPeriodGuardService);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.existsSubmittedSessionByAssessmentId(assessmentId)).thenReturn(false);
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assessmentScheduleProctorRepository.findByScheduleId(any(UUID.class))).thenReturn(List.of());
        when(assessmentCandidateRepository.findByScheduleId(any(UUID.class))).thenReturn(List.of());
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
    }

    // --- CLASS_TEST: the new opening/closing window is synced down to the assessment schedule, so it must be long enough ---

    @Test
    void should_reject_class_test_window_shorter_than_assessment_time() {
        Assessment assessment = classTest(3 * 3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        // Shrinks the window to 1 hour while the assessment duration is 3 hours.
        assertThatThrownBy(() -> useCase.execute(command(open, open.plus(1, ChronoUnit.HOURS))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("closing time");
        verify(assessmentScheduleRepository, never()).save(any());
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_allow_renaming_class_test_without_revalidating_window() {
        // The guard may only run when the operation really touches time; renaming a test whose
        // window is already too short must not be blocked (consistent with the regular assessment
        // branch).
        Assessment assessment = classTest(3 * 3600);
        assessment.setOpenAt(open);
        assessment.setCloseAt(open.plus(1, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(new UpdateAssessmentCommand(
            assessmentId, "Tên mới", null, null, null, null, null, null, null, null, null, null, null));

        verify(assessmentRepository).save(assessment);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_sync_class_test_schedules_when_window_valid() {
        Assessment assessment = classTest(3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, open, close);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));

        useCase.execute(command(open, close.plus(1, ChronoUnit.HOURS)));

        assertThat(schedule.getStartDate()).isEqualTo(open);
        assertThat(schedule.getEndDate()).isEqualTo(close.plus(1, ChronoUnit.HOURS));
        verify(assessmentScheduleRepository).save(schedule);
    }

    // --- Editing the time of a class test writes straight to its assessment schedule, so all three anti-overlap rules must be checked ---

    @Test
    void should_reject_class_test_window_change_when_a_student_is_busy_in_the_new_window() {
        Assessment assessment = classTest(3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, open, close);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        UUID studentId = UUID.randomUUID();
        when(assessmentCandidateRepository.findByScheduleId(schedule.getId())).thenReturn(List.of(candidate(schedule.getId(), studentId)));
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(studentId, UUID.randomUUID(), open, close)));

        assertThatThrownBy(() -> useCase.execute(command(open, close.plus(1, ChronoUnit.HOURS))))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("already has another assessment schedule");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_class_test_window_change_when_a_proctor_is_busy_in_the_new_window() {
        Assessment assessment = classTest(3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, open, close);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        UUID teacherId = UUID.randomUUID();
        when(assessmentScheduleProctorRepository.findByScheduleId(schedule.getId()))
            .thenReturn(List.of(AssessmentScheduleProctor.builder()
                    .scheduleId(schedule.getId())
                    .teacherId(teacherId)
                    .build()));
        when(assessmentScheduleProctorRepository.existsOverlappingAssignment(
            eq(teacherId), any(Instant.class), any(Instant.class), eq(schedule.getId()))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command(open, close.plus(1, ChronoUnit.HOURS))))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("The proctor");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_class_test_window_change_when_the_room_is_taken() {
        Assessment assessment = classTest(3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED, open, close);
        UUID roomId = UUID.randomUUID();
        schedule.setSchoolRoomId(roomId);
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));
        when(assessmentScheduleRepository.existsOverlapping(
            eq(roomId), any(Instant.class), any(Instant.class), eq(schedule.getId()))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command(open, close.plus(1, ChronoUnit.HOURS))))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("The room");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    private AssessmentCandidate candidate(UUID scheduleId, UUID studentId) {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(assessmentId);
        candidate.setStudentId(studentId);
        candidate.setScheduleId(scheduleId);
        candidate.setStatus(AssessmentCandidateStatus.ASSIGNED);
        return candidate;
    }

    // --- The opening/closing window must lie within the school's subscription plan term ---

    /**
     * Look at the school of the test ITSELF, not the caller's school: the test already exists, so
     * its schoolId is the right scope (a SCHOOL_ADMIN of another school must not borrow their own
     * school's plan allowance to widen the window of the other school's test).
     */
    @Test
    void should_check_subscription_period_against_school_of_the_assessment() {
        Assessment assessment = classTest(3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(command(open, close));

        verify(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(schoolId, open, close);
    }

    @Test
    void should_reject_when_new_window_falls_outside_subscription_period() {
        Assessment assessment = classTest(3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        doThrow(new IllegalArgumentException("The closing time must be within the school's subscription plan term"))
            .when(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(command(open, close)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("subscription plan term");
        // Block BEFORE writing: the sessions of a class test are synchronized to this window, so
        // letting it through would push the sessions beyond the plan limit as well.
        verify(assessmentRepository, never()).save(any());
        verify(assessmentScheduleRepository, never()).save(any());
    }

    /**
     * An old centralized assessment that is still missing time points must have them filled in before it
     * can be saved, rather than silently skipped.
     */
    @Test
    void should_reject_updating_centralized_assessment_without_window() {
        Assessment assessment = centralized(3600);
        assessment.setOpenAt(null);
        assessment.setCloseAt(null);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentCommand(
                assessmentId, "Tên mới", null, null, null, null, null, null, null, null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("An assessment must have an opening time and a closing time");
        verify(assessmentRepository, never()).save(any());
    }

    // --- A published class test: editing duration/maxAttempt must re-check the token quota ---

    @Test
    void should_reject_editing_scheduled_class_test_when_token_quota_exceeded() {
        Assessment assessment = classTest(3600);
        assessment.setStatus(AssessmentStatus.SCHEDULED);
        assessment.setOpenAt(open);
        assessment.setCloseAt(close);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        doThrow(new BusinessRuleViolationException("The quota has been exceeded"))
            .when(classTestTokenQuotaGuardService).requireWithinTokenQuota(assessment);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentCommand(
                assessmentId, null, null, null, null, null, null, 2 * 3600, null, null, null, null, null)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("quota has been exceeded");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_not_revalidate_quota_when_editing_draft_class_test() {
        // Not published yet, so there is no old estimate to re-check -- publishing will check it
        // later.
        Assessment assessment = classTest(3 * 3600);
        assessment.setOpenAt(open);
        assessment.setCloseAt(open.plus(6, ChronoUnit.HOURS));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(new UpdateAssessmentCommand(
            assessmentId, null, null, null, null, null, null, 2 * 3600, null, null, null, null, null));

        verifyNoInteractions(classTestTokenQuotaGuardService);
        verify(assessmentRepository).save(assessment);
    }

    @Test
    void should_not_revalidate_quota_when_scheduled_class_test_edit_does_not_touch_duration_or_attempt() {
        Assessment assessment = classTest(3600);
        assessment.setStatus(AssessmentStatus.SCHEDULED);
        assessment.setOpenAt(open);
        assessment.setCloseAt(close);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(new UpdateAssessmentCommand(
            assessmentId, "Tên mới", null, null, null, null, null, null, null, null, null, null, null));

        verifyNoInteractions(classTestTokenQuotaGuardService);
        verify(assessmentRepository).save(assessment);
    }

    // --- Regular assessment: schedules are independent, so changing the assessment window must re-check the existing schedules ---

    @Test
    void should_reject_when_new_assessment_window_leaves_schedules_outside() {
        Assessment assessment = centralized(null);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId))
            .thenReturn(List.of(schedule(AssessmentScheduleStatus.PUBLISHED, open, close)));

        // Moves the assessment window to the next day, so the old session falls outside.
        assertThatThrownBy(() -> useCase.execute(
                command(open.plus(1, ChronoUnit.DAYS), close.plus(1, ChronoUnit.DAYS))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("outside");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_reject_when_new_assessment_time_duration_exceeds_existing_schedules() {
        Assessment assessment = centralized(null);
        assessment.setOpenAt(open);
        assessment.setCloseAt(close);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        // The session lasts 1 hour; sets the assessment duration to 2 hours.
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId))
            .thenReturn(List.of(schedule(AssessmentScheduleStatus.DRAFT, open, open.plus(1, ChronoUnit.HOURS))));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentCommand(
                assessmentId, null, null, null, null, null, null, 2 * 3600, null, null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds the duration");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_ignore_completed_and_cancelled_schedules() {
        Assessment assessment = centralized(null);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(AssessmentScheduleStatus.COMPLETED, open, close),
            schedule(AssessmentScheduleStatus.CANCELLED, open, close),
            schedule(AssessmentScheduleStatus.MOVED, open, close)));

        AssessmentDto result = useCase.execute(command(open.plus(1, ChronoUnit.DAYS), close.plus(1, ChronoUnit.DAYS)));

        assertThat(result).isNotNull();
        verify(assessmentRepository).save(any(Assessment.class));
    }

    @Test
    void should_allow_updating_name_only_without_touching_schedules() {
        Assessment assessment = centralized(null);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(new UpdateAssessmentCommand(
            assessmentId, "Tên mới", null, null, null, null, null, null, null, null, null, null, null));

        // No time field is sent -> no need to query the sessions.
        verify(assessmentScheduleRepository, never()).findByAssessmentId(any());
        verify(assessmentRepository).save(any(Assessment.class));
    }

    // --- Once the assessment has started, editing its information is locked ---

    @Test
    void should_reject_updating_centralized_assessment_after_it_started() {
        Assessment assessment = centralized(null);
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentCommand(
                assessmentId, "Tên mới", null, null, null, null, null, null, null, null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has started");
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_reject_updating_centralized_assessment_after_results_published() {
        Assessment assessment = centralized(null);
        assessment.setStatus(AssessmentStatus.RESULTS_PUBLISHED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentCommand(
                assessmentId, "Tên mới", null, null, null, null, null, null, null, null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_allow_updating_centralized_assessment_while_scheduled() {
        Assessment assessment = centralized(null);
        assessment.setStatus(AssessmentStatus.SCHEDULED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(new UpdateAssessmentCommand(
            assessmentId, "Tên mới", null, null, null, null, null, null, null, null, null, null, null));

        verify(assessmentRepository).save(assessment);
    }

    // --- Proctoring configuration (stream) ---

    @Test
    void should_update_stream_config_when_types_provided() {
        Assessment assessment = centralized(null);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(streamCommand(List.of("CAMERA", "SCREEN"), AssessmentStreamTypePermission.ANY));

        assertThat(assessment.getRequiredStreamType()).isEqualTo(AssessmentRequiredStreamType.CAMERA_AND_SCREEN);
        assertThat(assessment.getStreamTypePermission()).isEqualTo(AssessmentStreamTypePermission.ANY);
        verify(assessmentRepository).save(assessment);
    }

    @Test
    void should_keep_stream_config_when_types_null() {
        // Renaming must not lose the proctoring configuration: null = "do not touch", not "turn
        // off".
        Assessment assessment = centralized(null);
        assessment.setRequiredStreamType(AssessmentRequiredStreamType.CAMERA);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(new UpdateAssessmentCommand(
            assessmentId, "Tên mới", null, null, null, null, null, null, null, null, null, null, null));

        assertThat(assessment.getRequiredStreamType()).isEqualTo(AssessmentRequiredStreamType.CAMERA);
    }

    @Test
    void should_disable_monitoring_when_stream_types_empty() {
        Assessment assessment = centralized(null);
        assessment.setRequiredStreamType(AssessmentRequiredStreamType.CAMERA_AND_SCREEN);
        assessment.setStreamTypePermission(AssessmentStreamTypePermission.ALL);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(streamCommand(List.of(), null));

        assertThat(assessment.getRequiredStreamType()).isNull();
        assertThat(assessment.getStreamTypePermission()).isNull();
    }

    @Test
    void should_reject_permission_without_two_stream_types() {
        Assessment assessment = centralized(null);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(streamCommand(List.of("CAMERA"), AssessmentStreamTypePermission.ALL)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_reject_stream_update_after_assessment_started() {
        Assessment assessment = centralized(null);
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(streamCommand(List.of("CAMERA"), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has started");
        verify(assessmentRepository, never()).save(any());
    }

    // --- A CHAIR can edit a centralized assessment like a school admin ---

    @Test
    void should_allow_the_chair_to_update_a_centralized_assessment() {
        Assessment assessment = centralized(null);
        assessment.setName("Cũ");
        givenCallerIsChairInsteadOfSchoolAdmin();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        useCase.execute(new UpdateAssessmentCommand(
            assessmentId, "Mới", null, null, null, null, null, null, null, null, null, null, null));

        assertThat(assessment.getName()).isEqualTo("Mới");
        verify(assessmentRepository).save(assessment);
    }

    @Test
    void should_reject_updating_centralized_assessment_when_caller_is_neither_school_admin_nor_chair() {
        Assessment assessment = centralized(null);
        givenCallerIsChairInsteadOfSchoolAdmin();
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentCommand(
            assessmentId, "Mới", null, null, null, null, null, null, null, null, null, null, null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Access denied");
        verify(assessmentRepository, never()).save(any());
    }

    /** Demotes the caller from school admin to a teacher who only holds the CHAIR role of that very assessment. */
    private void givenCallerIsChairInsteadOfSchoolAdmin() {
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));
    }

    private UpdateAssessmentCommand streamCommand(
            List<String> requiredStreamTypes, AssessmentStreamTypePermission streamTypePermission) {
        return new UpdateAssessmentCommand(
            assessmentId, null, null, null, null, null, null, null, null, null,
            requiredStreamTypes, streamTypePermission, null);
    }

    private UpdateAssessmentCommand command(Instant openAt, Instant closeAt) {
        return new UpdateAssessmentCommand(assessmentId, null, null, openAt, closeAt, null, null, null, null, null, null, null, null);
    }

    private Assessment classTest(Integer assessmentTimeDurationSecond) {
        Assessment assessment = baseAssessment(AssessmentKind.CLASS_TEST, assessmentTimeDurationSecond);
        assessment.setStatus(AssessmentStatus.DRAFT);
        return assessment;
    }

    private Assessment centralized(Integer assessmentTimeDurationSecond) {
        Assessment assessment = baseAssessment(AssessmentKind.CENTRALIZED, assessmentTimeDurationSecond);
        assessment.setStatus(AssessmentStatus.DRAFT);
        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(schoolId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(schoolUser));
        UserRoleInfo role = new UserRoleInfo(
            UUID.randomUUID(), userId, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường");
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of(role));
        return assessment;
    }

    private Assessment baseAssessment(AssessmentKind kind, Integer assessmentTimeDurationSecond) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setKind(kind);
        assessment.setAssessmentTimeDurationSecond(assessmentTimeDurationSecond);
        // Both kinds require an open/close window, so every test must already have one -- a test
        // that needs a different window overrides it with setOpenAt/setCloseAt.
        assessment.setOpenAt(open);
        assessment.setCloseAt(close);
        return assessment;
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status, Instant start, Instant end) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAssessmentId(assessmentId);
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        schedule.setStatus(status);
        return schedule;
    }
}
