package com.sep.vox.application.usecase.examschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.CreateExamScheduleCommand;
import com.sep.vox.application.port.input.usecase.examschedule.CreateExamScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class CreateExamScheduleUseCaseTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private SchoolRoomRepository schoolRoomRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private CreateExamScheduleUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final OffsetDateTime start = OffsetDateTime.parse("2026-07-10T08:00:00+07:00");
    private final OffsetDateTime end = OffsetDateTime.parse("2026-07-10T10:00:00+07:00");

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        schoolRoomRepository = mock(SchoolRoomRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateExamScheduleUseCase(
            examRepository, examScheduleRepository, schoolRoomRepository, examMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
    }

    @Test
    void should_create_draft_schedule_on_happy_path() {
        var room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(examScheduleRepository.existsOverlapping(roomId, start, end, null)).thenReturn(false);
        when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(new CreateExamScheduleCommand(examId, roomId, start, end));

        assertThat(result.status()).isEqualTo(ExamScheduleStatus.DRAFT.name());
        assertThat(result.schoolRoomId()).isEqualTo(roomId);
        verify(examScheduleRepository).save(any(ExamSchedule.class));
    }

    @Test
    void should_reject_when_end_not_after_start() {
        assertThatThrownBy(() -> useCase.execute(new CreateExamScheduleCommand(examId, roomId, end, start)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_when_room_belongs_to_other_school() {
        var room = room(UUID.randomUUID());
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> useCase.execute(new CreateExamScheduleCommand(examId, roomId, start, end)))
            .isInstanceOf(ForbiddenException.class);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_when_overlapping() {
        var room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(examScheduleRepository.existsOverlapping(eq(roomId), eq(start), eq(end), eq(null))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new CreateExamScheduleCommand(examId, roomId, start, end)))
            .isInstanceOf(DuplicatedException.class);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_when_window_shorter_than_exam_time_duration() {
        var exam = exam();
        // Ca thi dài 2 tiếng (start..end) nhưng thời gian làm bài là 3 tiếng.
        exam.setExamTimeDurationSecond(3 * 3600);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> useCase.execute(new CreateExamScheduleCommand(examId, roomId, start, end)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("thời gian làm bài");
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_accept_when_window_equals_exam_time_duration() {
        var exam = exam();
        exam.setExamTimeDurationSecond(2 * 3600);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(examScheduleRepository.existsOverlapping(roomId, start, end, null)).thenReturn(false);
        when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(new CreateExamScheduleCommand(examId, roomId, start, end));

        assertThat(result.status()).isEqualTo(ExamScheduleStatus.DRAFT.name());
        verify(examScheduleRepository).save(any(ExamSchedule.class));
    }

    @Test
    void should_reject_when_schedule_starts_before_exam_open() {
        var exam = exam();
        exam.setOpenAt(start.plusMinutes(1));
        exam.setCloseAt(end);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> useCase.execute(new CreateExamScheduleCommand(examId, roomId, start, end)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mở và đóng");
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_when_schedule_ends_after_exam_close() {
        var exam = exam();
        exam.setOpenAt(start);
        exam.setCloseAt(end.minusMinutes(1));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> useCase.execute(new CreateExamScheduleCommand(examId, roomId, start, end)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mở và đóng");
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_accept_when_schedule_exactly_fills_exam_window() {
        var exam = exam();
        exam.setOpenAt(start);
        exam.setCloseAt(end);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        var room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(examScheduleRepository.existsOverlapping(roomId, start, end, null)).thenReturn(false);
        when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(new CreateExamScheduleCommand(examId, roomId, start, end));

        assertThat(result.status()).isEqualTo(ExamScheduleStatus.DRAFT.name());
        verify(examScheduleRepository).save(any(ExamSchedule.class));
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }

    private SchoolRoom room(UUID ownerSchoolId) {
        var room = mock(SchoolRoom.class);
        when(room.getSchoolId()).thenReturn(ownerSchoolId);
        return room;
    }
}
