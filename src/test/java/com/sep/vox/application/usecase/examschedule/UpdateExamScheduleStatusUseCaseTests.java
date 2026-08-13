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
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.UpdateExamScheduleStatusCommand;
import com.sep.vox.application.port.input.service.ExamScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.examschedule.UpdateExamScheduleStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class UpdateExamScheduleStatusUseCaseTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamCandidateRepository examCandidateRepository;
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
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateExamScheduleStatusUseCase(
            examRepository, examScheduleRepository, examScheduleProctorRepository,
            // Validator thật trên proctor repo đã mock: mặc định "giáo viên rảnh", test dời ca bên
            // dưới bật cờ trùng lịch lên để kiểm tra luật.
            new ExamScheduleProctorConflictValidator(examScheduleProctorRepository), examCandidateRepository,
            examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.SCHEDULED)));
        when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examCandidateRepository.findByScheduleId(any(UUID.class))).thenReturn(List.of());
        when(examScheduleProctorRepository.findByScheduleId(any(UUID.class))).thenReturn(List.of());
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
    void should_publish_when_every_candidate_has_paper() {
        var schedule = schedule(ExamScheduleStatus.DRAFT);
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(1L);
        when(examCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidateWithPaper()));

        var result = useCase.execute(command("PUBLISH", null));

        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
        assertThat(result.status()).isEqualTo(ExamScheduleStatus.PUBLISHED.name());
    }

    /**
     * Ca công bố mà thí sinh chưa có đề thì tới lúc vào phòng thi mới nổ
     * (VerifyExamScheduleOtpUseCase) — phải chặn ngay từ lúc công bố.
     */
    @Test
    void should_reject_publish_when_candidate_has_no_paper() {
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(1L);
        when(examCandidateRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(candidateWithPaper(), candidate()));

        assertThatThrownBy(() -> useCase.execute(command("PUBLISH", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Còn 1 học sinh chưa được gán đề");
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_publish_when_schedule_has_no_candidate() {
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(1L);
        when(examCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(command("PUBLISH", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ca thi chưa có thí sinh nào");
        verify(examScheduleRepository, never()).save(any());
    }

    /** Thí sinh đã huỷ/miễn thi không vào phòng nên không cần đề, không được chặn cả ca. */
    @Test
    void should_ignore_cancelled_candidate_without_paper_when_publishing() {
        var schedule = schedule(ExamScheduleStatus.DRAFT);
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(examScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(1L);
        when(examCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(
            candidateWithPaper(),
            candidate(null, ExamCandidateStatus.CANCELLED)));

        useCase.execute(command("PUBLISH", null));

        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.PUBLISHED);
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
        var targetId = givenTarget(ExamScheduleStatus.DRAFT);
        var schedule = givenSource(ExamScheduleStatus.PUBLISHED);

        var result = useCase.execute(command("MOVE", targetId));

        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.MOVED);
        assertThat(schedule.getMovedToScheduleId()).isEqualTo(targetId);
        assertThat(result.movedToScheduleId()).isEqualTo(targetId);
    }

    @Test
    void should_move_candidates_to_target_schedule() {
        var targetId = givenTarget(ExamScheduleStatus.DRAFT);
        givenSource(ExamScheduleStatus.PUBLISHED);
        var first = candidate();
        var second = candidate();
        when(examCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(first, second));

        useCase.execute(command("MOVE", targetId));

        assertThat(first.getScheduleId()).isEqualTo(targetId);
        assertThat(second.getScheduleId()).isEqualTo(targetId);
        verify(examCandidateRepository).saveAll(List.of(first, second));
    }

    @Test
    void should_move_proctors_to_target_schedule() {
        var targetId = givenTarget(ExamScheduleStatus.DRAFT);
        givenSource(ExamScheduleStatus.PUBLISHED);
        var teacherId = UUID.randomUUID();
        var proctorId = UUID.randomUUID();
        when(examScheduleProctorRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(new ExamScheduleProctor(proctorId, scheduleId, teacherId)));
        when(examScheduleProctorRepository.existsByScheduleIdAndTeacherId(targetId, teacherId)).thenReturn(false);

        useCase.execute(command("MOVE", targetId));

        var saved = ArgumentCaptor.forClass(ExamScheduleProctor.class);
        verify(examScheduleProctorRepository).save(saved.capture());
        assertThat(saved.getValue().getScheduleId()).isEqualTo(targetId);
        assertThat(saved.getValue().getTeacherId()).isEqualTo(teacherId);
        verify(examScheduleProctorRepository).deleteById(proctorId);
    }

    @Test
    void should_not_duplicate_proctor_already_assigned_to_target() {
        var targetId = givenTarget(ExamScheduleStatus.DRAFT);
        givenSource(ExamScheduleStatus.PUBLISHED);
        var teacherId = UUID.randomUUID();
        var proctorId = UUID.randomUUID();
        when(examScheduleProctorRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(new ExamScheduleProctor(proctorId, scheduleId, teacherId)));
        when(examScheduleProctorRepository.existsByScheduleIdAndTeacherId(targetId, teacherId)).thenReturn(true);

        useCase.execute(command("MOVE", targetId));

        verify(examScheduleProctorRepository, never()).save(any());
        verify(examScheduleProctorRepository).deleteById(proctorId);
    }

    /**
     * Ca đích có khung giờ khác ca nguồn, nên giám thị đi theo có thể đâm vào một ca thứ ba — dời ca
     * không được phép âm thầm tạo ra trùng lịch.
     */
    @Test
    void should_reject_move_when_proctor_busy_in_target_window() {
        var targetId = givenTarget(ExamScheduleStatus.DRAFT);
        givenSource(ExamScheduleStatus.PUBLISHED);
        var teacherId = UUID.randomUUID();
        when(examScheduleProctorRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(new ExamScheduleProctor(UUID.randomUUID(), scheduleId, teacherId)));
        when(examScheduleProctorRepository.existsByScheduleIdAndTeacherId(targetId, teacherId)).thenReturn(false);
        when(examScheduleProctorRepository.existsOverlappingAssignment(
            eq(teacherId), any(Instant.class), any(Instant.class), eq(scheduleId))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command("MOVE", targetId)))
            .isInstanceOf(DuplicatedException.class);
        verify(examScheduleProctorRepository, never()).save(any());
    }

    @Test
    void should_reject_move_to_the_same_schedule() {
        givenSource(ExamScheduleStatus.PUBLISHED);

        assertThatThrownBy(() -> useCase.execute(command("MOVE", scheduleId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_move_when_exam_already_started() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.IN_PROGRESS)));
        var targetId = givenTarget(ExamScheduleStatus.DRAFT);
        givenSource(ExamScheduleStatus.PUBLISHED);

        assertThatThrownBy(() -> useCase.execute(command("MOVE", targetId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examScheduleRepository, never()).save(any());
    }

    @Test
    void should_allow_complete_when_exam_in_progress() {
        // Đánh dấu ca hoàn thành là thao tác vận hành lúc đang thi -- không nằm trong phạm vi khoá.
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.IN_PROGRESS)));
        var schedule = givenSource(ExamScheduleStatus.PUBLISHED);

        useCase.execute(command("COMPLETE", null));

        assertThat(schedule.getStatus()).isEqualTo(ExamScheduleStatus.COMPLETED);
    }

    private UUID givenTarget(ExamScheduleStatus status) {
        var targetId = UUID.randomUUID();
        var target = schedule(status);
        target.setId(targetId);
        when(examScheduleRepository.findByIdForUpdate(targetId)).thenReturn(Optional.of(target));
        return targetId;
    }

    private ExamSchedule givenSource(ExamScheduleStatus status) {
        var schedule = schedule(status);
        when(examScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        return schedule;
    }

    private ExamCandidate candidate() {
        return candidate(null, ExamCandidateStatus.ASSIGNED);
    }

    private ExamCandidate candidateWithPaper() {
        return candidate(UUID.randomUUID(), ExamCandidateStatus.ASSIGNED);
    }

    private ExamCandidate candidate(UUID assignedPaperId, ExamCandidateStatus status) {
        var candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(examId);
        candidate.setStudentId(UUID.randomUUID());
        candidate.setScheduleId(scheduleId);
        candidate.setAssignedPaperId(assignedPaperId);
        candidate.setStatus(status);
        return candidate;
    }

    private UpdateExamScheduleStatusCommand command(String action, UUID targetScheduleId) {
        return new UpdateExamScheduleStatusCommand(examId, scheduleId, action, null, targetScheduleId);
    }

    private Exam exam(ExamStatus status) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setStatus(status);
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
