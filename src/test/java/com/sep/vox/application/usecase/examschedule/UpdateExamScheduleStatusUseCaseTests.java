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

import com.sep.vox.application.port.input.command.UpdateExamScheduleStatusCommand;
import com.sep.vox.application.port.input.usecase.examschedule.UpdateExamScheduleStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class UpdateExamScheduleStatusUseCaseTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private UpdateExamScheduleStatusUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateExamScheduleStatusUseCase(
            examRepository, examScheduleRepository, examScheduleProctorRepository, examMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void should_reject_publish_without_proctor() {
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(command("PUBLISH", null)))
            .isInstanceOf(IllegalStateException.class);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_publish_when_at_least_one_proctor() {
        var schedule = schedule(ExamScheduleStatus.DRAFT);
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(1L);

        var result = useCase.execute(command("PUBLISH", null));

        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
        assertThat(result.status()).isEqualTo(ExamScheduleStatus.PUBLISHED.name());
    }

    @Test
    void should_reject_invalid_transition() {
        // COMPLETE requires PUBLISHED; DRAFT should be rejected
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(command("COMPLETE", null)))
            .isInstanceOf(IllegalStateException.class);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_unknown_action() {
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(command("FROBNICATE", null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_move_and_set_moved_to_schedule_id() {
        var targetId = UUID.randomUUID();
        var schedule = schedule(ExamScheduleStatus.PUBLISHED);
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        var target = schedule(ExamScheduleStatus.DRAFT);
        target.setId(targetId);
        when(examScheduleRepository.findById(targetId)).thenReturn(Optional.of(target));

        var result = useCase.execute(command("MOVE", targetId));

        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.MOVED);
        assertThat(schedule.getMovedToScheduleId()).isEqualTo(targetId);
        assertThat(result.movedToScheduleId()).isEqualTo(targetId);
    }

    private UpdateExamScheduleStatusCommand command(String action, UUID targetScheduleId) {
        return new UpdateExamScheduleStatusCommand(examId, scheduleId, action, null, targetScheduleId);
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }

    private ExamSchedule schedule(ExamScheduleStatus status) {
        var schedule = new ExamSchedule();
        schedule.setId(scheduleId);
        schedule.setExamId(examId);
        schedule.setSchoolRoomId(roomId);
        schedule.setStartDate(Instant.parse("2026-07-10T08:00:00+07:00"));
        schedule.setEndDate(Instant.parse("2026-07-10T10:00:00+07:00"));
        schedule.setStatus(status);
        return schedule;
    }
}
