package com.sep.vox.application.usecase.exampaper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
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
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * Xoá mã đề phải dọn cả phần thi và câu trong đề: hai bảng đó treo trên paper_id mà không có FK
 * nào, nên xoá mỗi dòng đề là để lại section/item mồ côi.
 */
class DeleteExamPaperUseCaseTests {

    private ExamPaperRepository examPaperRepository;
    private ExamPaperSectionRepository examPaperSectionRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private ExamRepository examRepository;
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
        examMemberRepository = mock(ExamMemberRepository.class);
        recalculateExamTimeDurationService = mock(RecalculateExamTimeDurationService.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new DeleteExamPaperUseCase(
            examPaperRepository,
            examPaperSectionRepository,
            examPaperItemRepository,
            examRepository,
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
