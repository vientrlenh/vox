package com.sep.vox.application.usecase.assessmentpaper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.DeleteAssessmentPaperCommand;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.assessmentpaper.DeleteAssessmentPaperUseCase;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Xoá mã đề phải dọn mọi con trỏ đang chỉ vào nó: phần thi, câu trong đề và mã đề đã phân cho thí
 * sinh đều treo trên paper_id mà không có FK nào, nên xoá mỗi dòng đề là để lại con trỏ mồ côi.
 * Phiên thi cũng trỏ vào paper_id nhưng cột đó NOT NULL nên không gỡ được — chỉ chặn xoá.
 */
class DeleteAssessmentPaperUseCaseTests {

    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private AssessmentPaperItemRepository assessmentPaperItemRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private UserContextPort userContextPort;
    private DeleteAssessmentPaperUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentPaperSectionRepository = mock(AssessmentPaperSectionRepository.class);
        assessmentPaperItemRepository = mock(AssessmentPaperItemRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        recalculateAssessmentTimeDurationService = mock(RecalculateAssessmentTimeDurationService.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new DeleteAssessmentPaperUseCase(
            assessmentPaperRepository,
            assessmentPaperSectionRepository,
            assessmentPaperItemRepository,
            assessmentRepository,
            assessmentCandidateRepository,
            assessmentSessionRepository,
            new AssessmentPaperAuthoringAccessService(
                assessmentMemberRepository, mock(SchoolUserRepository.class), mock(UserRoleQueryRepository.class)),
            recalculateAssessmentTimeDurationService,
            userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(assessmentPaperRepository.findById(paperId)).thenReturn(Optional.of(paper(AssessmentPaperStatus.DRAFT)));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(assessmentId, userId)).thenReturn(Optional.of(
            AssessmentMember.builder()
                    .assessmentId(assessmentId)
                    .userId(userId)
                    .role(AssessmentMemberRole.AUTHOR)
                    .grantedAt(Instant.now())
                    .grantedBy(userId)
                    .build()));
    }

    @Test
    void should_delete_paper_items_and_sections_before_the_paper() {
        useCase.execute(new DeleteAssessmentPaperCommand(paperId));

        InOrder order = inOrder(assessmentPaperItemRepository, assessmentPaperSectionRepository, assessmentPaperRepository);
        order.verify(assessmentPaperItemRepository).deleteByPaperIdIn(List.of(paperId));
        order.verify(assessmentPaperSectionRepository).deleteByPaperIdIn(List.of(paperId));
        order.verify(assessmentPaperRepository).deleteById(paperId);
        verify(recalculateAssessmentTimeDurationService).recalculate(assessmentId);
    }

    @Test
    void should_reject_when_paper_is_not_draft() {
        when(assessmentPaperRepository.findById(paperId)).thenReturn(Optional.of(paper(AssessmentPaperStatus.LOCKED)));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentPaperCommand(paperId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentPaperItemRepository, never()).deleteByPaperIdIn(anyCollection());
        verify(assessmentPaperRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_user_is_not_the_creator() {
        AssessmentPaper paper = paper(AssessmentPaperStatus.DRAFT);
        paper.setCreatedBy(UUID.randomUUID().toString());
        when(assessmentPaperRepository.findById(paperId)).thenReturn(Optional.of(paper));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentPaperCommand(paperId)))
            .isInstanceOf(ForbiddenException.class);
        verify(assessmentPaperItemRepository, never()).deleteByPaperIdIn(anyCollection());
        verify(assessmentPaperRepository, never()).deleteById(any());
    }

    @Test
    void should_clear_paper_assignment_of_affected_candidates() {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(assessmentId);
        candidate.setAssignedPaperId(paperId);
        when(assessmentCandidateRepository.findByAssignedPaperId(paperId)).thenReturn(List.of(candidate));

        useCase.execute(new DeleteAssessmentPaperCommand(paperId));

        ArgumentCaptor<Collection<AssessmentCandidate>> captor = ArgumentCaptor.captor();
        verify(assessmentCandidateRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(saved -> {
            assertThat(saved.getAssignedPaperId()).isNull();
            assertThat(saved.getUpdatedBy()).isEqualTo(userId);
            assertThat(saved.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void should_not_touch_candidates_when_nobody_was_assigned_the_paper() {
        when(assessmentCandidateRepository.findByAssignedPaperId(paperId)).thenReturn(List.of());

        useCase.execute(new DeleteAssessmentPaperCommand(paperId));

        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_not_clear_assignments_when_paper_is_not_draft() {
        when(assessmentPaperRepository.findById(paperId)).thenReturn(Optional.of(paper(AssessmentPaperStatus.LOCKED)));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentPaperCommand(paperId)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    /** paper_id của phiên thi là NOT NULL: không gỡ được con trỏ thì chỉ còn cách không cho xoá. */
    @Test
    void should_reject_when_paper_already_has_assessment_sessions() {
        when(assessmentSessionRepository.existsByPaperId(paperId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentPaperCommand(paperId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã có học sinh làm bài");
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
        verify(assessmentPaperRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_assessment_already_started() {
        Assessment assessment = assessment();
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentPaperCommand(paperId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("kỳ thi đã bắt đầu");
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
        verify(assessmentPaperRepository, never()).deleteById(any());
    }

    private AssessmentPaper paper(AssessmentPaperStatus status) {
        AssessmentPaper paper = new AssessmentPaper();
        paper.setId(paperId);
        paper.setAssessmentId(assessmentId);
        paper.setStatus(status);
        paper.setCreatedBy(userId.toString());
        return paper;
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setKind(AssessmentKind.CLASS_TEST);
        return assessment;
    }
}
