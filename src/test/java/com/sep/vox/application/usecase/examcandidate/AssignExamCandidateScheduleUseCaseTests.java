package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
import com.sep.vox.application.port.input.command.AssignExamCandidateScheduleCommand;
import com.sep.vox.application.port.input.service.ExamPaperAutoAssigner;
import com.sep.vox.application.port.input.service.ExamScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.ExamScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.examcandidate.AssignExamCandidateScheduleUseCase;
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
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository.StudentScheduleConflict;

class AssignExamCandidateScheduleUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamScheduleRepository examScheduleRepository;
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
    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        // Access service thật trên các repository đã mock: luật phân quyền vẫn được test đúng như
        // trước khi nó được tách ra khỏi use case.
        useCase = new AssignExamCandidateScheduleUseCase(
            examRepository, examCandidateRepository, examScheduleRepository,
            new ExamPaperAutoAssigner(mock(ExamPaperRepository.class), examCandidateRepository),
            new ExamScheduleManageAccessService(
                examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort),
            // Validator thật chạy trên candidate repository đã mock: luật "không xếp học sinh vào hai
            // ca trùng giờ" được test qua chính use case thay vì phải tin một mock trả sẵn.
            new ExamScheduleCandidateConflictValidator(examCandidateRepository, mock(UserRepository.class)));

        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(examCandidateRepository.save(any(ExamCandidate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void should_assign_candidate_to_schedule() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));

        var result = useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId));

        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(examCandidateRepository).save(any(ExamCandidate.class));
    }

    @Test
    void should_reject_when_schedule_status_not_assignable() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(ExamScheduleStatus.COMPLETED)));

        assertThatThrownBy(() -> useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId)))
            .isInstanceOf(IllegalStateException.class);
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
    void should_short_circuit_when_reassigning_to_same_schedule() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(scheduleId)));

        var result = useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId));

        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(examScheduleRepository, never()).findByIdForUpdate(any());
        verify(examCandidateRepository, never()).save(any());
        verify(examCandidateRepository, never()).countByScheduleId(eq(scheduleId));
    }

    @Test
    void should_reject_when_student_already_has_an_overlapping_schedule() {
        var candidate = candidate(null);
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(examScheduleRepository.findByIdForUpdate(scheduleId))
            .thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                candidate.getStudentId(), UUID.randomUUID(), start, end)));

        assertThatThrownBy(() -> useCase.execute(
                new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId)))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("khoảng thời gian này");
        verify(examCandidateRepository, never()).save(any());
    }

    @Test
    void should_ignore_the_student_current_schedule_when_checking_conflicts() {
        // Đổi ca trong cùng một kỳ thi: ca cũ chồng giờ ca mới, nhưng xếp sang ca mới là THAY THẾ
        // chỗ cũ nên không phải trùng giờ.
        var currentScheduleId = UUID.randomUUID();
        var candidate = candidate(currentScheduleId);
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(examScheduleRepository.findByIdForUpdate(scheduleId))
            .thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));
        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                candidate.getStudentId(), currentScheduleId, start, end)));

        var result = useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId));

        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(examCandidateRepository).save(any(ExamCandidate.class));
    }

    @Test
    void should_check_conflicts_across_all_exams_of_the_student() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        when(examScheduleRepository.findByIdForUpdate(scheduleId))
            .thenReturn(Optional.of(schedule(ExamScheduleStatus.DRAFT)));

        useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId));

        // Không truyền examId: phép soát phải quét toàn trường, không giới hạn trong kỳ thi này.
        verify(examCandidateRepository).findConflictsForStudents(anyCollection(), eq(start), eq(end), eq(null));
    }

    @Test
    void should_skip_the_conflict_check_when_the_schedule_has_no_window() {
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        var undated = schedule(ExamScheduleStatus.DRAFT);
        undated.setStartDate(null);
        undated.setEndDate(null);
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(undated));

        useCase.execute(new AssignExamCandidateScheduleCommand(examId, candidateId, scheduleId));

        verify(examCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
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
        // Có khung giờ thì luật chống trùng lịch mới chạy — thiếu giờ là validator bỏ qua.
        s.setStartDate(start);
        s.setEndDate(end);
        return s;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }
}
