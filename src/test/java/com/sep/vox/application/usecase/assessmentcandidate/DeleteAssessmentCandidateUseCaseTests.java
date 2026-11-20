package com.sep.vox.application.usecase.assessmentcandidate;

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

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteAssessmentCandidateCommand;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.DeleteAssessmentCandidateUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class DeleteAssessmentCandidateUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private DeleteAssessmentCandidateUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new DeleteAssessmentCandidateUseCase(
            assessmentRepository, assessmentCandidateRepository, assessmentSessionRepository, assessmentMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.DRAFT)));
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(assessmentId)));
        when(assessmentSessionRepository.findByCandidateId(candidateId)).thenReturn(List.of());
    }

    @Test
    void should_delete_candidate_without_any_session() {
        useCase.execute(new DeleteAssessmentCandidateCommand(assessmentId, candidateId));

        verify(assessmentCandidateRepository).deleteById(candidateId);
    }

    @Test
    void should_reject_when_candidate_already_has_session() {
        when(assessmentSessionRepository.findByCandidateId(candidateId)).thenReturn(List.of(new AssessmentSession()));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentCandidateCommand(assessmentId, candidateId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã có bài thi");
        verify(assessmentCandidateRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_assessment_already_started() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentCandidateCommand(assessmentId, candidateId)))
            .isInstanceOf(IllegalStateException.class);
        verify(assessmentCandidateRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_user_is_neither_school_admin_nor_chair() {
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentCandidateCommand(assessmentId, candidateId)))
            .isInstanceOf(ForbiddenException.class);
        verify(assessmentCandidateRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_candidate_belongs_to_another_assessment() {
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate(UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentCandidateCommand(assessmentId, candidateId)))
            .isInstanceOf(NotFoundException.class);
        verify(assessmentCandidateRepository, never()).deleteById(any());
    }

    private AssessmentCandidate candidate(UUID ownerAssessmentId) {
        AssessmentCandidate c = new AssessmentCandidate();
        c.setId(candidateId);
        c.setAssessmentId(ownerAssessmentId);
        c.setStudentId(UUID.randomUUID());
        c.setStatus(AssessmentCandidateStatus.ASSIGNED);
        return c;
    }

    private Assessment assessment(AssessmentStatus status) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setStatus(status);
        return assessment;
    }
}
