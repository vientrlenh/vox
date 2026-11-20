package com.sep.vox.application.usecase.assessmentschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteAssessmentScheduleProctorCommand;
import com.sep.vox.application.port.input.usecase.assessmentschedule.RemoveAssessmentScheduleProctorUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class RemoveAssessmentScheduleProctorUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private RemoveAssessmentScheduleProctorUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID proctorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new RemoveAssessmentScheduleProctorUseCase(
            assessmentRepository, assessmentScheduleRepository, assessmentScheduleProctorRepository, assessmentMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
        when(assessmentScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule()));
    }

    @Test
    void should_remove_proctor_belonging_to_schedule() {
        AssessmentScheduleProctor proctor = new AssessmentScheduleProctor(proctorId, scheduleId, UUID.randomUUID());
        when(assessmentScheduleProctorRepository.findById(proctorId)).thenReturn(Optional.of(proctor));

        UUID result = useCase.execute(new DeleteAssessmentScheduleProctorCommand(assessmentId, scheduleId, proctorId));

        assertThat(result).isEqualTo(proctorId);
        verify(assessmentScheduleProctorRepository).deleteById(proctorId);
    }

    @Test
    void should_reject_when_proctor_not_in_schedule() {
        AssessmentScheduleProctor proctor = new AssessmentScheduleProctor(proctorId, UUID.randomUUID(), UUID.randomUUID());
        when(assessmentScheduleProctorRepository.findById(proctorId)).thenReturn(Optional.of(proctor));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentScheduleProctorCommand(assessmentId, scheduleId, proctorId)))
            .isInstanceOf(NotFoundException.class);
        verify(assessmentScheduleProctorRepository, never()).deleteById(any());
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }

    private AssessmentSchedule schedule() {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(scheduleId);
        schedule.setAssessmentId(assessmentId);
        schedule.setStatus(AssessmentScheduleStatus.DRAFT);
        return schedule;
    }
}
