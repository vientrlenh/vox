package com.sep.vox.application.usecase.assessmentappeal;

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

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentAppealCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.assessmentappeal.CreateAssessmentAppealUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentResultAppealItem;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealItemRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

public class CreateAssessmentAppealUseCaseTests {

    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private AssessmentResultAppealItemRepository assessmentResultAppealItemRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentItemResponseRepository assessmentItemResponseRepository;
    private AssessmentAppealAccessService assessmentAppealAccessService;
    private CreateAssessmentAppealUseCase useCase;

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
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        assessmentResultAppealItemRepository = mock(AssessmentResultAppealItemRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentItemResponseRepository = mock(AssessmentItemResponseRepository.class);
        assessmentAppealAccessService = mock(AssessmentAppealAccessService.class);
        useCase = new CreateAssessmentAppealUseCase(
            assessmentResultAppealRepository,
            assessmentResultAppealItemRepository,
            assessmentCandidateResultRepository,
            assessmentItemResponseRepository,
            mock(AssessmentGradingAssignmentRepository.class),
            assessmentAppealAccessService,
            mock(ResultStatusHistoryRecorder.class)
        );

        when(assessmentAppealAccessService.requireActiveUserId()).thenReturn(studentId);
        when(assessmentResultAppealRepository.save(any())).thenAnswer(invocation -> {
            AssessmentResultAppeal appeal = invocation.getArgument(0);
            appeal.setId(appealId);
            return appeal;
        });
    }

    private AppealContext context(AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult candidateResult = new AssessmentCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setSessionId(sessionId);
        candidateResult.setStatus(status);
        candidateResult.setTotalScore(new BigDecimal("6.00"));
        return new AppealContext(null, candidateResult, new AssessmentSession(),
            UUID.randomUUID(), studentId, "IELTS Speaking Mock");
    }

    private AssessmentItemResponse response() {
        return response(responseId, paperItemId);
    }

    private AssessmentItemResponse response(UUID id, UUID paperItem) {
        AssessmentItemResponse response = new AssessmentItemResponse();
        response.setId(id);
        response.setPaperItemId(paperItem);
        return response;
    }

    private CreateAssessmentAppealCommand command() {
        return command(List.of(paperItemId));
    }

    private CreateAssessmentAppealCommand command(List<UUID> paperItemIds) {
        return new CreateAssessmentAppealCommand(
            candidateResultId, paperItemIds, "Điểm không phản ánh đúng bài nói", null);
    }

    @SuppressWarnings("unchecked")
    private List<AssessmentResultAppealItem> captureSavedItems() {
        ArgumentCaptor<List<AssessmentResultAppealItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(assessmentResultAppealItemRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_create_appeal_and_resolve_response_of_the_part() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        UUID result = useCase.execute(command());

        assertThat(result).isEqualTo(appealId);
        ArgumentCaptor<AssessmentResultAppeal> captor = ArgumentCaptor.forClass(AssessmentResultAppeal.class);
        verify(assessmentResultAppealRepository).save(captor.capture());
        AssessmentResultAppeal appeal = captor.getValue();
        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.PENDING);
        assertThat(appeal.getScoreBefore()).isEqualByComparingTo("6.00");
        assertThat(appeal.getRequestedBy()).isEqualTo(studentId);

        List<AssessmentResultAppealItem> items = captureSavedItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getAppealId()).isEqualTo(appealId);
        assertThat(items.get(0).getPaperItemId()).isEqualTo(paperItemId);
        assertThat(items.get(0).getResponseId()).isEqualTo(responseId);
    }

    @Test
    void should_move_candidate_result_to_appealed() {
        AssessmentAppealAccessService.AppealContext context = context(AssessmentCandidateResultStatus.RELEASED);
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId)).thenReturn(context);
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        useCase.execute(command());

        assertThat(context.candidateResult().getStatus()).isEqualTo(AssessmentCandidateResultStatus.APPEALED);
        verify(assessmentCandidateResultRepository).save(context.candidateResult());
    }

    @Test
    void should_reject_when_result_not_released() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.PENDING_REVIEW));

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã được công bố");

        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_when_an_open_appeal_already_exists() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentResultAppealRepository.existsOpenByCandidateResultId(candidateResultId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(ResourceDuplicatedException.class);

        verify(assessmentResultAppealRepository, never()).save(any());
    }

    // ---- hạn mức số vòng phúc khảo ------------------------------------------

    @Test
    void should_allow_a_second_appeal_after_the_first_was_published() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentResultAppealRepository.countPublishedByCandidateResultId(candidateResultId)).thenReturn(1L);
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        assertThat(useCase.execute(command())).isEqualTo(appealId);
    }

    @Test
    void should_reject_a_third_appeal_when_round_limit_reached() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentResultAppealRepository.countPublishedByCandidateResultId(candidateResultId)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tối đa 2 lần");

        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_not_count_rejected_appeals_toward_the_round_limit() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        useCase.execute(command());

        // Quota phải đọc riêng số đơn ĐÃ CÔNG BỐ, không phải tổng số đơn của kết quả.
        verify(assessmentResultAppealRepository).countPublishedByCandidateResultId(candidateResultId);
        verify(assessmentResultAppealRepository, never()).findByCandidateResultId(any());
    }

    @Test
    void should_report_open_appeal_before_round_limit() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentResultAppealRepository.existsOpenByCandidateResultId(candidateResultId)).thenReturn(true);
        when(assessmentResultAppealRepository.countPublishedByCandidateResultId(candidateResultId)).thenReturn(2L);

        // Vướng cả hai thì báo lỗi cụ thể hơn: đang có đơn xử lý dở.
        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(ResourceDuplicatedException.class);
    }

    @Test
    void should_reject_when_caller_is_not_the_owning_student() {
        AssessmentAppealAccessService.AppealContext context = context(AssessmentCandidateResultStatus.RELEASED);
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId)).thenReturn(context);
        org.mockito.Mockito.doThrow(new ForbiddenException("BẢO MẬT"))
            .when(assessmentAppealAccessService).authorizeOwningStudent(eq(context), eq(studentId));

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(ForbiddenException.class);

        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_when_part_has_no_response() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("câu trả lời");
    }

    // ---- không chọn phần nào = phúc khảo toàn bài ---------------------------

    @Test
    void should_appeal_every_answered_part_when_none_selected() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentItemResponseRepository.findBySessionId(sessionId))
            .thenReturn(List.of(response(), response(otherResponseId, otherPaperItemId)));

        assertThat(useCase.execute(command(null))).isEqualTo(appealId);

        List<AssessmentResultAppealItem> items = captureSavedItems();
        assertThat(items).extracting(item -> item.getPaperItemId())
            .containsExactly(paperItemId, otherPaperItemId);
        assertThat(items).extracting(item -> item.getResponseId())
            .containsExactly(responseId, otherResponseId);
    }

    @Test
    void should_appeal_every_answered_part_when_selection_is_empty() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentItemResponseRepository.findBySessionId(sessionId))
            .thenReturn(List.of(response(), response(otherResponseId, otherPaperItemId)));

        assertThat(useCase.execute(command(List.of()))).isEqualTo(appealId);

        assertThat(captureSavedItems()).extracting(item -> item.getPaperItemId())
            .containsExactly(paperItemId, otherPaperItemId);
    }

    @Test
    void should_reject_when_the_paper_has_no_answer_at_all() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(command(null)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("không có câu trả lời nào");

        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_create_appeal_with_multiple_items() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentItemResponseRepository.findBySessionId(sessionId))
            .thenReturn(List.of(response(), response(otherResponseId, otherPaperItemId)));

        UUID result = useCase.execute(command(List.of(paperItemId, otherPaperItemId)));

        assertThat(result).isEqualTo(appealId);
        List<AssessmentResultAppealItem> items = captureSavedItems();
        assertThat(items).hasSize(2);
        assertThat(items).extracting(item -> item.getPaperItemId())
            .containsExactly(paperItemId, otherPaperItemId);
        assertThat(items).extracting(item -> item.getResponseId())
            .containsExactly(responseId, otherResponseId);
    }

    @Test
    void should_reject_duplicate_paper_items() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));

        assertThatThrownBy(() -> useCase.execute(command(List.of(paperItemId, paperItemId))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("trùng phần thi");

        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_when_any_selected_part_has_no_response() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(response()));

        assertThatThrownBy(() -> useCase.execute(command(List.of(paperItemId, otherPaperItemId))))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("câu trả lời");

        verify(assessmentResultAppealRepository, never()).save(any());    
    }

    @Test
    void should_resolve_all_responses_with_a_single_repository_call() {
        when(assessmentAppealAccessService.loadByCandidateResultId(candidateResultId))
            .thenReturn(context(AssessmentCandidateResultStatus.RELEASED));
        when(assessmentItemResponseRepository.findBySessionId(sessionId))
            .thenReturn(List.of(response(), response(otherResponseId, otherPaperItemId)));

        useCase.execute(command(List.of(paperItemId, otherPaperItemId)));

        verify(assessmentItemResponseRepository, org.mockito.Mockito.times(1)).findBySessionId(sessionId);
    }
}
