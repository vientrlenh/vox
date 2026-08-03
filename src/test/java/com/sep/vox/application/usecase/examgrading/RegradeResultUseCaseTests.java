package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingActionSupport.PreparedAction;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver.ResolvedItem;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.RegradeResultUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Use case phức tạp nhất của tính năng: nó vừa ghi đè phán quyết của AI, vừa quyết định
 * bài đi về trạng thái nào. Hai chỗ dễ sai nhất được khoá ở đây — bản cũ không được
 * SUPERSEDED thì điểm cũ vẫn được tính, và trạng thái đích lấy nhầm mặc định thì hậu
 * kiểm sẽ thu hồi điểm đã công bố của học sinh.
 */
class RegradeResultUseCaseTests {

    private GradingActionSupport gradingActionSupport;
    private GradingItemScoreResolver gradingItemScoreResolver;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private ExamSessionRepository examSessionRepository;
    private UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private RegradeResultUseCase useCase;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID responseId = UUID.randomUUID();
    private final UUID paperItemId = UUID.randomUUID();
    private final UUID criterionId = UUID.randomUUID();

    private ExamSession session;
    private ExamCandidateResult recalculated;

    @BeforeEach
    void setUp() {
        gradingActionSupport = mock(GradingActionSupport.class);
        gradingItemScoreResolver = mock(GradingItemScoreResolver.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        examItemCriterionScoreRepository = mock(ExamItemCriterionScoreRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        upsertExamCandidateResultUseCase = mock(UpsertExamCandidateResultUseCase.class);
        useCase = new RegradeResultUseCase(
            gradingActionSupport,
            gradingItemScoreResolver,
            examItemEvaluationRepository,
            examItemCriterionScoreRepository,
            examSessionRepository,
            upsertExamCandidateResultUseCase);

        session = new ExamSession();
        session.setId(sessionId);

        recalculated = new ExamCandidateResult();
        recalculated.setId(candidateResultId);
        recalculated.setSessionId(sessionId);
        recalculated.setTotalScore(new BigDecimal("7.50"));
        recalculated.setStatus(ExamCandidateResultStatus.RELEASED);

        when(examItemEvaluationRepository.findByResponseIdIn(anyCollection())).thenReturn(List.of());
        when(examItemEvaluationRepository.save(any())).thenAnswer(call -> {
            var saved = (ExamItemEvaluation) call.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(upsertExamCandidateResultUseCase.execute(any(), any())).thenReturn(recalculated);
        when(gradingItemScoreResolver.resolve(any(), any(), anyBoolean())).thenReturn(List.of(
            new ResolvedItem(paperItemId, responseId, new BigDecimal("7.50"), "Tốt hơn phần trước",
                List.of(new SubmitGradingCommand.CriterionScoreItem(
                    criterionId, new BigDecimal("8.00"), "Phát âm rõ")))));
    }

    private ExamCandidateResult given(GradingRoundType roundType, ExamCandidateResultStatus status) {
        var current = new ExamCandidateResult();
        current.setId(candidateResultId);
        current.setSessionId(sessionId);
        current.setStatus(status);
        current.setTotalScore(new BigDecimal("6.00"));

        var assignment = ExamGradingAssignment.open(candidateResultId, teacherId, roundType, null,
            current.getTotalScore(), Instant.now(), UUID.randomUUID(), null);
        assignment.setId(assignmentId);
        var context = new GradingContext(assignment, current, session, UUID.randomUUID(), "IELTS Mock");

        when(gradingActionSupport.prepare(assignmentId, GradingOutcome.REGRADED, null)).thenReturn(
            new PreparedAction(context, teacherId, roundType, GradingOutcome.REGRADED, null,
                new ResultStatusHistoryRecorder.Snapshot(
                    candidateResultId, status, current.getTotalScore())));
        return current;
    }

    private SubmitGradingCommand command() {
        return new SubmitGradingCommand(assignmentId, List.of(new SubmitGradingCommand.ItemGrade(
            paperItemId,
            List.of(new SubmitGradingCommand.CriterionScoreItem(
                criterionId, new BigDecimal("8.00"), "Phát âm rõ")),
            "Tốt hơn phần trước")));
    }

    private ExamItemEvaluation captureSavedEvaluation() {
        var captor = ArgumentCaptor.forClass(ExamItemEvaluation.class);
        verify(examItemEvaluationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_write_a_finalized_human_evaluation_for_each_graded_item() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        var saved = captureSavedEvaluation();
        assertThat(saved.getEngineType()).isEqualTo(ExamEvaluationEngineType.HUMAN);
        assertThat(saved.getStatus()).isEqualTo(ExamItemEvaluationStatus.FINALIZED);
        assertThat(saved.getReviewerId()).isEqualTo(teacherId);
        assertThat(saved.getItemScore()).isEqualByComparingTo("7.50");
        assertThat(saved.getPaperItemId()).isEqualTo(paperItemId);
        // Người đã chấm thì không còn gì để chờ — cờ này bật là calculator giữ bài lại.
        assertThat(saved.isRequiresHumanReview()).isFalse();
    }

    @Test
    void should_supersede_every_previous_evaluation_of_the_graded_items() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);
        var aiEvaluation = new ExamItemEvaluation();
        aiEvaluation.setId(UUID.randomUUID());
        aiEvaluation.setResponseId(responseId);
        aiEvaluation.setStatus(ExamItemEvaluationStatus.AUTO_GRADED);
        when(examItemEvaluationRepository.findByResponseIdIn(anyCollection()))
            .thenReturn(List.of(aiEvaluation));

        useCase.execute(command());

        // Calculator chọn bản còn hiệu lực theo TRẠNG THÁI, không ưu tiên HUMAN theo
        // engineType — bỏ bước này là điểm AI cũ vẫn được tính vào tổng.
        assertThat(aiEvaluation.getStatus()).isEqualTo(ExamItemEvaluationStatus.SUPERSEDED);
    }

    /**
     * Màn kết quả của học sinh đọc lại chính bản AI đã SUPERSEDED này để lấy lượt nói và
     * bằng chứng AI (xem {@code ExamItemEvaluationRepository#findLatestAiByResponseId}).
     * Nếu chấm lại mà xoá hoặc làm rỗng các cột đó, học sinh sẽ mất nội dung câu hỏi,
     * audio và transcript ngay khi giáo viên nộp — nên bất biến này phải được khoá ở đây.
     */
    @Test
    void should_only_flip_status_of_the_ai_evaluation_so_its_evidence_survives() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);
        var aiEvaluation = new ExamItemEvaluation();
        aiEvaluation.setId(UUID.randomUUID());
        aiEvaluation.setResponseId(responseId);
        aiEvaluation.setEngineType(ExamEvaluationEngineType.AI_SINGLE);
        aiEvaluation.setStatus(ExamItemEvaluationStatus.AUTO_GRADED);
        aiEvaluation.setOverallConfidence(new BigDecimal("0.82"));
        aiEvaluation.setValidityJson("{\"ruleResults\":[]}");
        aiEvaluation.setFeedbackSummary("AI: phát âm ổn");
        aiEvaluation.setSuggestionsJson("[\"nói chậm lại\"]");
        aiEvaluation.setPromptVersion("v3");
        when(examItemEvaluationRepository.findByResponseIdIn(anyCollection()))
            .thenReturn(List.of(aiEvaluation));

        useCase.execute(command());

        assertThat(aiEvaluation.getEngineType()).isEqualTo(ExamEvaluationEngineType.AI_SINGLE);
        assertThat(aiEvaluation.getOverallConfidence()).isEqualByComparingTo("0.82");
        assertThat(aiEvaluation.getValidityJson()).isEqualTo("{\"ruleResults\":[]}");
        assertThat(aiEvaluation.getFeedbackSummary()).isEqualTo("AI: phát âm ổn");
        assertThat(aiEvaluation.getSuggestionsJson()).isEqualTo("[\"nói chậm lại\"]");
        assertThat(aiEvaluation.getPromptVersion()).isEqualTo("v3");
    }

    @Test
    void should_not_rewrite_an_evaluation_that_is_already_superseded() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);
        var old = new ExamItemEvaluation();
        old.setId(UUID.randomUUID());
        old.setResponseId(responseId);
        old.setStatus(ExamItemEvaluationStatus.SUPERSEDED);
        when(examItemEvaluationRepository.findByResponseIdIn(anyCollection())).thenReturn(List.of(old));

        useCase.execute(command());

        // Chỉ đúng MỘT lần save: bản mới. Bản cũ đã SUPERSEDED thì không ghi lại.
        verify(examItemEvaluationRepository).save(any());
    }

    @Test
    void should_store_the_criterion_scores_against_the_new_evaluation() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        @SuppressWarnings("unchecked")
        var captor = (ArgumentCaptor<List<ExamItemCriterionScore>>) (ArgumentCaptor<?>)
            ArgumentCaptor.forClass(List.class);
        verify(examItemCriterionScoreRepository).saveAll(captor.capture());
        var scores = captor.getValue();
        assertThat(scores).hasSize(1);
        assertThat(scores.get(0).getRubricCriterionId()).isEqualTo(criterionId);
        assertThat(scores.get(0).getRawScore()).isEqualByComparingTo("8.00");
        assertThat(scores.get(0).getFinalScore()).isEqualByComparingTo("8.00");
        assertThat(scores.get(0).getEvaluationId()).isNotNull();
    }

    @Test
    void should_keep_a_spot_checked_result_released_instead_of_taking_the_score_back() {
        given(GradingRoundType.SPOT_CHECK, ExamCandidateResultStatus.RELEASED);

        useCase.execute(command());

        // resultStatusAfter(SPOT_CHECK, REGRADED) = null nghĩa là GIỮ NGUYÊN. Nếu để
        // resolveDefaultStatus tự quyết, bài có item cần soi sẽ bị kéo về
        // PENDING_REVIEW — tức thu hồi điểm đã công bố vì một lý do kỹ thuật.
        verify(upsertExamCandidateResultUseCase).execute(sessionId, ExamCandidateResultStatus.RELEASED);
    }

    @Test
    void should_release_the_result_after_an_initial_round() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        verify(upsertExamCandidateResultUseCase).execute(sessionId, ExamCandidateResultStatus.RELEASED);
    }

    @Test
    void should_release_the_result_after_an_appeal_round() {
        given(GradingRoundType.APPEAL, ExamCandidateResultStatus.RE_GRADING);

        useCase.execute(command());

        verify(upsertExamCandidateResultUseCase).execute(sessionId, ExamCandidateResultStatus.RELEASED);
    }

    @Test
    void should_clear_the_flag_once_a_human_has_looked_at_the_session() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);
        session.setFlagged(true);

        useCase.execute(command());

        assertThat(session.isFlagged()).isFalse();
        verify(examSessionRepository).save(session);
    }

    @Test
    void should_not_touch_a_session_that_was_never_flagged() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    void should_demand_full_coverage_before_closing_the_assignment() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        // Nộp là chốt COMPLETED: thiếu một phần mà vẫn cho nộp thì phần đó khoá cứng.
        verify(gradingItemScoreResolver).resolve(any(), any(), eq(true));
    }

    @Test
    void should_finish_with_the_recalculated_result_not_the_stale_one() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        var response = useCase.execute(command());

        // finish() ghi audit và phát sự kiện dựa trên điểm — đưa bản cũ vào là báo sai
        // điểm cho học sinh.
        verify(gradingActionSupport).finish(any(), eq(recalculated));
        assertThat(response.totalScore()).isEqualByComparingTo("7.50");
        assertThat(response.outcome()).isEqualTo(GradingOutcome.REGRADED.name());
    }

    @Test
    void should_save_criterion_scores_even_when_nothing_needed_superseding() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(command());

        verify(examItemCriterionScoreRepository).saveAll(anyList());
    }
}
