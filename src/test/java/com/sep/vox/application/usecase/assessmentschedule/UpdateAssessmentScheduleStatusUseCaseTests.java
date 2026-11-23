package com.sep.vox.application.usecase.assessmentschedule;

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
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.UpdateAssessmentScheduleStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.assessmentschedule.UpdateAssessmentScheduleStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository.StudentScheduleConflict;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class UpdateAssessmentScheduleStatusUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private UpdateAssessmentScheduleStatusUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateAssessmentScheduleStatusUseCase(
            assessmentRepository, assessmentScheduleRepository, assessmentScheduleProctorRepository,
            // Validator thật trên proctor repo đã mock: mặc định "giáo viên rảnh", test dời ca bên
            // dưới bật cờ trùng lịch lên để kiểm tra luật.
            new AssessmentScheduleProctorConflictValidator(assessmentScheduleProctorRepository),
            // Validator thí sinh cũng dùng bản thật trên candidate repo đã mock, cùng lý do.
            new AssessmentScheduleCandidateConflictValidator(assessmentCandidateRepository, mock(UserRepository.class)),
            assessmentCandidateRepository,
            assessmentMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.SCHEDULED)));
        when(assessmentScheduleRepository.save(any(AssessmentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assessmentCandidateRepository.findByScheduleId(any(UUID.class))).thenReturn(List.of());
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of());
        when(assessmentScheduleProctorRepository.findByScheduleId(any(UUID.class))).thenReturn(List.of());
    }

    @Test
    void should_reject_publish_without_proctor() {
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(command("PUBLISH", null)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_publish_when_every_candidate_has_paper() {
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT);
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(assessmentScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(1L);
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(candidateWithPaper()));

        AssessmentScheduleDto result = useCase.execute(command("PUBLISH", null));

        assertThat(schedule.getStatus()).isEqualTo(AssessmentScheduleStatus.PUBLISHED);
        assertThat(result.status()).isEqualTo(AssessmentScheduleStatus.PUBLISHED);
    }

    /**
     * A session published while its candidates have no paper only blows up when entering the assessment room
     * (VerifyAssessmentScheduleOtpUseCase) -- it must be blocked right at publishing.
     */
    @Test
    void should_reject_publish_when_candidate_has_no_paper() {
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(1L);
        when(assessmentCandidateRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(candidateWithPaper(), candidate()));

        assertThatThrownBy(() -> useCase.execute(command("PUBLISH", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Còn 1 học sinh chưa được gán đề");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_publish_when_schedule_has_no_candidate() {
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));
        when(assessmentScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(1L);
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(command("PUBLISH", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ca thi chưa có thí sinh nào");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    /**
     * A candidate who was cancelled/exempted does not enter the room, so does not need a paper and
     * must not block the whole session.
     */
    @Test
    void should_ignore_cancelled_candidate_without_paper_when_publishing() {
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.DRAFT);
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(assessmentScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(1L);
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(
            candidateWithPaper(),
            candidate(null, AssessmentCandidateStatus.CANCELLED)));

        useCase.execute(command("PUBLISH", null));

        assertThat(schedule.getStatus()).isEqualTo(AssessmentScheduleStatus.PUBLISHED);
    }

    @Test
    void should_reject_invalid_transition() {
        // COMPLETE requires PUBLISHED; DRAFT should be rejected
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(command("COMPLETE", null)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_unknown_action() {
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule(AssessmentScheduleStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(command("FROBNICATE", null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_move_and_set_moved_to_schedule_id() {
        UUID targetId = givenTarget(AssessmentScheduleStatus.DRAFT);
        AssessmentSchedule schedule = givenSource(AssessmentScheduleStatus.PUBLISHED);

        AssessmentScheduleDto result = useCase.execute(command("MOVE", targetId));

        assertThat(schedule.getStatus()).isEqualTo(AssessmentScheduleStatus.MOVED);
        assertThat(schedule.getMovedToScheduleId()).isEqualTo(targetId);
        assertThat(result.movedToScheduleId()).isEqualTo(targetId);
    }

    @Test
    void should_move_candidates_to_target_schedule() {
        UUID targetId = givenTarget(AssessmentScheduleStatus.DRAFT);
        givenSource(AssessmentScheduleStatus.PUBLISHED);
        AssessmentCandidate first = candidate();
        AssessmentCandidate second = candidate();
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(first, second));

        useCase.execute(command("MOVE", targetId));

        assertThat(first.getScheduleId()).isEqualTo(targetId);
        assertThat(second.getScheduleId()).isEqualTo(targetId);
        verify(assessmentCandidateRepository).saveAll(List.of(first, second));
    }

    @Test
    void should_move_proctors_to_target_schedule() {
        UUID targetId = givenTarget(AssessmentScheduleStatus.DRAFT);
        givenSource(AssessmentScheduleStatus.PUBLISHED);
        UUID teacherId = UUID.randomUUID();
        UUID proctorId = UUID.randomUUID();
        when(assessmentScheduleProctorRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(AssessmentScheduleProctor.builder()
                    .id(proctorId)
                    .scheduleId(scheduleId)
                    .teacherId(teacherId)
                    .build()));
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(targetId, teacherId)).thenReturn(false);

        useCase.execute(command("MOVE", targetId));

        ArgumentCaptor<AssessmentScheduleProctor> saved = ArgumentCaptor.forClass(AssessmentScheduleProctor.class);
        verify(assessmentScheduleProctorRepository).save(saved.capture());
        assertThat(saved.getValue().getScheduleId()).isEqualTo(targetId);
        assertThat(saved.getValue().getTeacherId()).isEqualTo(teacherId);
        verify(assessmentScheduleProctorRepository).deleteById(proctorId);
    }

    @Test
    void should_not_duplicate_proctor_already_assigned_to_target() {
        UUID targetId = givenTarget(AssessmentScheduleStatus.DRAFT);
        givenSource(AssessmentScheduleStatus.PUBLISHED);
        UUID teacherId = UUID.randomUUID();
        UUID proctorId = UUID.randomUUID();
        when(assessmentScheduleProctorRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(AssessmentScheduleProctor.builder()
                    .id(proctorId)
                    .scheduleId(scheduleId)
                    .teacherId(teacherId)
                    .build()));
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(targetId, teacherId)).thenReturn(true);

        useCase.execute(command("MOVE", targetId));

        verify(assessmentScheduleProctorRepository, never()).save(any());
        verify(assessmentScheduleProctorRepository).deleteById(proctorId);
    }

    /**
     * The target session has a different time window from the source session, so a proctor who
     * moves along may collide with a third session -- moving a session must not silently create an
     * overlap.
     */
    @Test
    void should_reject_move_when_proctor_busy_in_target_window() {
        UUID targetId = givenTarget(AssessmentScheduleStatus.DRAFT);
        givenSource(AssessmentScheduleStatus.PUBLISHED);
        UUID teacherId = UUID.randomUUID();
        when(assessmentScheduleProctorRepository.findByScheduleId(scheduleId))
            .thenReturn(List.of(AssessmentScheduleProctor.builder()
                    .id(UUID.randomUUID())
                    .scheduleId(scheduleId)
                    .teacherId(teacherId)
                    .build()));
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(targetId, teacherId)).thenReturn(false);
        when(assessmentScheduleProctorRepository.existsOverlappingAssignment(
            eq(teacherId), any(Instant.class), any(Instant.class), eq(scheduleId))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command("MOVE", targetId)))
            .isInstanceOf(ResourceDuplicatedException.class);
        verify(assessmentScheduleProctorRepository, never()).save(any());
    }

    /**
     * Symmetric with the proctor rule: a candidate who moves along to the target session may
     * collide with a third session.
     */
    @Test
    void should_reject_move_when_student_busy_in_target_window() {
        UUID targetId = givenTarget(AssessmentScheduleStatus.DRAFT);
        givenSource(AssessmentScheduleStatus.PUBLISHED);
        AssessmentCandidate moving = candidate();
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(moving));
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                moving.getStudentId(), UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(3600))));

        assertThatThrownBy(() -> useCase.execute(command("MOVE", targetId)))
            .isInstanceOf(ResourceDuplicatedException.class);
        // Nobody is moved, not even those with no conflict.
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_ignore_the_source_schedule_when_checking_candidate_conflicts() {
        // A candidate in the source session is of course "busy" there -- the source session is
        // about to become MOVED, so it is not counted.
        UUID targetId = givenTarget(AssessmentScheduleStatus.DRAFT);
        givenSource(AssessmentScheduleStatus.PUBLISHED);
        AssessmentCandidate moving = candidate();
        when(assessmentCandidateRepository.findByScheduleId(scheduleId)).thenReturn(List.of(moving));
        when(assessmentCandidateRepository.findConflictsForStudents(anyCollection(), any(), any(), any()))
            .thenReturn(List.of(new StudentScheduleConflict(
                moving.getStudentId(), scheduleId, Instant.now(), Instant.now().plusSeconds(3600))));

        useCase.execute(command("MOVE", targetId));

        assertThat(moving.getScheduleId()).isEqualTo(targetId);
        verify(assessmentCandidateRepository).saveAll(anyCollection());
    }

    @Test
    void should_reject_move_to_the_same_schedule() {
        givenSource(AssessmentScheduleStatus.PUBLISHED);

        assertThatThrownBy(() -> useCase.execute(command("MOVE", scheduleId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_move_when_assessment_already_started() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.IN_PROGRESS)));
        UUID targetId = givenTarget(AssessmentScheduleStatus.DRAFT);
        givenSource(AssessmentScheduleStatus.PUBLISHED);

        assertThatThrownBy(() -> useCase.execute(command("MOVE", targetId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_allow_complete_when_assessment_in_progress() {
        // Marking a session completed is an operational action during the assessment -- it is outside the
        // scope of the lock.
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.IN_PROGRESS)));
        AssessmentSchedule schedule = givenSource(AssessmentScheduleStatus.PUBLISHED);

        useCase.execute(command("COMPLETE", null));

        assertThat(schedule.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
    }

    private UUID givenTarget(AssessmentScheduleStatus status) {
        UUID targetId = UUID.randomUUID();
        AssessmentSchedule target = schedule(status);
        target.setId(targetId);
        when(assessmentScheduleRepository.findByIdForUpdate(targetId)).thenReturn(Optional.of(target));
        return targetId;
    }

    private AssessmentSchedule givenSource(AssessmentScheduleStatus status) {
        AssessmentSchedule schedule = schedule(status);
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        return schedule;
    }

    private AssessmentCandidate candidate() {
        return candidate(null, AssessmentCandidateStatus.ASSIGNED);
    }

    private AssessmentCandidate candidateWithPaper() {
        return candidate(UUID.randomUUID(), AssessmentCandidateStatus.ASSIGNED);
    }

    private AssessmentCandidate candidate(UUID assignedPaperId, AssessmentCandidateStatus status) {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(assessmentId);
        candidate.setStudentId(UUID.randomUUID());
        candidate.setScheduleId(scheduleId);
        candidate.setAssignedPaperId(assignedPaperId);
        candidate.setStatus(status);
        return candidate;
    }

    private UpdateAssessmentScheduleStatusCommand command(String action, UUID targetScheduleId) {
        return new UpdateAssessmentScheduleStatusCommand(assessmentId, scheduleId, action, null, targetScheduleId);
    }

    private Assessment assessment(AssessmentStatus status) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setStatus(status);
        return assessment;
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(scheduleId);
        schedule.setAssessmentId(assessmentId);
        schedule.setSchoolRoomId(roomId);
        schedule.setStartDate(Instant.parse("2026-07-10T08:00:00+07:00"));
        schedule.setEndDate(Instant.parse("2026-07-10T10:00:00+07:00"));
        schedule.setStatus(status);
        return schedule;
    }
}
