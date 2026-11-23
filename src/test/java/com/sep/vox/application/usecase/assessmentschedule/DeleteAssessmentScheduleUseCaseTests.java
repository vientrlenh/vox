package com.sep.vox.application.usecase.assessmentschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.sep.vox.application.port.input.command.DeleteAssessmentScheduleCommand;
import com.sep.vox.application.port.input.usecase.assessmentschedule.DeleteAssessmentScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class DeleteAssessmentScheduleUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private DeleteAssessmentScheduleUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new DeleteAssessmentScheduleUseCase(
            assessmentRepository, assessmentScheduleRepository, assessmentCandidateRepository, assessmentScheduleProctorRepository,
            assessmentMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.SCHEDULED)));
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule()));
    }

    @Test
    void should_reject_delete_when_has_candidates() {
        when(assessmentCandidateRepository.countByScheduleId(scheduleId)).thenReturn(3L);

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentScheduleCommand(assessmentId, scheduleId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_delete_when_has_proctors() {
        when(assessmentCandidateRepository.countByScheduleId(scheduleId)).thenReturn(0L);
        when(assessmentScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentScheduleCommand(assessmentId, scheduleId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("giám thị");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_delete_when_assessment_already_started() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.IN_PROGRESS)));
        when(assessmentCandidateRepository.countByScheduleId(scheduleId)).thenReturn(0L);
        when(assessmentScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentScheduleCommand(assessmentId, scheduleId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_soft_delete_when_no_candidates() {
        AssessmentSchedule schedule = schedule();
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(assessmentCandidateRepository.countByScheduleId(scheduleId)).thenReturn(0L);
        when(assessmentScheduleProctorRepository.countByScheduleId(scheduleId)).thenReturn(0L);
        when(assessmentScheduleRepository.save(any(AssessmentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(new DeleteAssessmentScheduleCommand(assessmentId, scheduleId));

        assertThat(schedule.getStatus()).isEqualTo(AssessmentScheduleStatus.DELETED);
        verify(assessmentScheduleRepository).save(schedule);
    }

    private Assessment assessment(AssessmentStatus status) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setStatus(status);
        return assessment;
    }

    private AssessmentSchedule schedule() {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(scheduleId);
        schedule.setAssessmentId(assessmentId);
        schedule.setSchoolRoomId(UUID.randomUUID());
        schedule.setStartDate(Instant.parse("2026-07-10T08:00:00+07:00"));
        schedule.setEndDate(Instant.parse("2026-07-10T10:00:00+07:00"));
        schedule.setStatus(AssessmentScheduleStatus.DRAFT);
        return schedule;
    }
}
