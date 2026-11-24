package com.sep.vox.application.usecase.assessmentcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.assessmentcandidate.AssignAssessmentPapersCommand;
import com.sep.vox.application.port.input.command.assessmentcandidate.AssessmentPaperAssignmentItem;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AssignAssessmentPapersUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.assessmentcandidate.AssignAssessmentPapersResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class AssignAssessmentPapersUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AssignAssessmentPapersUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidate1 = UUID.randomUUID();
    private final UUID candidate2 = UUID.randomUUID();
    private final UUID paper1 = UUID.randomUUID();
    private final UUID paper2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new AssignAssessmentPapersUseCase(
            assessmentRepository, assessmentCandidateRepository, assessmentPaperRepository, assessmentMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
    }

    @Test
    void should_assign_papers_on_happy_path() {
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            paper(paper1, AssessmentPaperStatus.LOCKED), paper(paper2, AssessmentPaperStatus.LOCKED)));
        when(assessmentCandidateRepository.findByIdInAndAssessmentId(anyCollection(), org.mockito.ArgumentMatchers.eq(assessmentId)))
            .thenReturn(List.of(candidate(candidate1), candidate(candidate2)));
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });

        AssignAssessmentPapersResponse result = useCase.execute(new AssignAssessmentPapersCommand(assessmentId, List.of(
            new AssessmentPaperAssignmentItem(candidate1, paper1),
            new AssessmentPaperAssignmentItem(candidate2, paper2))));

        assertThat(result.updated()).isEqualTo(2);
        verify(assessmentCandidateRepository).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_any_assessment_paper_not_locked_even_outside_batch() {
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            paper(paper1, AssessmentPaperStatus.LOCKED), paper(paper2, AssessmentPaperStatus.DRAFT)));

        assertThatThrownBy(() -> useCase.execute(new AssignAssessmentPapersCommand(assessmentId, List.of(
            new AssessmentPaperAssignmentItem(candidate1, paper1)))))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_candidate_not_in_assessment() {
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(paper(paper1, AssessmentPaperStatus.LOCKED)));
        when(assessmentCandidateRepository.findByIdInAndAssessmentId(anyCollection(), org.mockito.ArgumentMatchers.eq(assessmentId)))
            .thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new AssignAssessmentPapersCommand(assessmentId, List.of(
            new AssessmentPaperAssignmentItem(candidate1, paper1)))))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_paper_not_part_of_assessment() {
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(paper(paper1, AssessmentPaperStatus.LOCKED)));
        when(assessmentCandidateRepository.findByIdInAndAssessmentId(anyCollection(), org.mockito.ArgumentMatchers.eq(assessmentId)))
            .thenReturn(List.of(candidate(candidate1)));

        assertThatThrownBy(() -> useCase.execute(new AssignAssessmentPapersCommand(assessmentId, List.of(
            new AssessmentPaperAssignmentItem(candidate1, UUID.randomUUID())))))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    private AssessmentCandidate candidate(UUID id) {
        AssessmentCandidate c = new AssessmentCandidate();
        c.setId(id);
        c.setAssessmentId(assessmentId);
        c.setStudentId(UUID.randomUUID());
        c.setStatus(AssessmentCandidateStatus.ASSIGNED);
        return c;
    }

    private AssessmentPaper paper(UUID id, AssessmentPaperStatus status) {
        AssessmentPaper p = new AssessmentPaper();
        p.setId(id);
        p.setAssessmentId(assessmentId);
        p.setStatus(status);
        return p;
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }
}
