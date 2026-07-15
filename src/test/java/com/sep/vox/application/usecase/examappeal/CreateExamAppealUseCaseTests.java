package com.sep.vox.application.usecase.examappeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamAppealCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.examappeal.CreateExamAppealUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

public class CreateExamAppealUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamItemResponseRepository examItemResponseRepository;
    private ExamAppealAccessService examAppealAccessService;
    private CreateExamAppealUseCase useCase;

    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID paperItemId = UUID.randomUUID();
    private final UUID responseId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID appealId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        useCase = new CreateExamAppealUseCase(
            examResultAppealRepository,
            examCandidateResultRepository,
            examItemResponseRepository,
            examAppealAccessService
        );

        when(examAppealAccessService.requireActiveUserId()).thenReturn(studentId);
        when(examResultAppealRepository.save(any())).thenAnswer(invocation -> {
            ExamResultAppeal appeal = invocation.getArgument(0);
            appeal.setId(appealId);
            return appeal;
        });
    }

    private AppealContext context(ExamCandidateResultStatus status) {
        var candidateResult = new ExamCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setSessionId(sessionId);
        candidateResult.setStatus(status);
        candidateResult.setTotalScore(new BigDecimal("6.00"));
        return new AppealContext(null, candidateResult, new ExamSession(),
            UUID.randomUUID(), studentId, "IELTS Speaking Mock");
    }

    private ExamItemResponse response() {
        var response = new ExamItemResponse();
        response.setId(responseId);
        response.setPaperItemId(paperItemId);
        return response;
    }

    private CreateExamAppealCommand command() {
        return new CreateExamAppealCommand(candidateResultId, paperItemId, "Điểm không phản ánh đúng bài nói", null);
    }

    @Test
    void should_create_appeal_and_resolve_response_of_the_part() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        var result = useCase.execute(command());

        assertThat(result).isEqualTo(appealId);
        var captor = ArgumentCaptor.forClass(ExamResultAppeal.class);
        verify(examResultAppealRepository).save(captor.capture());
        var appeal = captor.getValue();
        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.PENDING);
        assertThat(appeal.getResponseId()).isEqualTo(responseId);
        assertThat(appeal.getScoreBefore()).isEqualByComparingTo("6.00");
        assertThat(appeal.getRequestedBy()).isEqualTo(studentId);
    }

    @Test
    void should_move_candidate_result_to_appealed() {
        var context = context(ExamCandidateResultStatus.RELEASED);
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId)).thenReturn(context);
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        useCase.execute(command());

        assertThat(context.candidateResult().getStatus()).isEqualTo(ExamCandidateResultStatus.APPEALED);
        verify(examCandidateResultRepository).save(context.candidateResult());
    }

    @Test
    void should_reject_when_result_not_released() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.PENDING_REVIEW));

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã được công bố");

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_when_an_open_appeal_already_exists() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examResultAppealRepository.existsOpenByCandidateResultId(candidateResultId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(DuplicatedException.class);

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_when_caller_is_not_the_owning_student() {
        var context = context(ExamCandidateResultStatus.RELEASED);
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId)).thenReturn(context);
        org.mockito.Mockito.doThrow(new ForbiddenException("BẢO MẬT"))
            .when(examAppealAccessService).authorizeOwningStudent(eq(context), eq(studentId));

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(ForbiddenException.class);

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_when_part_has_no_response() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("câu trả lời");
    }

    @Test
    void should_reject_when_part_not_selected() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));

        assertThatThrownBy(() ->
            useCase.execute(new CreateExamAppealCommand(candidateResultId, null, "lý do", null)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
