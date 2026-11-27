package com.sep.vox.application.usecase.assessmentschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.assessmentschedule.AddAssessmentScheduleProctorCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.assessmentschedule.AddAssessmentScheduleProctorUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentScheduleProctorDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AddAssessmentScheduleProctorUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AddAssessmentScheduleProctorUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        // The real validator runs on the mocked proctor repository: the rule "do not proctor two schedules at the same time"
        // is tested through the use case itself instead of trusting a canned mock.
        useCase = new AddAssessmentScheduleProctorUseCase(
            assessmentRepository, assessmentScheduleRepository, assessmentScheduleProctorRepository,
            new AssessmentScheduleProctorConflictValidator(assessmentScheduleProctorRepository),
            new AssessmentScheduleManageAccessService(
                assessmentMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort),
            schoolUserRepository, userRoleQueryRepository);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule()));
    }

    @Test
    void should_add_proctor_on_happy_path() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, teacherId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId)).thenReturn(List.of(teacherRole()));
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, teacherId)).thenReturn(false);
        when(assessmentScheduleProctorRepository.save(any(AssessmentScheduleProctor.class)))
            .thenAnswer(inv -> {
                AssessmentScheduleProctor p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

        AssessmentScheduleProctorDto result = useCase.execute(new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, teacherId));

        assertThat(result.teacherId()).isEqualTo(teacherId);
        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(assessmentScheduleProctorRepository).save(any(AssessmentScheduleProctor.class));
    }

    @Test
    void should_reject_when_teacher_not_in_school() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, teacherId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, teacherId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleProctorRepository, never()).save(any());
    }

    @Test
    void should_reject_when_user_is_not_teacher() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, teacherId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, teacherId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleProctorRepository, never()).save(any());
    }

    @Test
    void should_reject_when_proctor_already_assigned() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, teacherId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId)).thenReturn(List.of(teacherRole()));
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, teacherId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, teacherId)))
            .isInstanceOf(ResourceDuplicatedException.class);
        verify(assessmentScheduleProctorRepository, never()).save(any());
    }

    @Test
    void should_reject_when_teacher_already_proctors_an_overlapping_schedule() {
        givenTeacherIsEligible();
        // Another schedule (of any assessment) overlaps the time window of the schedule being arranged.
        when(assessmentScheduleProctorRepository.existsOverlappingAssignment(teacherId, start, end, scheduleId))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, teacherId)))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("in this time range");
        verify(assessmentScheduleProctorRepository, never()).save(any());
    }

    /**
     * Two sessions that touch exactly at the boundary (the earlier one ends when the later one
     * starts) are not an overlap -- the intersection condition is half-open, exactly like the
     * room-check rule.
     */
    @Test
    void should_accept_when_teacher_has_no_overlapping_schedule() {
        givenTeacherIsEligible();
        when(assessmentScheduleProctorRepository.existsOverlappingAssignment(teacherId, start, end, scheduleId))
            .thenReturn(false);
        when(assessmentScheduleProctorRepository.save(any(AssessmentScheduleProctor.class)))
            .thenAnswer(inv -> {
                AssessmentScheduleProctor p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

        AssessmentScheduleProctorDto result = useCase.execute(new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, teacherId));

        assertThat(result.teacherId()).isEqualTo(teacherId);
        verify(assessmentScheduleProctorRepository).save(any(AssessmentScheduleProctor.class));
    }

    /** The rule scans the whole school, so the check must not carry any assessment constraint. */
    @Test
    void should_check_conflicts_across_all_assessments_of_the_teacher() {
        givenTeacherIsEligible();
        when(assessmentScheduleProctorRepository.save(any(AssessmentScheduleProctor.class)))
            .thenAnswer(inv -> {
                AssessmentScheduleProctor p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

        useCase.execute(new AddAssessmentScheduleProctorCommand(assessmentId, scheduleId, teacherId));

        verify(assessmentScheduleProctorRepository).existsOverlappingAssignment(teacherId, start, end, scheduleId);
    }

    private void givenTeacherIsEligible() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, teacherId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId)).thenReturn(List.of(teacherRole()));
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, teacherId)).thenReturn(false);
    }

    private UserRoleInfo teacherRole() {
        return new UserRoleInfo(UUID.randomUUID(), teacherId, UUID.randomUUID(), Instant.now(),
            "TEACHER", "Teacher");
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }

    private AssessmentSchedule schedule() {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(scheduleId);
        schedule.setAssessmentId(assessmentId);
        schedule.setSchoolRoomId(UUID.randomUUID());
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        schedule.setStatus(AssessmentScheduleStatus.DRAFT);
        return schedule;
    }
}
