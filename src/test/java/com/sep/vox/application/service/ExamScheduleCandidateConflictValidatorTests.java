package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.service.ExamScheduleCandidateConflictValidator;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.FullName;

/**
 * Luật "một học sinh không ngồi hai phòng cùng lúc".
 *
 * <p>Khác với validator giám thị (chỉ uỷ quyền một dòng xuống repository), lớp này giữ logic thật:
 * tự loại ca hiện tại của từng thí sinh, bỏ qua người đã miễn/huỷ thi, và dựng message nêu tên.
 * Nên nó có test riêng thay vì chỉ được test gián tiếp qua use case.
 */
class ExamScheduleCandidateConflictValidatorTests {

    private ExamCandidateRepository examCandidateRepository;
    private UserRepository userRepository;
    private ExamScheduleCandidateConflictValidator validator;

    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID busyScheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examCandidateRepository = mock(ExamCandidateRepository.class);
        userRepository = mock(UserRepository.class);
        validator = new ExamScheduleCandidateConflictValidator(examCandidateRepository, userRepository);

        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
        when(userRepository.findByIdIn(anyCollection())).thenReturn(List.of());
    }

    @Test
    void should_pass_when_no_student_has_an_overlapping_schedule() {
        assertThatCode(() -> validator.requireCandidatesFree(List.of(candidate(null)), start, end))
            .doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_the_only_overlap_is_the_candidate_own_current_schedule() {
        // The student is currently in busyScheduleId itself; moving them to another session
        // REPLACES the old spot, so a conflict row pointing at the old session is not an overlap.
        ExamCandidate candidate = candidate(busyScheduleId);
        givenConflicts(new StudentScheduleConflict(candidate.getStudentId(), busyScheduleId, start, end));

        assertThatCode(() -> validator.requireCandidatesFree(List.of(candidate), start, end))
            .doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_a_student_has_an_overlapping_schedule_in_another_exam() {
        ExamCandidate candidate = candidate(null);
        givenConflicts(new StudentScheduleConflict(candidate.getStudentId(), busyScheduleId, start, end));
        givenNames(candidate.getStudentId(), "Nguyễn Văn An");

        assertThatThrownBy(() -> validator.requireCandidatesFree(List.of(candidate), start, end))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("Nguyễn Văn An")
            .hasMessageContaining("khoảng thời gian này");
    }

    @Test
    void should_ignore_each_candidate_own_current_schedule_when_checking_a_batch() {
        // Two candidates are in TWO different current sessions, and both sessions overlap the
        // target time window. Both conflict rows are their own rows, so nobody is blocked.
        ExamCandidate first = candidate(UUID.randomUUID());
        ExamCandidate second = candidate(UUID.randomUUID());
        givenConflicts(
            new StudentScheduleConflict(first.getStudentId(), first.getScheduleId(), start, end),
            new StudentScheduleConflict(second.getStudentId(), second.getScheduleId(), start, end));

        assertThatCode(() -> validator.requireCandidatesFree(List.of(first, second), start, end))
            .doesNotThrowAnyException();
    }

    @Test
    void should_check_conflicts_once_for_the_whole_batch() {
        validator.requireCandidatesFree(List.of(candidate(null), candidate(null), candidate(null)), start, end);

        verify(examCandidateRepository, times(1))
            .findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    @Test
    void should_ignore_exempted_and_cancelled_candidates() {
        ExamCandidate exempted = candidate(null);
        exempted.setStatus(ExamCandidateStatus.EXEMPTED);
        ExamCandidate cancelled = candidate(null);
        cancelled.setStatus(ExamCandidateStatus.CANCELLED);

        validator.requireCandidatesFree(List.of(exempted, cancelled), start, end);

        verify(examCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    @Test
    void should_still_check_blocked_candidates() {
        ExamCandidate blocked = candidate(null);
        blocked.setBlockedAt(start);
        givenConflicts(new StudentScheduleConflict(blocked.getStudentId(), busyScheduleId, start, end));

        assertThatThrownBy(() -> validator.requireCandidatesFree(List.of(blocked), start, end))
            .isInstanceOf(DuplicatedException.class);
    }

    @Test
    void should_skip_the_query_when_the_window_is_not_set() {
        validator.requireCandidatesFree(List.of(candidate(null)), null, null);

        verify(examCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    @Test
    void should_name_the_conflicting_students_in_the_message() {
        ExamCandidate first = candidate(null);
        ExamCandidate second = candidate(null);
        givenConflicts(
            new StudentScheduleConflict(first.getStudentId(), busyScheduleId, start, end),
            new StudentScheduleConflict(second.getStudentId(), busyScheduleId, start, end));
        givenNames(first.getStudentId(), "Trần Thị Bình", second.getStudentId(), "Lê Văn Cường");

        assertThatThrownBy(() -> validator.requireCandidatesFree(List.of(first, second), start, end))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("2 học sinh")
            .hasMessageContaining("Trần Thị Bình")
            .hasMessageContaining("Lê Văn Cường");
    }

    @Test
    void should_summarise_when_more_than_five_students_conflict() {
        List<ExamCandidate> candidates = new ArrayList<ExamCandidate>();
        List<ExamCandidateRepository.StudentScheduleConflict> conflicts = new ArrayList<StudentScheduleConflict>();
        for (int i = 0; i < 8; i++) {
            ExamCandidate candidate = candidate(null);
            candidates.add(candidate);
            conflicts.add(new StudentScheduleConflict(candidate.getStudentId(), busyScheduleId, start, end));
        }
        givenConflicts(conflicts.toArray(StudentScheduleConflict[]::new));

        assertThatThrownBy(() -> validator.requireCandidatesFree(candidates, start, end))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("8 học sinh")
            .hasMessageContaining("và 3 học sinh khác");
    }

    @Test
    void should_load_the_candidates_of_the_schedule_when_checking_a_new_window() {
        ExamCandidate candidate = candidate(scheduleId);
        when(examCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidate));
        givenConflicts(new StudentScheduleConflict(candidate.getStudentId(), busyScheduleId, start, end));
        givenNames(candidate.getStudentId(), "Phạm Thị Dung");

        assertThatThrownBy(() -> validator.requireCandidatesFreeForNewWindow(scheduleId, start, end))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("Phạm Thị Dung");
    }

    @Test
    void should_ignore_the_schedule_being_moved_when_checking_a_new_window() {
        // A candidate of the very session whose time is being changed is of course "busy" in that
        // session -- it must not block itself.
        ExamCandidate candidate = candidate(scheduleId);
        when(examCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidate));
        givenConflicts(new StudentScheduleConflict(candidate.getStudentId(), scheduleId, start, end));

        assertThatCode(() -> validator.requireCandidatesFreeForNewWindow(scheduleId, start, end))
            .doesNotThrowAnyException();
    }

    private void givenConflicts(StudentScheduleConflict... conflicts) {
        when(examCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(conflicts));
    }

    private void givenNames(Object... idThenName) {
        List<User> users = new ArrayList<User>();
        for (int i = 0; i < idThenName.length; i += 2) {
            User user = new User();
            user.setId((UUID) idThenName[i]);
            user.setFullName(new FullName((String) idThenName[i + 1]));
            users.add(user);
        }
        when(userRepository.findByIdIn(anyCollection())).thenReturn(users);
    }

    private ExamCandidate candidate(UUID currentScheduleId) {
        ExamCandidate candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(UUID.randomUUID());
        candidate.setStudentId(UUID.randomUUID());
        candidate.setScheduleId(currentScheduleId);
        candidate.setStatus(ExamCandidateStatus.ASSIGNED);
        return candidate;
    }
}
