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
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.examappeal.CreateExamAppealUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamResultAppealItem;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamResultAppealItemRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

public class CreateExamAppealUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamResultAppealItemRepository examResultAppealItemRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamItemResponseRepository examItemResponseRepository;
    private ExamAppealAccessService examAppealAccessService;
    private CreateExamAppealUseCase useCase;

    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID paperItemId = UUID.randomUUID();
    private final UUID otherPaperItemId = UUID.randomUUID();
    private final UUID responseId = UUID.randomUUID();
    private final UUID otherResponseId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID appealId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examResultAppealItemRepository = mock(ExamResultAppealItemRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        useCase = new CreateExamAppealUseCase(
            examResultAppealRepository,
            examResultAppealItemRepository,
            examCandidateResultRepository,
            examItemResponseRepository,
            mock(ExamGradingAssignmentRepository.class),
            examAppealAccessService,
            mock(ResultStatusHistoryRecorder.class)
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
        return response(responseId, paperItemId);
    }

    private ExamItemResponse response(UUID id, UUID paperItem) {
        var response = new ExamItemResponse();
        response.setId(id);
        response.setPaperItemId(paperItem);
        return response;
    }

    private CreateExamAppealCommand command() {
        return command(List.of(paperItemId));
    }

    private CreateExamAppealCommand command(List<UUID> paperItemIds) {
        return new CreateExamAppealCommand(
            candidateResultId, paperItemIds, "Điểm không phản ánh đúng bài nói", null);
    }

    @SuppressWarnings("unchecked")
    private List<ExamResultAppealItem> captureSavedItems() {
        var captor = ArgumentCaptor.forClass(List.class);
        verify(examResultAppealItemRepository).saveAll(captor.capture());
        return captor.getValue();
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
        assertThat(appeal.getScoreBefore()).isEqualByComparingTo("6.00");
        assertThat(appeal.getRequestedBy()).isEqualTo(studentId);

        var items = captureSavedItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getAppealId()).isEqualTo(appealId);
        assertThat(items.get(0).getPaperItemId()).isEqualTo(paperItemId);
        assertThat(items.get(0).getResponseId()).isEqualTo(responseId);
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

    // ---- hạn mức số vòng phúc khảo ------------------------------------------

    @Test
    void should_allow_a_second_appeal_after_the_first_was_published() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examResultAppealRepository.countPublishedByCandidateResultId(candidateResultId)).thenReturn(1L);
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        assertThat(useCase.execute(command())).isEqualTo(appealId);
    }

    @Test
    void should_reject_a_third_appeal_when_round_limit_reached() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examResultAppealRepository.countPublishedByCandidateResultId(candidateResultId)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("tối đa 2 lần");

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_not_count_rejected_appeals_toward_the_round_limit() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        useCase.execute(command());

        // Quota phải đọc riêng số đơn ĐÃ CÔNG BỐ, không phải tổng số đơn của kết quả.
        verify(examResultAppealRepository).countPublishedByCandidateResultId(candidateResultId);
        verify(examResultAppealRepository, never()).findByCandidateResultId(any());
    }

    @Test
    void should_report_open_appeal_before_round_limit() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examResultAppealRepository.existsOpenByCandidateResultId(candidateResultId)).thenReturn(true);
        when(examResultAppealRepository.countPublishedByCandidateResultId(candidateResultId)).thenReturn(2L);

        // Vướng cả hai thì báo lỗi cụ thể hơn: đang có đơn xử lý dở.
        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(DuplicatedException.class);
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

        assertThatThrownBy(() -> useCase.execute(command(null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_when_no_part_selected() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));

        assertThatThrownBy(() -> useCase.execute(command(List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ít nhất một phần thi");

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_create_appeal_with_multiple_items() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examItemResponseRepository.findBySessionId(sessionId))
            .thenReturn(List.of(response(), response(otherResponseId, otherPaperItemId)));

        var result = useCase.execute(command(List.of(paperItemId, otherPaperItemId)));

        assertThat(result).isEqualTo(appealId);
        var items = captureSavedItems();
        assertThat(items).hasSize(2);
        assertThat(items).extracting(item -> item.getPaperItemId())
            .containsExactly(paperItemId, otherPaperItemId);
        assertThat(items).extracting(item -> item.getResponseId())
            .containsExactly(responseId, otherResponseId);
    }

    @Test
    void should_reject_duplicate_paper_items() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));

        assertThatThrownBy(() -> useCase.execute(command(List.of(paperItemId, paperItemId))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("trùng phần thi");

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_when_any_selected_part_has_no_response() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        assertThatThrownBy(() -> useCase.execute(command(List.of(paperItemId, otherPaperItemId))))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("câu trả lời");

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_resolve_all_responses_with_a_single_repository_call() {
        when(examAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(ExamCandidateResultStatus.RELEASED));
        when(examItemResponseRepository.findBySessionId(sessionId))
            .thenReturn(List.of(response(), response(otherResponseId, otherPaperItemId)));

        useCase.execute(command(List.of(paperItemId, otherPaperItemId)));

        verify(examItemResponseRepository, org.mockito.Mockito.times(1)).findBySessionId(sessionId);
    }
}
