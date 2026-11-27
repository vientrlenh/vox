package com.sep.vox.application.usecase.assessmentcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.sep.vox.application.port.input.command.assessmentcandidate.AutoFillAssessmentCandidatesCommand;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AutoFillAssessmentCandidatesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AutoFillAssessmentCandidatesUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AutoFillAssessmentCandidatesUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    private final UUID schedule1 = UUID.randomUUID();
    private final UUID schedule2 = UUID.randomUUID();
    private final UUID room1 = UUID.randomUUID();
    private final UUID room2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        // The real access service on mocked repositories: the authorization rule is still tested exactly as it was
        // before it was extracted from the use case.
        useCase = new AutoFillAssessmentCandidatesUseCase(
            assessmentRepository, assessmentCandidateRepository, assessmentScheduleRepository,
            new AssessmentPaperAutoAssigner(mock(AssessmentPaperRepository.class), assessmentCandidateRepository),
            new AssessmentScheduleManageAccessService(
                assessmentMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort),
            new AssessmentScheduleCandidateConflictValidator(assessmentCandidateRepository, mock(UserRepository.class)));

        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });
    }

    @Test
    void should_distribute_candidates_round_robin_in_schedule_order() {
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(schedule2, room2, AssessmentScheduleStatus.PUBLISHED, Instant.parse("2026-01-02T09:00:00Z")),
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-01T09:00:00Z"))
        ));
        when(assessmentScheduleRepository.findByIdForUpdate(schedule1)).thenReturn(Optional.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-01T09:00:00Z"))));
        when(assessmentScheduleRepository.findByIdForUpdate(schedule2)).thenReturn(Optional.of(
            schedule(schedule2, room2, AssessmentScheduleStatus.PUBLISHED, Instant.parse("2026-01-02T09:00:00Z"))));
        AssessmentCandidate c1 = candidate();
        AssessmentCandidate c2 = candidate();
        AssessmentCandidate c3 = candidate();
        when(assessmentCandidateRepository.findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessmentId))
            .thenReturn(List.of(c1, c2, c3));

        List<AssessmentCandidateDto> result = useCase.execute(new AutoFillAssessmentCandidatesCommand(assessmentId, null));

        // Sessions are ordered by (startDate, id): schedule1 first, schedule2 second. Even
        // round-robin distribution: c1 -> schedule1, c2 -> schedule2, c3 -> schedule1.
        assertThat(result).hasSize(3);
        assertThat(c1.getScheduleId()).isEqualTo(schedule1);
        assertThat(c2.getScheduleId()).isEqualTo(schedule2);
        assertThat(c3.getScheduleId()).isEqualTo(schedule1);
    }

    @Test
    void should_lock_all_schedules_before_touching_candidates() {
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-01T09:00:00Z")),
            schedule(schedule2, room2, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-02T09:00:00Z"))
        ));
        when(assessmentScheduleRepository.findByIdForUpdate(schedule1)).thenReturn(Optional.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-01T09:00:00Z"))));
        when(assessmentScheduleRepository.findByIdForUpdate(schedule2)).thenReturn(Optional.of(
            schedule(schedule2, room2, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-02T09:00:00Z"))));
        when(assessmentCandidateRepository.findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessmentId))
            .thenReturn(List.of(candidate()));

        useCase.execute(new AutoFillAssessmentCandidatesCommand(assessmentId, null));

        // All schedules must be locked BEFORE candidates are loaded/saved.
        InOrder inOrder = inOrder(assessmentScheduleRepository, assessmentCandidateRepository);
        inOrder.verify(assessmentScheduleRepository).findByIdForUpdate(schedule1);
        inOrder.verify(assessmentScheduleRepository).findByIdForUpdate(schedule2);
        inOrder.verify(assessmentCandidateRepository).findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessmentId);
        inOrder.verify(assessmentCandidateRepository).saveAll(anyCollection());
    }

    @Test
    void should_restrict_to_given_schedule_ids() {
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-01T09:00:00Z")),
            schedule(schedule2, room2, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-02T09:00:00Z"))
        ));
        when(assessmentScheduleRepository.findByIdForUpdate(schedule2)).thenReturn(Optional.of(
            schedule(schedule2, room2, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-02T09:00:00Z"))));
        AssessmentCandidate c1 = candidate();
        when(assessmentCandidateRepository.findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessmentId))
            .thenReturn(List.of(c1));

        List<AssessmentCandidateDto> result = useCase.execute(new AutoFillAssessmentCandidatesCommand(assessmentId, List.of(schedule2)));

        assertThat(result).hasSize(1);
        assertThat(c1.getScheduleId()).isEqualTo(schedule2);
        verify(assessmentScheduleRepository, never()).findByIdForUpdate(schedule1);
    }

    @Test
    void should_return_empty_when_no_unassigned_candidates() {
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-01T09:00:00Z"))
        ));
        when(assessmentScheduleRepository.findByIdForUpdate(schedule1)).thenReturn(Optional.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-01T09:00:00Z"))));
        when(assessmentCandidateRepository.findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessmentId))
            .thenReturn(List.of());

        List<AssessmentCandidateDto> result = useCase.execute(new AutoFillAssessmentCandidatesCommand(assessmentId, null));

        assertThat(result).isEmpty();
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_return_empty_when_no_target_schedules() {
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.COMPLETED, Instant.parse("2026-01-01T09:00:00Z"))
        ));

        List<AssessmentCandidateDto> result = useCase.execute(new AutoFillAssessmentCandidatesCommand(assessmentId, null));

        assertThat(result).isEmpty();
        verify(assessmentScheduleRepository, never()).findByIdForUpdate(schedule1);
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_autofill_when_a_student_has_an_overlapping_schedule() {
        givenTwoLockedSchedules();
        AssessmentCandidate c1 = candidate();
        when(assessmentCandidateRepository.findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessmentId))
            .thenReturn(List.of(c1));
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                c1.getStudentId(), UUID.randomUUID(),
                Instant.parse("2026-01-01T09:00:00Z"), Instant.parse("2026-01-01T11:00:00Z"))));

        assertThatThrownBy(() -> useCase.execute(new AutoFillAssessmentCandidatesCommand(assessmentId, null)))
            .isInstanceOf(ResourceDuplicatedException.class);
        // The whole batch fails: nobody is assigned.
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
        assertThat(c1.getScheduleId()).isNull();
    }

    /**
     * Every assessment schedule has a different time window, so each group must be checked against the EXACT window
     * of the schedule that receives that group -- checking the whole batch against one window is wrong.
     */
    @Test
    void should_check_conflicts_per_target_schedule_window() {
        givenTwoLockedSchedules();
        when(assessmentCandidateRepository.findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessmentId))
            .thenReturn(List.of(candidate(), candidate()));

        useCase.execute(new AutoFillAssessmentCandidatesCommand(assessmentId, null));

        verify(assessmentCandidateRepository).findConflictsForStudents(anyCollection(),
            eq(Instant.parse("2026-01-01T09:00:00Z")), eq(Instant.parse("2026-01-01T11:00:00Z")), eq(null));
        verify(assessmentCandidateRepository).findConflictsForStudents(anyCollection(),
            eq(Instant.parse("2026-01-02T09:00:00Z")), eq(Instant.parse("2026-01-02T11:00:00Z")), eq(null));
    }

    @Test
    void should_skip_the_conflict_check_for_schedules_without_a_window() {
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, null)));
        when(assessmentScheduleRepository.findByIdForUpdate(schedule1)).thenReturn(Optional.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, null)));
        when(assessmentCandidateRepository.findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessmentId))
            .thenReturn(List.of(candidate()));

        useCase.execute(new AutoFillAssessmentCandidatesCommand(assessmentId, null));

        verify(assessmentCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    private void givenTwoLockedSchedules() {
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-01T09:00:00Z")),
            schedule(schedule2, room2, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-02T09:00:00Z"))
        ));
        when(assessmentScheduleRepository.findByIdForUpdate(schedule1)).thenReturn(Optional.of(
            schedule(schedule1, room1, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-01T09:00:00Z"))));
        when(assessmentScheduleRepository.findByIdForUpdate(schedule2)).thenReturn(Optional.of(
            schedule(schedule2, room2, AssessmentScheduleStatus.DRAFT, Instant.parse("2026-01-02T09:00:00Z"))));
    }

    private AssessmentCandidate candidate() {
        AssessmentCandidate c = new AssessmentCandidate();
        c.setId(UUID.randomUUID());
        c.setAssessmentId(assessmentId);
        c.setStudentId(UUID.randomUUID());
        c.setStatus(AssessmentCandidateStatus.ASSIGNED);
        c.setAssignedAt(Instant.now());
        return c;
    }

    private AssessmentSchedule schedule(UUID id, UUID roomId, AssessmentScheduleStatus status, Instant start) {
        AssessmentSchedule s = new AssessmentSchedule();
        s.setId(id);
        s.setAssessmentId(assessmentId);
        s.setSchoolRoomId(roomId);
        s.setStatus(status);
        s.setStartDate(start);
        // The anti-overlap rule only runs with a complete time window -- a missing end time means
        // it is skipped.
        s.setEndDate(start == null ? null : start.plus(2, ChronoUnit.HOURS));
        return s;
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }
}
