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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AddExamScheduleProctorCommand;
import com.sep.vox.application.port.input.usecase.examschedule.AddExamScheduleProctorUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AddExamScheduleProctorUseCaseTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AddExamScheduleProctorUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new AddExamScheduleProctorUseCase(
            examRepository, examScheduleRepository, examScheduleProctorRepository, examMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule()));
    }

    @Test
    void should_add_proctor_on_happy_path() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, teacherId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId)).thenReturn(List.of(teacherRole()));
        when(examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, teacherId)).thenReturn(false);
        when(examScheduleProctorRepository.save(any(ExamScheduleProctor.class)))
            .thenAnswer(inv -> {
                ExamScheduleProctor p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

        var result = useCase.execute(new AddExamScheduleProctorCommand(examId, scheduleId, teacherId));

        assertThat(result.teacherId()).isEqualTo(teacherId);
        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(examScheduleProctorRepository).save(any(ExamScheduleProctor.class));
    }

    @Test
    void should_reject_when_teacher_not_in_school() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, teacherId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AddExamScheduleProctorCommand(examId, scheduleId, teacherId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(examScheduleProctorRepository, never()).save(any());
    }

    @Test
    void should_reject_when_user_is_not_teacher() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, teacherId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AddExamScheduleProctorCommand(examId, scheduleId, teacherId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(examScheduleProctorRepository, never()).save(any());
    }

    @Test
    void should_reject_when_proctor_already_assigned() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, teacherId)).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId)).thenReturn(List.of(teacherRole()));
        when(examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, teacherId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AddExamScheduleProctorCommand(examId, scheduleId, teacherId)))
            .isInstanceOf(DuplicatedException.class);
        verify(examScheduleProctorRepository, never()).save(any());
    }

    private UserRoleInfo teacherRole() {
        return new UserRoleInfo(UUID.randomUUID(), teacherId, UUID.randomUUID(), Instant.now(),
            "TEACHER", "Teacher");
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }

    private ExamSchedule schedule() {
        var schedule = new ExamSchedule();
        schedule.setId(scheduleId);
        schedule.setExamId(examId);
        schedule.setSchoolRoomId(UUID.randomUUID());
        schedule.setStatus(ExamScheduleStatus.DRAFT);
        return schedule;
    }
}
