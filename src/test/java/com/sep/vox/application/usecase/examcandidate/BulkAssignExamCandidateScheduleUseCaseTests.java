package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BulkAssignExamCandidateScheduleCommand;
import com.sep.vox.application.port.input.service.ExamPaperAutoAssigner;
import com.sep.vox.application.port.input.service.ExamScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.ExamScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.examcandidate.BulkAssignExamCandidateScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class BulkAssignExamCandidateScheduleUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private BulkAssignExamCandidateScheduleUseCase useCase;

    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");
    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID firstCandidateId = UUID.randomUUID();
    private final UUID secondCandidateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        useCase = new BulkAssignExamCandidateScheduleUseCase(
            examRepository, examCandidateRepository, examScheduleRepository,
            new ExamPaperAutoAssigner(mock(ExamPaperRepository.class), examCandidateRepository),
            new ExamScheduleManageAccessService(
                examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort),
            // Validator thật trên repository đã mock: luật chống trùng lịch được test qua use case.
            new ExamScheduleCandidateConflictValidator(examCandidateRepository, userRepository));

        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
        when(userRepository.findByIdIn(anyCollection())).thenReturn(List.of());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.DRAFT)));
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });
    }

    @Test
    void should_assign_multiple_candidates_to_schedule() {
        givenSchedule(ExamScheduleStatus.PUBLISHED);
        givenCandidates(candidate(firstCandidateId, null), candidate(secondCandidateId, null));

        var result = useCase.execute(command(scheduleId));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.scheduleId()).isEqualTo(scheduleId));
        // Ca chỉ bị khoá đúng một lần dù có bao nhiêu thí sinh.
        verify(examScheduleRepository).findByIdForUpdate(scheduleId);
    }

    @Test
    void should_unassign_multiple_candidates_when_schedule_id_is_null() {
        givenCandidates(candidate(firstCandidateId, scheduleId), candidate(secondCandidateId, scheduleId));

        var result = useCase.execute(command(null));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.scheduleId()).isNull());
        verify(examScheduleRepository, never()).findByIdForUpdate(any());
    }

    /** Tra theo (id, examId) nên thiếu dòng = có id lạ; hỏng cả lượt thay vì xếp một phần. */
    @Test
    void should_reject_when_any_candidate_belongs_to_another_exam() {
        givenSchedule(ExamScheduleStatus.DRAFT);
        givenCandidates(candidate(firstCandidateId, null));

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(NotFoundException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_schedule_is_not_draft_or_published() {
        givenSchedule(ExamScheduleStatus.COMPLETED);

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_exam_already_started() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_do_nothing_when_candidate_list_is_empty() {
        var result = useCase.execute(new BulkAssignExamCandidateScheduleCommand(examId, List.of(), scheduleId));

        assertThat(result).isEmpty();
        verify(examScheduleRepository, never()).findByIdForUpdate(any());
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_bulk_assign_when_any_student_has_an_overlapping_schedule() {
        givenSchedule(ExamScheduleStatus.PUBLISHED);
        var first = candidate(firstCandidateId, null);
        var second = candidate(secondCandidateId, null);
        givenCandidates(first, second);
        givenConflicts(new StudentScheduleConflict(second.getStudentId(), UUID.randomUUID(), start, end));
        givenName(second.getStudentId(), "Trần Thị Bình");

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("Trần Thị Bình");
        // Cả lượt hỏng: không ai được xếp, kể cả người không vướng.
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_ignore_each_candidate_own_current_schedule_when_checking_conflicts() {
        // Hai thí sinh đang ở HAI ca hiện tại khác nhau, cả hai đều chồng giờ ca đích. Xếp sang ca
        // đích là thay thế chỗ cũ nên không ai bị chặn.
        givenSchedule(ExamScheduleStatus.PUBLISHED);
        var firstCurrent = UUID.randomUUID();
        var secondCurrent = UUID.randomUUID();
        var first = candidate(firstCandidateId, firstCurrent);
        var second = candidate(secondCandidateId, secondCurrent);
        givenCandidates(first, second);
        givenConflicts(
            new StudentScheduleConflict(first.getStudentId(), firstCurrent, start, end),
            new StudentScheduleConflict(second.getStudentId(), secondCurrent, start, end));

        var result = useCase.execute(command(scheduleId));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.scheduleId()).isEqualTo(scheduleId));
    }

    @Test
    void should_check_conflicts_once_for_the_whole_batch() {
        givenSchedule(ExamScheduleStatus.PUBLISHED);
        givenCandidates(candidate(firstCandidateId, null), candidate(secondCandidateId, null));

        useCase.execute(command(scheduleId));

        verify(examCandidateRepository).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    @Test
    void should_skip_the_conflict_check_when_unassigning() {
        givenCandidates(candidate(firstCandidateId, scheduleId), candidate(secondCandidateId, scheduleId));

        useCase.execute(command(null));

        verify(examCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    private BulkAssignExamCandidateScheduleCommand command(UUID targetScheduleId) {
        return new BulkAssignExamCandidateScheduleCommand(
            examId, List.of(firstCandidateId, secondCandidateId), targetScheduleId);
    }

    private void givenSchedule(ExamScheduleStatus status) {
        var schedule = new ExamSchedule();
        schedule.setId(scheduleId);
        schedule.setExamId(examId);
        schedule.setStatus(status);
        // Có khung giờ thì luật chống trùng lịch mới chạy — thiếu giờ là validator bỏ qua.
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
    }

    private void givenConflicts(StudentScheduleConflict... conflicts) {
        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(conflicts));
    }

    private void givenName(UUID studentId, String fullName) {
        var user = new User();
        user.setId(studentId);
        user.setFullName(new FullName(fullName));
        when(userRepository.findByIdIn(anyCollection())).thenReturn(List.of(user));
    }

    private void givenCandidates(ExamCandidate... candidates) {
        when(examCandidateRepository.findByIdInAndExamId(
            List.of(firstCandidateId, secondCandidateId), examId)).thenReturn(List.of(candidates));
    }

    private ExamCandidate candidate(UUID id, UUID currentScheduleId) {
        var candidate = new ExamCandidate();
        candidate.setId(id);
        candidate.setExamId(examId);
        candidate.setStudentId(UUID.randomUUID());
        candidate.setScheduleId(currentScheduleId);
        candidate.setStatus(ExamCandidateStatus.ASSIGNED);
        return candidate;
    }

    private Exam exam(ExamStatus status) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setStatus(status);
        return exam;
    }
}
