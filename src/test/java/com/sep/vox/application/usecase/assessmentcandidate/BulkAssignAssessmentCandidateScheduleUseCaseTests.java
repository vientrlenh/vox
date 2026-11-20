package com.sep.vox.application.usecase.assessmentcandidate;

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
import com.sep.vox.application.port.input.command.BulkAssignAssessmentCandidateScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.BulkAssignAssessmentCandidateScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class BulkAssignAssessmentCandidateScheduleUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private BulkAssignAssessmentCandidateScheduleUseCase useCase;

    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");
    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID firstCandidateId = UUID.randomUUID();
    private final UUID secondCandidateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        useCase = new BulkAssignAssessmentCandidateScheduleUseCase(
            assessmentRepository, assessmentCandidateRepository, assessmentScheduleRepository,
            new AssessmentPaperAutoAssigner(mock(AssessmentPaperRepository.class), assessmentCandidateRepository),
            new AssessmentScheduleManageAccessService(
                assessmentMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort),
            // Validator thật trên repository đã mock: luật chống trùng lịch được test qua use case.
            new AssessmentScheduleCandidateConflictValidator(assessmentCandidateRepository, userRepository));

        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
        when(userRepository.findByIdIn(anyCollection())).thenReturn(List.of());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.DRAFT)));
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });
    }

    @Test
    void should_assign_multiple_candidates_to_schedule() {
        givenSchedule(AssessmentScheduleStatus.PUBLISHED);
        givenCandidates(candidate(firstCandidateId, null), candidate(secondCandidateId, null));

        List<AssessmentCandidateDto> result = useCase.execute(command(scheduleId));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.scheduleId()).isEqualTo(scheduleId));
        // The session is locked exactly once no matter how many candidates there are.
        verify(assessmentScheduleRepository).findByIdForUpdate(scheduleId);
    }

    @Test
    void should_unassign_multiple_candidates_when_schedule_id_is_null() {
        givenCandidates(candidate(firstCandidateId, scheduleId), candidate(secondCandidateId, scheduleId));

        List<AssessmentCandidateDto> result = useCase.execute(command(null));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.scheduleId()).isNull());
        verify(assessmentScheduleRepository, never()).findByIdForUpdate(any());
    }

    /** Looked up by (id, assessmentId) so a missing row = an unknown id; the whole batch fails instead of placing only part of it. */
    @Test
    void should_reject_when_any_candidate_belongs_to_another_assessment() {
        givenSchedule(AssessmentScheduleStatus.DRAFT);
        givenCandidates(candidate(firstCandidateId, null));

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(NotFoundException.class);
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_schedule_is_not_draft_or_published() {
        givenSchedule(AssessmentScheduleStatus.COMPLETED);

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(IllegalStateException.class);
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_assessment_already_started() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(IllegalStateException.class);
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_do_nothing_when_candidate_list_is_empty() {
        List<AssessmentCandidateDto> result = useCase.execute(new BulkAssignAssessmentCandidateScheduleCommand(assessmentId, List.of(), scheduleId));

        assertThat(result).isEmpty();
        verify(assessmentScheduleRepository, never()).findByIdForUpdate(any());
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_bulk_assign_when_any_student_has_an_overlapping_schedule() {
        givenSchedule(AssessmentScheduleStatus.PUBLISHED);
        AssessmentCandidate first = candidate(firstCandidateId, null);
        AssessmentCandidate second = candidate(secondCandidateId, null);
        givenCandidates(first, second);
        givenConflicts(new StudentScheduleConflict(second.getStudentId(), UUID.randomUUID(), start, end));
        givenName(second.getStudentId(), "Trần Thị Bình");

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("Trần Thị Bình");
        // The whole batch fails: nobody is assigned, not even those with no conflict.
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_ignore_each_candidate_own_current_schedule_when_checking_conflicts() {
        // Two candidates are in TWO different current sessions, and both overlap the target session
        // in time. Moving them to the target session replaces their old spot, so nobody is blocked.
        givenSchedule(AssessmentScheduleStatus.PUBLISHED);
        UUID firstCurrent = UUID.randomUUID();
        UUID secondCurrent = UUID.randomUUID();
        AssessmentCandidate first = candidate(firstCandidateId, firstCurrent);
        AssessmentCandidate second = candidate(secondCandidateId, secondCurrent);
        givenCandidates(first, second);
        givenConflicts(
            new StudentScheduleConflict(first.getStudentId(), firstCurrent, start, end),
            new StudentScheduleConflict(second.getStudentId(), secondCurrent, start, end));

        List<AssessmentCandidateDto> result = useCase.execute(command(scheduleId));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.scheduleId()).isEqualTo(scheduleId));
    }

    @Test
    void should_check_conflicts_once_for_the_whole_batch() {
        givenSchedule(AssessmentScheduleStatus.PUBLISHED);
        givenCandidates(candidate(firstCandidateId, null), candidate(secondCandidateId, null));

        useCase.execute(command(scheduleId));

        verify(assessmentCandidateRepository).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    @Test
    void should_skip_the_conflict_check_when_unassigning() {
        givenCandidates(candidate(firstCandidateId, scheduleId), candidate(secondCandidateId, scheduleId));

        useCase.execute(command(null));

        verify(assessmentCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    private BulkAssignAssessmentCandidateScheduleCommand command(UUID targetScheduleId) {
        return new BulkAssignAssessmentCandidateScheduleCommand(
            assessmentId, List.of(firstCandidateId, secondCandidateId), targetScheduleId);
    }

    private void givenSchedule(AssessmentScheduleStatus status) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(scheduleId);
        schedule.setAssessmentId(assessmentId);
        schedule.setStatus(status);
        // The anti-overlap rule only runs when there is a time window -- without times the
        // validator skips it.
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
    }

    private void givenConflicts(StudentScheduleConflict... conflicts) {
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(conflicts));
    }

    private void givenName(UUID studentId, String fullName) {
        User user = new User();
        user.setId(studentId);
        user.setFullName(new FullName(fullName));
        when(userRepository.findByIdIn(anyCollection())).thenReturn(List.of(user));
    }

    private void givenCandidates(AssessmentCandidate... candidates) {
        when(assessmentCandidateRepository.findByIdInAndAssessmentId(
            List.of(firstCandidateId, secondCandidateId), assessmentId)).thenReturn(List.of(candidates));
    }

    private AssessmentCandidate candidate(UUID id, UUID currentScheduleId) {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(id);
        candidate.setAssessmentId(assessmentId);
        candidate.setStudentId(UUID.randomUUID());
        candidate.setScheduleId(currentScheduleId);
        candidate.setStatus(AssessmentCandidateStatus.ASSIGNED);
        return candidate;
    }

    private Assessment assessment(AssessmentStatus status) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setStatus(status);
        return assessment;
    }
}
