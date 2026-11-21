package com.sep.vox.application.usecase.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.UpdateAssessmentDeliveryModeCommand;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentDeliveryModeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class UpdateAssessmentDeliveryModeUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private UpdateAssessmentDeliveryModeUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateAssessmentDeliveryModeUseCase(
            assessmentRepository,
            assessmentPaperRepository,
            assessmentMemberRepository,
            assessmentCandidateRepository,
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
    }

    @Test
    void should_update_delivery_mode_for_class_test_as_chair() {
        Assessment assessment = assessment(AssessmentKind.CLASS_TEST, AssessmentStatus.DRAFT);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.save(assessment)).thenAnswer(inv -> inv.getArgument(0));
        AssessmentPaper lockedPaper = mock(AssessmentPaper.class);
        when(lockedPaper.getStatus()).thenReturn(AssessmentPaperStatus.LOCKED);
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(lockedPaper));

        AssessmentDto result = useCase.execute(new UpdateAssessmentDeliveryModeCommand(assessmentId, AssessmentDeliveryMode.STUDENT_DEVICE));

        assertThat(assessment.getDeliveryMode()).isEqualTo(AssessmentDeliveryMode.STUDENT_DEVICE);
        assertThat(result.deliveryMode()).isEqualTo(AssessmentDeliveryMode.STUDENT_DEVICE);
        assertThat(result.papersLocked()).isTrue();
        verify(assessmentRepository).save(assessment);
    }

    @Test
    void should_update_delivery_mode_for_centralized_as_school_admin() {
        Assessment assessment = assessment(AssessmentKind.CENTRALIZED, AssessmentStatus.DRAFT);
        when(schoolUserRepository.findByUserId(userId))
            .thenReturn(Optional.of(com.sep.vox.domain.model.school.SchoolUser.builder().schoolId(schoolId).userId(userId).startDate(null).endDate(null).build()));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId))
            .thenReturn(List.of(new com.sep.vox.application.query.dto.UserRoleInfo(
                UUID.randomUUID(), userId, UUID.randomUUID(), null, "SCHOOL_ADMIN", "School Admin")));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(assessment)).thenAnswer(inv -> inv.getArgument(0));
        AssessmentPaper lockedPaper = mock(AssessmentPaper.class);
        when(lockedPaper.getStatus()).thenReturn(AssessmentPaperStatus.LOCKED);
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(lockedPaper));

        AssessmentDto result = useCase.execute(new UpdateAssessmentDeliveryModeCommand(assessmentId, AssessmentDeliveryMode.LAB));

        assertThat(assessment.getDeliveryMode()).isEqualTo(AssessmentDeliveryMode.LAB);
        assertThat(result.deliveryMode()).isEqualTo(AssessmentDeliveryMode.LAB);
        verify(assessmentRepository).save(assessment);
    }

    @Test
    void should_reject_when_candidates_already_assigned_to_schedule() {
        Assessment assessment = assessment(AssessmentKind.CLASS_TEST, AssessmentStatus.DRAFT);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentCandidateRepository.existsByAssessmentIdAndScheduleIdIsNotNull(assessmentId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentDeliveryModeCommand(assessmentId, AssessmentDeliveryMode.LAB)))
            .isInstanceOf(IllegalStateException.class);
        verify(assessmentRepository, never()).save(assessment);
    }

    @Test
    void should_reject_when_assessment_already_in_progress() {
        Assessment assessment = assessment(AssessmentKind.CLASS_TEST, AssessmentStatus.IN_PROGRESS);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentDeliveryModeCommand(assessmentId, AssessmentDeliveryMode.STUDENT_DEVICE)))
            .isInstanceOf(IllegalStateException.class);
        verify(assessmentRepository, never()).save(assessment);
    }

    @Test
    void should_reject_when_not_authorized() {
        Assessment assessment = assessment(AssessmentKind.CLASS_TEST, AssessmentStatus.DRAFT);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentDeliveryModeCommand(assessmentId, AssessmentDeliveryMode.LAB)))
            .isInstanceOf(ForbiddenException.class);
    }

    private Assessment assessment(AssessmentKind kind, AssessmentStatus status) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setKind(kind);
        assessment.setStatus(status);
        return assessment;
    }
}
