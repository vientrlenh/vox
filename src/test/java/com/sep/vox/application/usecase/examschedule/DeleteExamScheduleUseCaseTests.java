package com.sep.vox.application.usecase.examschedule;

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

import com.sep.vox.application.port.input.command.DeleteExamScheduleCommand;
import com.sep.vox.application.port.input.usecase.examschedule.DeleteExamScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class DeleteExamScheduleUseCaseTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private DeleteExamScheduleUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new DeleteExamScheduleUseCase(
            examRepository, examScheduleRepository, examCandidateRepository, examScheduleProctorRepository,
            examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.SCHEDULED)));
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule()));
    }

    @Test
    void should_reject_delete_when_has_candidates() {
        when(examCandidateRepository.countByScheduleId(scheduleId)).thenReturn(3L);

        assertThatThrownBy(() -> useCase.execute(new DeleteExamScheduleCommand(examId, scheduleId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_delete_when_has_proctors() {
        when(examCandidateRepository.countByScheduleId(scheduleId)).thenReturn(0L);
        when(examScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(new DeleteExamScheduleCommand(examId, scheduleId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("giám thị");
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_delete_when_exam_already_started() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.IN_PROGRESS)));
        when(examCandidateRepository.countByScheduleId(scheduleId)).thenReturn(0L);
        when(examScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(new DeleteExamScheduleCommand(examId, scheduleId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_soft_delete_when_no_candidates() {
        var schedule = schedule();
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(examCandidateRepository.countByScheduleId(scheduleId)).thenReturn(0L);
        when(examScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(0L);
        when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(new DeleteExamScheduleCommand(examId, scheduleId));

        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.DELETED);
        verify(examScheduleRepository).save(schedule);
    }

    private Exam exam(ExamStatus status) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setStatus(status);
        return exam;
    }

    private ExamSchedule schedule() {
        var schedule = new ExamSchedule();
        schedule.setId(scheduleId);
        schedule.setExamId(examId);
        schedule.setSchoolRoomId(UUID.randomUUID());
        schedule.setStartDate(Instant.parse("2026-07-10T08:00:00+07:00"));
        schedule.setEndDate(Instant.parse("2026-07-10T10:00:00+07:00"));
        schedule.setStatus(ExamScheduleStatus.DRAFT);
        return schedule;
    }
}
