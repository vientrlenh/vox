package com.sep.vox.application.usecase.examschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sep.vox.application.port.input.command.UpdateExamScheduleCommand;
import com.sep.vox.application.port.input.service.ExamScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.ExamScheduleRoomValidator;
import com.sep.vox.application.port.input.usecase.examschedule.UpdateExamScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class UpdateExamScheduleUseCaseTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private SchoolRoomRepository schoolRoomRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private UpdateExamScheduleUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");
    private final Instant newStart = Instant.parse("2026-07-11T08:00:00+07:00");
    private final Instant newEnd = Instant.parse("2026-07-11T10:00:00+07:00");

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        schoolRoomRepository = mock(SchoolRoomRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        // Validator thật chạy trên hai repository đã mock: luật kiểm tra phòng vẫn được test đúng
        // như trước khi nó được tách ra khỏi use case.
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        useCase = new UpdateExamScheduleUseCase(
            examRepository, examScheduleRepository,
            new ExamScheduleRoomValidator(schoolRoomRepository, examScheduleRepository),
            new ExamScheduleProctorConflictValidator(examScheduleProctorRepository),
            examMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
    }

    @Test
    void should_update_draft_schedule_time() {
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(examScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(newStart), eq(newEnd), any(), eq(userId)))
            .thenReturn(1);

        var result = useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd));

        assertThat(result).isEqualTo(scheduleId);
        verify(examScheduleRepository).updateAtomic(eq(scheduleId), eq(null), eq(newStart), eq(newEnd), any(), eq(userId));
    }

    @Test
    void should_reject_when_schedule_not_draft() {
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.PUBLISHED)));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(IllegalStateException.class);
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_reject_when_new_time_overlaps() {
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(DuplicatedException.class);
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    /**
     * Lỗ dễ bỏ sót: gán giám thị lúc hai ca chưa đụng nhau, rồi kéo giờ cho chúng chồng lên. Chặn ở
     * mỗi lúc gán là chưa đủ.
     */
    @Test
    void should_reject_moving_schedule_when_proctor_busy_in_new_window() {
        var teacherId = UUID.randomUUID();
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(examScheduleProctorRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(new ExamScheduleProctor(scheduleId, teacherId)));
        when(examScheduleProctorRepository.existsOverlappingAssignment(teacherId, newStart, newEnd, scheduleId))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("Giám thị");
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_reject_when_atomic_update_affects_no_rows() {
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(examScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(newStart), eq(newEnd), any(), eq(userId)))
            .thenReturn(0);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_reject_when_new_window_shorter_than_exam_time_duration() {
        var exam = exam();
        // Khung giờ mới dài 2 tiếng nhưng thời gian làm bài là 3 tiếng.
        exam.setExamTimeDurationSecond(3 * 3600);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("thời gian làm bài");
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_reject_when_new_window_falls_outside_exam_window() {
        var exam = exam();
        exam.setOpenAt(start);
        exam.setCloseAt(end);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));

        // newStart/newEnd là ngày hôm sau, nằm ngoài khung mở/đóng của kỳ thi.
        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mở và đóng");
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_allow_moving_class_test_schedule_outside_current_exam_window() {
        // Với CLASS_TEST, ca thi là thứ định nghĩa khung giờ kỳ thi (openAt/closeAt được ghi
        // ngược lại từ ca thi), nên dời ca thi ra ngoài khung hiện tại là hợp lệ.
        var exam = exam();
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(ExamStatus.DRAFT);
        exam.setOpenAt(start);
        exam.setCloseAt(end);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);

        var result = useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd));

        assertThat(result).isEqualTo(scheduleId);
        assertThat(exam.getOpenAt()).isEqualTo(newStart);
        assertThat(exam.getCloseAt()).isEqualTo(newEnd);
        verify(examRepository).save(exam);
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
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        schedule.setStatus(status);
        return schedule;
    }
}
