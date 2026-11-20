package com.sep.vox.application.usecase.exampaper;

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

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.DeleteExamPaperCommand;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.exampaper.DeleteExamPaperUseCase;
import com.sep.vox.application.port.input.service.ExamPaperAuthoringAccessService;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Xoá mã đề phải dọn mọi con trỏ đang chỉ vào nó: phần thi, câu trong đề và mã đề đã phân cho thí
 * sinh đều treo trên paper_id mà không có FK nào, nên xoá mỗi dòng đề là để lại con trỏ mồ côi.
 * Phiên thi cũng trỏ vào paper_id nhưng cột đó NOT NULL nên không gỡ được — chỉ chặn xoá.
 */
class DeleteExamPaperUseCaseTests {

    private ExamPaperRepository examPaperRepository;
    private ExamPaperSectionRepository examPaperSectionRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamMemberRepository examMemberRepository;
    private RecalculateExamTimeDurationService recalculateExamTimeDurationService;
    private UserContextPort userContextPort;
    private DeleteExamPaperUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examPaperRepository = mock(ExamPaperRepository.class);
        examPaperSectionRepository = mock(ExamPaperSectionRepository.class);
        examPaperItemRepository = mock(ExamPaperItemRepository.class);
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        recalculateExamTimeDurationService = mock(RecalculateExamTimeDurationService.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new DeleteExamPaperUseCase(
            examPaperRepository,
            examPaperSectionRepository,
            examPaperItemRepository,
            examRepository,
            examCandidateRepository,
            examSessionRepository,
            new ExamPaperAuthoringAccessService(
                examMemberRepository, mock(SchoolUserRepository.class), mock(UserRoleQueryRepository.class)),
            recalculateExamTimeDurationService,
            userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(examPaperRepository.findById(paperId)).thenReturn(Optional.of(paper(ExamPaperStatus.DRAFT)));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(examMemberRepository.findByExamIdAndUserId(examId, userId)).thenReturn(Optional.of(
            new ExamMember(examId, userId, ExamMemberRole.AUTHOR, Instant.now(), userId)));
    }

    @Test
    void should_delete_paper_items_and_sections_before_the_paper() {
        useCase.execute(new DeleteExamPaperCommand(paperId));

        var order = inOrder(examPaperItemRepository, examPaperSectionRepository, examPaperRepository);
        order.verify(examPaperItemRepository).deleteByPaperIdIn(List.of(paperId));
        order.verify(examPaperSectionRepository).deleteByPaperIdIn(List.of(paperId));
        order.verify(examPaperRepository).deleteById(paperId);
        verify(recalculateExamTimeDurationService).recalculate(examId);
    }

    @Test
    void should_reject_when_paper_is_not_draft() {
        when(examPaperRepository.findById(paperId)).thenReturn(Optional.of(paper(ExamPaperStatus.LOCKED)));

        assertThatThrownBy(() -> useCase.execute(new DeleteExamPaperCommand(paperId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examPaperItemRepository, never()).deleteByPaperIdIn(anyCollection());
        verify(examPaperRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_user_is_not_the_creator() {
        var paper = paper(ExamPaperStatus.DRAFT);
        paper.setCreatedBy(UUID.randomUUID());
        when(examPaperRepository.findById(paperId)).thenReturn(Optional.of(paper));

        assertThatThrownBy(() -> useCase.execute(new DeleteExamPaperCommand(paperId)))
            .isInstanceOf(ForbiddenException.class);
        verify(examPaperItemRepository, never()).deleteByPaperIdIn(anyCollection());
        verify(examPaperRepository, never()).deleteById(any());
    }

    @Test
    void should_clear_paper_assignment_of_affected_candidates() {
        var candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(examId);
        candidate.setAssignedPaperId(paperId);
        when(examCandidateRepository.findByAssignedPaperId(paperId)).thenReturn(List.of(candidate));

        useCase.execute(new DeleteExamPaperCommand(paperId));

        ArgumentCaptor<Collection<ExamCandidate>> captor = ArgumentCaptor.captor();
        verify(examCandidateRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(saved -> {
            assertThat(saved.getAssignedPaperId()).isNull();
            assertThat(saved.getUpdatedBy()).isEqualTo(userId);
            assertThat(saved.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void should_not_touch_candidates_when_nobody_was_assigned_the_paper() {
        when(examCandidateRepository.findByAssignedPaperId(paperId)).thenReturn(List.of());

        useCase.execute(new DeleteExamPaperCommand(paperId));

        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_not_clear_assignments_when_paper_is_not_draft() {
        when(examPaperRepository.findById(paperId)).thenReturn(Optional.of(paper(ExamPaperStatus.LOCKED)));

        assertThatThrownBy(() -> useCase.execute(new DeleteExamPaperCommand(paperId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    /** paper_id của phiên thi là NOT NULL: không gỡ được con trỏ thì chỉ còn cách không cho xoá. */
    @Test
    void should_reject_when_paper_already_has_exam_sessions() {
        when(examSessionRepository.existsByPaperId(paperId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new DeleteExamPaperCommand(paperId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã có học sinh làm bài");
        verify(examCandidateRepository, never()).saveAll(anyCollection());
        verify(examPaperRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_exam_already_started() {
        var exam = exam();
        exam.setStatus(ExamStatus.IN_PROGRESS);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> useCase.execute(new DeleteExamPaperCommand(paperId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("kỳ thi đã bắt đầu");
        verify(examCandidateRepository, never()).saveAll(anyCollection());
        verify(examPaperRepository, never()).deleteById(any());
    }

    private ExamPaper paper(ExamPaperStatus status) {
        var paper = new ExamPaper();
        paper.setId(paperId);
        paper.setExamId(examId);
        paper.setStatus(status);
        paper.setCreatedBy(userId);
        return paper;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setKind(ExamKind.CLASS_TEST);
        return exam;
    }
}
