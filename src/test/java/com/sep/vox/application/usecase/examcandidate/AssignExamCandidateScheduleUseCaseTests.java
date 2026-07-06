package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ConflictException;
import com.sep.vox.application.port.input.command.AssignExamCandidateScheduleCommand;
import com.sep.vox.application.port.input.usecase.examcandidate.AssignExamCandidateScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AssignExamCandidateScheduleUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamScheduleRepository examScheduleRepository;
    private SchoolRoomRepository schoolRoomRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AssignExamCandidateScheduleUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        schoolRoomRepository = mock(SchoolRoomRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new AssignExamCandidateScheduleUseCase(
            examRepository, examCandidateRepository, examScheduleRepository, schoolRoomRepository,
            examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(examCandidateRepository.save(any(ExamCandidate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void should_assign_candidate_to_schedule_when_capacity_available() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room(30)));
        when(examCandidateRepository.countByScheduleId(scheduleId)).thenReturn(5L);

        var result = useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId));

        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(examCandidateRepository).save(any(ExamCandidate.class));
    }

    @Test
    void should_reject_when_schedule_is_full() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.PUBLISHED)));
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room(5)));
        when(examCandidateRepository.countByScheduleId(scheduleId)).thenReturn(5L);

        assertThatThrownBy(() -> useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId)))
            .isInstanceOf(ConflictException.class);
        verify(examCandidateRepository, never()).save(any());
    }

    @Test
    void should_reject_when_schedule_status_not_assignable() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.COMPLETED)));

        assertThatThrownBy(() -> useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId)))
            .isInstanceOf(ConflictException.class);
        verify(examCandidateRepository, never()).save(any());
    }

    @Test
    void should_allow_unassign_even_when_already_unassigned() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));

        var result = useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, null));

        assertThat(result.scheduleId()).isNull();
        verify(examCandidateRepository).save(any(ExamCandidate.class));
        verify(examScheduleRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void should_short_circuit_when_reassigning_to_same_full_schedule() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(scheduleId)));

        var result = useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId));

        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(examScheduleRepository, never()).findByIdForUpdate(any());
        verify(examCandidateRepository, never()).save(any());
        verify(examCandidateRepository, never()).countByScheduleId(eq(scheduleId));
    }

    private ExamCandidate candidate(UUID currentScheduleId) {
        var c = new ExamCandidate();
        c.setId(candidateId);
        c.setExamId(examId);
        c.setStudentId(UUID.randomUUID());
        c.setScheduleId(currentScheduleId);
        c.setStatus(ExamCandidateStatus.ASSIGNED);
        return c;
    }

    private ExamSchedule schedule(ExamScheduleStatus status) {
        var s = new ExamSchedule();
        s.setId(scheduleId);
        s.setExamId(examId);
        s.setSchoolRoomId(roomId);
        s.setStatus(status);
        return s;
    }

    private SchoolRoom room(Integer capacity) {
        var room = new SchoolRoom();
        room.setId(roomId);
        room.setSchoolId(schoolId);
        room.setCapacity(capacity);
        return room;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }
}
