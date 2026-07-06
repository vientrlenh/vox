package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.sep.vox.application.port.input.command.AutoFillExamCandidatesCommand;
import com.sep.vox.application.port.input.usecase.examcandidate.AutoFillExamCandidatesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AutoFillExamCandidatesUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AutoFillExamCandidatesUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    private final UUID schedule1 = UUID.randomUUID();
    private final UUID schedule2 = UUID.randomUUID();
    private final UUID room1 = UUID.randomUUID();
    private final UUID room2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new AutoFillExamCandidatesUseCase(
            examRepository, examCandidateRepository, examScheduleRepository,
            examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });
    }

    @Test
    void should_distribute_candidates_round_robin_in_schedule_order() {
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(
            schedule(schedule2, room2, ExamScheduleStatus.PUBLISHED, OffsetDateTime.parse("2026-01-02T09:00:00Z")),
            schedule(schedule1, room1, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-01T09:00:00Z"))
        ));
        when(examScheduleRepository.findByIdForUpdate(schedule1)).thenReturn(Optional.of(
            schedule(schedule1, room1, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-01T09:00:00Z"))));
        when(examScheduleRepository.findByIdForUpdate(schedule2)).thenReturn(Optional.of(
            schedule(schedule2, room2, ExamScheduleStatus.PUBLISHED, OffsetDateTime.parse("2026-01-02T09:00:00Z"))));
        var c1 = candidate();
        var c2 = candidate();
        var c3 = candidate();
        when(examCandidateRepository.findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(examId))
            .thenReturn(List.of(c1, c2, c3));

        var result = useCase.execute(new AutoFillExamCandidatesCommand(examId, null));

        // Ca xếp theo (startDate, id): schedule1 trước, schedule2 sau.
        // Chia đều round-robin: c1 -> schedule1, c2 -> schedule2, c3 -> schedule1.
        assertThat(result).hasSize(3);
        assertThat(c1.getScheduleId()).isEqualTo(schedule1);
        assertThat(c2.getScheduleId()).isEqualTo(schedule2);
        assertThat(c3.getScheduleId()).isEqualTo(schedule1);
    }

    @Test
    void should_lock_all_schedules_before_touching_candidates() {
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(
            schedule(schedule1, room1, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-01T09:00:00Z")),
            schedule(schedule2, room2, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-02T09:00:00Z"))
        ));
        when(examScheduleRepository.findByIdForUpdate(schedule1)).thenReturn(Optional.of(
            schedule(schedule1, room1, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-01T09:00:00Z"))));
        when(examScheduleRepository.findByIdForUpdate(schedule2)).thenReturn(Optional.of(
            schedule(schedule2, room2, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-02T09:00:00Z"))));
        when(examCandidateRepository.findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(examId))
            .thenReturn(List.of(candidate()));

        useCase.execute(new AutoFillExamCandidatesCommand(examId, null));

        // All schedules must be locked BEFORE candidates are loaded/saved.
        InOrder inOrder = inOrder(examScheduleRepository, examCandidateRepository);
        inOrder.verify(examScheduleRepository).findByIdForUpdate(schedule1);
        inOrder.verify(examScheduleRepository).findByIdForUpdate(schedule2);
        inOrder.verify(examCandidateRepository).findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(examId);
        inOrder.verify(examCandidateRepository).saveAll(anyCollection());
    }

    @Test
    void should_restrict_to_given_schedule_ids() {
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(
            schedule(schedule1, room1, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-01T09:00:00Z")),
            schedule(schedule2, room2, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-02T09:00:00Z"))
        ));
        when(examScheduleRepository.findByIdForUpdate(schedule2)).thenReturn(Optional.of(
            schedule(schedule2, room2, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-02T09:00:00Z"))));
        var c1 = candidate();
        when(examCandidateRepository.findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(examId))
            .thenReturn(List.of(c1));

        var result = useCase.execute(new AutoFillExamCandidatesCommand(examId, List.of(schedule2)));

        assertThat(result).hasSize(1);
        assertThat(c1.getScheduleId()).isEqualTo(schedule2);
        verify(examScheduleRepository, never()).findByIdForUpdate(schedule1);
    }

    @Test
    void should_return_empty_when_no_unassigned_candidates() {
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(
            schedule(schedule1, room1, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-01T09:00:00Z"))
        ));
        when(examScheduleRepository.findByIdForUpdate(schedule1)).thenReturn(Optional.of(
            schedule(schedule1, room1, ExamScheduleStatus.DRAFT, OffsetDateTime.parse("2026-01-01T09:00:00Z"))));
        when(examCandidateRepository.findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(examId))
            .thenReturn(List.of());

        var result = useCase.execute(new AutoFillExamCandidatesCommand(examId, null));

        assertThat(result).isEmpty();
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_return_empty_when_no_target_schedules() {
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(
            schedule(schedule1, room1, ExamScheduleStatus.COMPLETED, OffsetDateTime.parse("2026-01-01T09:00:00Z"))
        ));

        var result = useCase.execute(new AutoFillExamCandidatesCommand(examId, null));

        assertThat(result).isEmpty();
        verify(examScheduleRepository, never()).findByIdForUpdate(schedule1);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    private ExamCandidate candidate() {
        var c = new ExamCandidate();
        c.setId(UUID.randomUUID());
        c.setExamId(examId);
        c.setStudentId(UUID.randomUUID());
        c.setStatus(ExamCandidateStatus.ASSIGNED);
        c.setAssignedAt(OffsetDateTime.now());
        return c;
    }

    private ExamSchedule schedule(UUID id, UUID roomId, ExamScheduleStatus status, OffsetDateTime start) {
        var s = new ExamSchedule();
        s.setId(id);
        s.setExamId(examId);
        s.setSchoolRoomId(roomId);
        s.setStatus(status);
        s.setStartDate(start);
        return s;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }
}
