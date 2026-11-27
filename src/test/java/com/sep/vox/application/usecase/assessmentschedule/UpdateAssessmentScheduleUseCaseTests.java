package com.sep.vox.application.usecase.assessmentschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.assessmentschedule.UpdateAssessmentScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.assessmentschedule.UpdateAssessmentScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class UpdateAssessmentScheduleUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private SchoolRoomRepository schoolRoomRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private SubscriptionPeriodGuardService subscriptionPeriodGuardService;
    private UpdateAssessmentScheduleUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");
    private final Instant newStart = Instant.parse("2026-07-11T08:00:00+07:00");
    private final Instant newEnd = Instant.parse("2026-07-11T10:00:00+07:00");

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        schoolRoomRepository = mock(SchoolRoomRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        // The real validator runs on the two mocked repositories: the room check rule is still tested exactly
        // as it was before it was extracted from the use case.
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        subscriptionPeriodGuardService = mock(SubscriptionPeriodGuardService.class);
        useCase = new UpdateAssessmentScheduleUseCase(
            assessmentRepository, assessmentScheduleRepository,
            new AssessmentScheduleRoomValidator(schoolRoomRepository, assessmentScheduleRepository),
            new AssessmentScheduleProctorConflictValidator(assessmentScheduleProctorRepository),
            new AssessmentScheduleCandidateConflictValidator(assessmentCandidateRepository, mock(UserRepository.class)),
            assessmentMemberRepository,
            schoolUserRepository, userRoleQueryRepository,
            subscriptionPeriodGuardService, userContextPort);

        when(assessmentCandidateRepository.findByScheduleId(any(UUID.class))).thenReturn(List.of());
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
    }

    @Test
    void should_update_draft_schedule_time() {
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(assessmentScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(false), eq(newStart), eq(newEnd)))
            .thenReturn(1);

        UUID result = useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd));

        assertThat(result).isEqualTo(scheduleId);
        verify(assessmentScheduleRepository).updateAtomic(eq(scheduleId), eq(null), eq(false), eq(newStart), eq(newEnd));
    }

    @Test
    void should_clear_the_room_of_a_centralized_schedule_without_checking_it() {
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(true), eq(null), eq(null)))
            .thenReturn(1);

        UUID result = useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, true, null, null));

        assertThat(result).isEqualTo(scheduleId);
        verify(schoolRoomRepository, never()).findById(any());
        verify(assessmentScheduleRepository, never()).existsOverlapping(any(), any(), any(), any());
    }

    @Test
    void should_clear_the_room_of_a_class_test_schedule() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(classTestAssessment()));
        AssessmentSchedule stored = schedule(AssessmentScheduleStatus.DRAFT);
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(stored));

        useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, true, null, null));

        assertThat(stored.getSchoolRoomId()).isNull();
        verify(assessmentScheduleRepository).save(stored);
        verify(assessmentScheduleRepository, never()).existsOverlapping(any(), any(), any(), any());
    }

    @Test
    void should_keep_the_stored_room_when_the_room_is_not_provided() {
        AssessmentSchedule stored = schedule(AssessmentScheduleStatus.DRAFT);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(classTestAssessment()));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(stored));

        useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd));

        assertThat(stored.getSchoolRoomId()).isEqualTo(roomId);
        verify(assessmentScheduleRepository).existsOverlapping(roomId, newStart, newEnd, scheduleId);
    }

    @Test
    void should_reject_when_schedule_not_draft() {
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.PUBLISHED)));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleRepository, never()).updateAtomic(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void should_reject_when_new_time_overlaps() {
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd)))
            .isInstanceOf(ResourceDuplicatedException.class);
        verify(assessmentScheduleRepository, never()).updateAtomic(any(), any(), anyBoolean(), any(), any());
    }

    /**
     * An easily missed hole: assign a proctor while the two sessions do not touch, then move their
     * times so that they overlap. Blocking only at assignment time is not enough.
     */
    @Test
    void should_reject_moving_schedule_when_proctor_busy_in_new_window() {
        UUID teacherId = UUID.randomUUID();
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(assessmentScheduleProctorRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(AssessmentScheduleProctor.builder()
                    .scheduleId(scheduleId)
                    .teacherId(teacherId)
                    .build()));
        when(assessmentScheduleProctorRepository.existsOverlappingAssignment(teacherId, newStart, newEnd, scheduleId))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd)))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("The proctor");
        verify(assessmentScheduleRepository, never()).updateAtomic(any(), any(), anyBoolean(), any(), any());
    }

    /**
     * The same hole, along the candidate axis: place a student while there is no overlap, then move
     * the times so they overlap.
     */
    @Test
    void should_reject_moving_schedule_when_student_busy_in_new_window() {
        AssessmentCandidate candidate = candidateOfSchedule();
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidate));
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                candidate.getStudentId(), UUID.randomUUID(), newStart, newEnd)));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd)))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("already has another assessment schedule");
        verify(assessmentScheduleRepository, never()).updateAtomic(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void should_ignore_candidates_of_the_schedule_being_moved() {
        // A candidate of the very session whose time is being changed is of course "busy" in that
        // session -- it must not block itself.
        AssessmentCandidate candidate = candidateOfSchedule();
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidate));
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                candidate.getStudentId(), scheduleId, newStart, newEnd)));
        when(assessmentScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(false), eq(newStart), eq(newEnd)))
            .thenReturn(1);

        UUID result = useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd));

        assertThat(result).isEqualTo(scheduleId);
    }

    private AssessmentCandidate candidateOfSchedule() {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(assessmentId);
        candidate.setStudentId(UUID.randomUUID());
        candidate.setScheduleId(scheduleId);
        candidate.setStatus(AssessmentCandidateStatus.ASSIGNED);
        return candidate;
    }

    @Test
    void should_reject_when_atomic_update_affects_no_rows() {
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(assessmentScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(false), eq(newStart), eq(newEnd)))
            .thenReturn(0);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_when_new_window_shorter_than_assessment_time_duration() {
        Assessment assessment = assessment();
        // The new time window is 2 hours but the assessment duration is 3 hours.
        assessment.setAssessmentTimeDurationSecond(3 * 3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("answering time");
        verify(assessmentScheduleRepository, never()).updateAtomic(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void should_reject_when_new_window_falls_outside_assessment_window() {
        Assessment assessment = assessment();
        assessment.setOpenAt(start);
        assessment.setCloseAt(end);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));

        // newStart/newEnd are the next day, outside the assessment's open/close window.
        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opening and closing");
        verify(assessmentScheduleRepository, never()).updateAtomic(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void should_allow_moving_class_test_schedule_outside_current_assessment_window() {
        // For CLASS_TEST, the session is what defines the assessment window (openAt/closeAt are written
        // back from the session), so moving the session outside the current window is valid.
        Assessment assessment = assessment();
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(AssessmentStatus.DRAFT);
        assessment.setOpenAt(start);
        assessment.setCloseAt(end);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);

        UUID result = useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd));

        assertThat(result).isEqualTo(scheduleId);
        assertThat(assessment.getOpenAt()).isEqualTo(newStart);
        assertThat(assessment.getCloseAt()).isEqualTo(newEnd);
        verify(assessmentRepository).save(assessment);
    }

    /**
     * Editing the session of a class test writes openAt/closeAt back down to the assessment, so it is
     * another path that writes the open/close window -- without checking the plan limit here,
     * changing a session's time would be a way around the constraint that Create/UpdateAssessmentUseCase
     * apply.
     */
    @Test
    void should_reject_moving_class_test_schedule_outside_subscription_period() {
        Assessment assessment = classTestAssessment();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        doThrow(new IllegalArgumentException("The closing time must be within the school's subscription plan term"))
            .when(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("subscription plan term");
        verify(assessmentScheduleRepository, never()).save(any());
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_check_subscription_period_with_effective_window_of_the_class_test() {
        Assessment assessment = classTestAssessment();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);

        useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd));

        verify(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(schoolId, newStart, newEnd);
    }

    /**
     * A centralized assessment takes another path: its schedules are bound to lie WITHIN the assessment's opening/closing window, and
     * that window was already checked against the plan term when the assessment was created/edited -- checking again here is redundant.
     */
    @Test
    void should_not_check_subscription_period_for_centralized_schedule() {
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(assessmentScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(false), eq(newStart), eq(newEnd)))
            .thenReturn(1);

        useCase.execute(new UpdateAssessmentScheduleCommand(scheduleId, null, false, newStart, newEnd));

        verifyNoInteractions(subscriptionPeriodGuardService);
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }

    private Assessment classTestAssessment() {
        Assessment assessment = assessment();
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(AssessmentStatus.DRAFT);
        assessment.setOpenAt(start);
        assessment.setCloseAt(end);
        return assessment;
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(scheduleId);
        schedule.setAssessmentId(assessmentId);
        schedule.setSchoolRoomId(roomId);
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        schedule.setStatus(status);
        return schedule;
    }
}
