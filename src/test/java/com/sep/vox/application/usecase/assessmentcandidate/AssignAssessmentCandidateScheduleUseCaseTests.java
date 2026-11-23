package com.sep.vox.application.usecase.assessmentcandidate;

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

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.assessmentcandidate.AssignAssessmentCandidateScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AssignAssessmentCandidateScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository.StudentScheduleConflict;

class AssignAssessmentCandidateScheduleUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AssignAssessmentCandidateScheduleUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        // Access service thật trên các repository đã mock: luật phân quyền vẫn được test đúng như
        // trước khi nó được tách ra khỏi use case.
        useCase = new AssignAssessmentCandidateScheduleUseCase(
            assessmentRepository, assessmentCandidateRepository, assessmentScheduleRepository,
            new AssessmentPaperAutoAssigner(mock(AssessmentPaperRepository.class), assessmentCandidateRepository),
            new AssessmentScheduleManageAccessService(
                assessmentMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort),
            // Validator thật chạy trên candidate repository đã mock: luật "không xếp học sinh vào hai
            // ca trùng giờ" được test qua chính use case thay vì phải tin một mock trả sẵn.
            new AssessmentScheduleCandidateConflictValidator(assessmentCandidateRepository, mock(UserRepository.class)));

        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
        when(assessmentCandidateRepository.save(any(AssessmentCandidate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void should_assign_candidate_to_schedule() {
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));

        AssessmentCandidateDto result = useCase.execute(new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, scheduleId));

        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(assessmentCandidateRepository).save(any(AssessmentCandidate.class));
    }

    @Test
    void should_reject_when_schedule_status_not_assignable() {
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.COMPLETED)));

        assertThatThrownBy(() -> useCase.execute(new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, scheduleId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentCandidateRepository, never()).save(any());
    }

    @Test
    void should_allow_unassign_even_when_already_unassigned() {
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));

        AssessmentCandidateDto result = useCase.execute(new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, null));

        assertThat(result.scheduleId()).isNull();
        verify(assessmentCandidateRepository).save(any(AssessmentCandidate.class));
        verify(assessmentScheduleRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void should_short_circuit_when_reassigning_to_same_schedule() {
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(scheduleId)));

        AssessmentCandidateDto result = useCase.execute(new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, scheduleId));

        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(assessmentScheduleRepository, never()).findByIdForUpdate(any());
        verify(assessmentCandidateRepository, never()).save(any());
        verify(assessmentCandidateRepository, never()).countByScheduleId(eq(scheduleId));
    }

    @Test
    void should_reject_when_student_already_has_an_overlapping_schedule() {
        AssessmentCandidate candidate = candidate(null);
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId))
            .thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                candidate.getStudentId(), UUID.randomUUID(), start, end)));

        assertThatThrownBy(() -> useCase.execute(
                new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, scheduleId)))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("khoảng thời gian này");
        verify(assessmentCandidateRepository, never()).save(any());
    }

    @Test
    void should_ignore_the_student_current_schedule_when_checking_conflicts() {
        // Changing session within the same assessment: the old session overlaps the new session in time,
        // but moving to the new session REPLACES the old spot, so it is not an overlap.
        UUID currentScheduleId = UUID.randomUUID();
        AssessmentCandidate candidate = candidate(currentScheduleId);
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId))
            .thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                candidate.getStudentId(), currentScheduleId, start, end)));

        AssessmentCandidateDto result = useCase.execute(new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, scheduleId));

        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        verify(assessmentCandidateRepository).save(any(AssessmentCandidate.class));
    }

    @Test
    void should_check_conflicts_across_all_assessments_of_the_student() {
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId))
            .thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));

        useCase.execute(new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, scheduleId));

        // assessmentId is not passed: the check must sweep the whole school, not be limited to this assessment.
        verify(assessmentCandidateRepository).findConflictsForStudents(anyCollection(), eq(start), eq(end), eq(null));
    }

    @Test
    void should_skip_the_conflict_check_when_the_schedule_has_no_window() {
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(null)));
        AssessmentSchedule undated = schedule(AssessmentScheduleStatus.DRAFT);
        undated.setStartDate(null);
        undated.setEndDate(null);
        when(assessmentScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(undated));

        useCase.execute(new AssignAssessmentCandidateScheduleCommand(assessmentId, candidateId, scheduleId));

        verify(assessmentCandidateRepository, never()).findConflictsForStudents(anyCollection(), any(), any(), any());
    }

    private AssessmentCandidate candidate(UUID currentScheduleId) {
        AssessmentCandidate c = new AssessmentCandidate();
        c.setId(candidateId);
        c.setAssessmentId(assessmentId);
        c.setStudentId(UUID.randomUUID());
        c.setScheduleId(currentScheduleId);
        c.setStatus(AssessmentCandidateStatus.ASSIGNED);
        return c;
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status) {
        AssessmentSchedule s = new AssessmentSchedule();
        s.setId(scheduleId);
        s.setAssessmentId(assessmentId);
        s.setSchoolRoomId(roomId);
        s.setStatus(status);
        // The anti-overlap rule only runs when there is a time window -- without times the
        // validator skips it.
        s.setStartDate(start);
        s.setEndDate(end);
        return s;
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }
}
