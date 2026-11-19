package com.sep.vox.application.usecase.examschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.UpdateExamScheduleCommand;
import com.sep.vox.application.port.input.service.ExamScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.ExamScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.ExamScheduleRoomValidator;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.examschedule.UpdateExamScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class UpdateExamScheduleUseCaseTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private SchoolRoomRepository schoolRoomRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private SubscriptionPeriodGuardService subscriptionPeriodGuardService;
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
        examCandidateRepository = mock(ExamCandidateRepository.class);
        subscriptionPeriodGuardService = mock(SubscriptionPeriodGuardService.class);
        useCase = new UpdateExamScheduleUseCase(
            examRepository, examScheduleRepository,
            new ExamScheduleRoomValidator(schoolRoomRepository, examScheduleRepository),
            new ExamScheduleProctorConflictValidator(examScheduleProctorRepository),
            new ExamScheduleCandidateConflictValidator(examCandidateRepository, mock(UserRepository.class)),
            examMemberRepository,
            schoolUserRepository, userRoleQueryRepository,
            subscriptionPeriodGuardService, userContextPort);

        when(examCandidateRepository.findByScheduleId(any(UUID.class))).thenReturn(List.of());
        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
    }

    @Test
    void should_update_draft_schedule_time() {
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(examScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(newStart), eq(newEnd), any(), eq(userId)))
            .thenReturn(1);

        UUID result = useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd));

        assertThat(result).isEqualTo(scheduleId);
        verify(examScheduleRepository).updateAtomic(eq(scheduleId), eq(null), eq(newStart), eq(newEnd), any(), eq(userId));
    }

    @Test
    void should_reject_when_schedule_not_draft() {
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.PUBLISHED)));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(IllegalStateException.class);
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_reject_when_new_time_overlaps() {
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(DuplicatedException.class);
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    /**
     * An easily missed hole: assign a proctor while the two sessions do not touch, then move their
     * times so that they overlap. Blocking only at assignment time is not enough.
     */
    @Test
    void should_reject_moving_schedule_when_proctor_busy_in_new_window() {
        UUID teacherId = UUID.randomUUID();
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
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

    /**
     * The same hole, along the candidate axis: place a student while there is no overlap, then move
     * the times so they overlap.
     */
    @Test
    void should_reject_moving_schedule_when_student_busy_in_new_window() {
        ExamCandidate candidate = candidateOfSchedule();
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(examCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidate));
        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                candidate.getStudentId(), UUID.randomUUID(), newStart, newEnd)));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("đã có ca thi khác");
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_ignore_candidates_of_the_schedule_being_moved() {
        // A candidate of the very session whose time is being changed is of course "busy" in that
        // session -- it must not block itself.
        ExamCandidate candidate = candidateOfSchedule();
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(examCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidate));
        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                candidate.getStudentId(), scheduleId, newStart, newEnd)));
        when(examScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(newStart), eq(newEnd), any(), eq(userId)))
            .thenReturn(1);

        UUID result = useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd));

        assertThat(result).isEqualTo(scheduleId);
    }

    private ExamCandidate candidateOfSchedule() {
        ExamCandidate candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(examId);
        candidate.setStudentId(UUID.randomUUID());
        candidate.setScheduleId(scheduleId);
        candidate.setStatus(ExamCandidateStatus.ASSIGNED);
        return candidate;
    }

    @Test
    void should_reject_when_atomic_update_affects_no_rows() {
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(examScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(newStart), eq(newEnd), any(), eq(userId)))
            .thenReturn(0);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_reject_when_new_window_shorter_than_exam_time_duration() {
        Exam exam = exam();
        // The new time window is 2 hours but the exam duration is 3 hours.
        exam.setExamTimeDurationSecond(3 * 3600);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("thời gian làm bài");
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_reject_when_new_window_falls_outside_exam_window() {
        Exam exam = exam();
        exam.setOpenAt(start);
        exam.setCloseAt(end);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));

        // newStart/newEnd are the next day, outside the exam's open/close window.
        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mở và đóng");
        verify(examScheduleRepository, never()).updateAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_allow_moving_class_test_schedule_outside_current_exam_window() {
        // For CLASS_TEST, the session is what defines the exam window (openAt/closeAt are written
        // back from the session), so moving the session outside the current window is valid.
        Exam exam = exam();
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(ExamStatus.DRAFT);
        exam.setOpenAt(start);
        exam.setCloseAt(end);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);

        UUID result = useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd));

        assertThat(result).isEqualTo(scheduleId);
        assertThat(exam.getOpenAt()).isEqualTo(newStart);
        assertThat(exam.getCloseAt()).isEqualTo(newEnd);
        verify(examRepository).save(exam);
    }

    /**
     * Editing the session of a class test writes openAt/closeAt back down to the exam, so it is
     * another path that writes the open/close window -- without checking the plan limit here,
     * changing a session's time would be a way around the constraint that Create/UpdateExamUseCase
     * apply.
     */
    @Test
    void should_reject_moving_class_test_schedule_outside_subscription_period() {
        Exam exam = classTestExam();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        doThrow(new IllegalStateException("Thời gian đóng bài phải nằm trong hạn gói dịch vụ của trường"))
            .when(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("hạn gói dịch vụ");
        verify(examScheduleRepository, never()).save(any());
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_check_subscription_period_with_effective_window_of_the_class_test() {
        Exam exam = classTestExam();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);

        useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd));

        verify(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(schoolId, newStart, newEnd);
    }

    /**
     * Kỳ thi tập trung đi đường khác: ca thi đã bị ràng phải nằm TRONG khung mở/đóng của kỳ thi, mà
     * khung đó đã được soi hạn gói lúc tạo/sửa kỳ thi — soi lại ở đây là thừa.
     */
    @Test
    void should_not_check_subscription_period_for_centralized_schedule() {
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleRepository.existsOverlapping(roomId, newStart, newEnd, scheduleId)).thenReturn(false);
        when(examScheduleRepository.updateAtomic(eq(scheduleId), eq(null), eq(newStart), eq(newEnd), any(), eq(userId)))
            .thenReturn(1);

        useCase.execute(new UpdateExamScheduleCommand(scheduleId, null, newStart, newEnd));

        verifyNoInteractions(subscriptionPeriodGuardService);
    }

    private Exam exam() {
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }

    private Exam classTestExam() {
        Exam exam = exam();
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(ExamStatus.DRAFT);
        exam.setOpenAt(start);
        exam.setCloseAt(end);
        return exam;
    }

    private ExamSchedule schedule(ExamScheduleStatus status) {
        ExamSchedule schedule = new ExamSchedule();
        schedule.setId(scheduleId);
        schedule.setExamId(examId);
        schedule.setSchoolRoomId(roomId);
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        schedule.setStatus(status);
        return schedule;
    }
}
