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

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.FullName;

/**
 * The rule "a student cannot sit in two rooms at the same time".
 *
 * <p>Unlike the proctor validator (which only delegates one row down to the repository), this class holds real logic:
 * it excludes each candidate's current schedule itself, skips people who were exempted/cancelled, and builds a message that names people.
 * So it has its own test instead of only being tested indirectly through a use case.
 */
class AssessmentScheduleCandidateConflictValidatorTests {

    private AssessmentCandidateRepository assessmentCandidateRepository;
    private UserRepository userRepository;
    private AssessmentScheduleCandidateConflictValidator validator;

    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID busyScheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        userRepository = mock(UserRepository.class);
        validator = new AssessmentScheduleCandidateConflictValidator(assessmentCandidateRepository, userRepository);

        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
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
        AssessmentCandidate candidate = candidate(busyScheduleId);
        givenConflicts(new StudentScheduleConflict(candidate.getStudentId(), busyScheduleId, start, end));

        assertThatCode(() -> validator.requireCandidatesFree(List.of(candidate), start, end))
            .doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_a_student_has_an_overlapping_schedule_in_another_assessment() {
        AssessmentCandidate candidate = candidate(null);
        givenConflicts(new StudentScheduleConflict(candidate.getStudentId(), busyScheduleId, start, end));
        givenNames(candidate.getStudentId(), "Nguyễn Văn An");

        assertThatThrownBy(() -> validator.requireCandidatesFree(List.of(candidate), start, end))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("Nguyễn Văn An")
            .hasMessageContaining("in this time range");
    }

    @Test
    void should_ignore_each_candidate_own_current_schedule_when_checking_a_batch() {
        // Two candidates are in TWO different current sessions, and both sessions overlap the
        // target time window. Both conflict rows are their own rows, so nobody is blocked.
        AssessmentCandidate first = candidate(UUID.randomUUID());
        AssessmentCandidate second = candidate(UUID.randomUUID());
        givenConflicts(
            new StudentScheduleConflict(first.getStudentId(), first.getScheduleId(), start, end),
            new StudentScheduleConflict(second.getStudentId(), second.getScheduleId(), start, end));

        assertThatCode(() -> validator.requireCandidatesFree(List.of(first, second), start, end))
            .doesNotThrowAnyException();
    }

    @Test
    void should_check_conflicts_once_for_the_whole_batch() {
        validator.requireCandidatesFree(List.of(candidate(null), candidate(null), candidate(null)), start, end);

        verify(assessmentCandidateRepository, times(1))
            .findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    @Test
    void should_ignore_exempted_and_cancelled_candidates() {
        AssessmentCandidate exempted = candidate(null);
        exempted.setStatus(AssessmentCandidateStatus.EXEMPTED);
        AssessmentCandidate cancelled = candidate(null);
        cancelled.setStatus(AssessmentCandidateStatus.CANCELLED);

        validator.requireCandidatesFree(List.of(exempted, cancelled), start, end);

        verify(assessmentCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    @Test
    void should_still_check_blocked_candidates() {
        AssessmentCandidate blocked = candidate(null);
        blocked.setBlockedAt(start);
        givenConflicts(new StudentScheduleConflict(blocked.getStudentId(), busyScheduleId, start, end));

        assertThatThrownBy(() -> validator.requireCandidatesFree(List.of(blocked), start, end))
            .isInstanceOf(ResourceDuplicatedException.class);
    }

    @Test
    void should_skip_the_query_when_the_window_is_not_set() {
        validator.requireCandidatesFree(List.of(candidate(null)), null, null);

        verify(assessmentCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    @Test
    void should_name_the_conflicting_students_in_the_message() {
        AssessmentCandidate first = candidate(null);
        AssessmentCandidate second = candidate(null);
        givenConflicts(
            new StudentScheduleConflict(first.getStudentId(), busyScheduleId, start, end),
            new StudentScheduleConflict(second.getStudentId(), busyScheduleId, start, end));
        givenNames(first.getStudentId(), "Trần Thị Bình", second.getStudentId(), "Lê Văn Cường");

        assertThatThrownBy(() -> validator.requireCandidatesFree(List.of(first, second), start, end))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("2 students")
            .hasMessageContaining("Trần Thị Bình")
            .hasMessageContaining("Lê Văn Cường");
    }

    @Test
    void should_summarise_when_more_than_five_students_conflict() {
        List<AssessmentCandidate> candidates = new ArrayList<AssessmentCandidate>();
        List<AssessmentCandidateRepository.StudentScheduleConflict> conflicts = new ArrayList<StudentScheduleConflict>();
        for (int i = 0; i < 8; i++) {
            AssessmentCandidate candidate = candidate(null);
            candidates.add(candidate);
            conflicts.add(new StudentScheduleConflict(candidate.getStudentId(), busyScheduleId, start, end));
        }
        givenConflicts(conflicts.toArray(StudentScheduleConflict[]::new));

        assertThatThrownBy(() -> validator.requireCandidatesFree(candidates, start, end))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("8 students")
            .hasMessageContaining("and 3 other students");
    }

    @Test
    void should_load_the_candidates_of_the_schedule_when_checking_a_new_window() {
        AssessmentCandidate candidate = candidate(scheduleId);
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidate));
        givenConflicts(new StudentScheduleConflict(candidate.getStudentId(), busyScheduleId, start, end));
        givenNames(candidate.getStudentId(), "Phạm Thị Dung");

        assertThatThrownBy(() -> validator.requireCandidatesFreeForNewWindow(scheduleId, start, end))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("Phạm Thị Dung");
    }

    @Test
    void should_ignore_the_schedule_being_moved_when_checking_a_new_window() {
        // A candidate of the very session whose time is being changed is of course "busy" in that
        // session -- it must not block itself.
        AssessmentCandidate candidate = candidate(scheduleId);
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidate));
        givenConflicts(new StudentScheduleConflict(candidate.getStudentId(), scheduleId, start, end));

        assertThatCode(() -> validator.requireCandidatesFreeForNewWindow(scheduleId, start, end))
            .doesNotThrowAnyException();
    }

    private void givenConflicts(StudentScheduleConflict... conflicts) {
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
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

    private AssessmentCandidate candidate(UUID currentScheduleId) {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(UUID.randomUUID());
        candidate.setStudentId(UUID.randomUUID());
        candidate.setScheduleId(currentScheduleId);
        candidate.setStatus(AssessmentCandidateStatus.ASSIGNED);
        return candidate;
    }
}
