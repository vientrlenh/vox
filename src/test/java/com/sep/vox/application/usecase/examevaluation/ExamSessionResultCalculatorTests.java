package com.sep.vox.application.usecase.examevaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;

/**
 * Khoá lại công thức tổng sau khi tách {@code rollUp}: {@code calculate} phải giữ
 * nguyên hành vi cho luồng AI và phúc khảo, còn {@code preview} phải cho ra đúng
 * con số mà {@code calculate} sẽ cho nếu bộ điểm kia được ghi thật.
 */
public class ExamSessionResultCalculatorTests {

    private ExamSessionRepository examSessionRepository;
    private ExamRepository examRepository;
    private AssessmentPolicyRepository assessmentPolicyRepository;
    private ExamItemResponseRepository examItemResponseRepository;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private ExamPaperSectionRepository examPaperSectionRepository;
    private RubricResultBandRepository rubricResultBandRepository;
    private FrameworkResultBandRepository frameworkResultBandRepository;
    private ExamSessionResultCalculator calculator;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();
    private final UUID speakingSectionId = UUID.randomUUID();
    private final UUID listeningSectionId = UUID.randomUUID();
    private final UUID speakingItemId = UUID.randomUUID();
    private final UUID listeningItemId = UUID.randomUUID();
    private final UUID speakingResponseId = UUID.randomUUID();
    private final UUID listeningResponseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examSessionRepository = mock(ExamSessionRepository.class);
        examRepository = mock(ExamRepository.class);
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        examPaperItemRepository = mock(ExamPaperItemRepository.class);
        examPaperSectionRepository = mock(ExamPaperSectionRepository.class);
        rubricResultBandRepository = mock(RubricResultBandRepository.class);
        frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        calculator = new ExamSessionResultCalculator(
            examSessionRepository, examRepository, assessmentPolicyRepository, examItemResponseRepository,
            examItemEvaluationRepository, examPaperItemRepository, examPaperSectionRepository,
            rubricResultBandRepository, frameworkResultBandRepository);

        var session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        session.setPaperId(paperId);
        session.setCandidateId(candidateId);
        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        var exam = new Exam();
        exam.setId(examId);
        exam.setAssessmentPolicyId(policyId);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        var policy = new AssessmentPolicy();
        policy.setId(policyId);
        policy.setRubricVersionId(rubricVersionId);
        when(assessmentPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));

        // Nói nặng hơn nghe (0.60 / 0.40) — nếu công thức lùi về trung bình cộng thì
        // trọng số này là thứ để lộ ra ngay.
        when(examPaperSectionRepository.findByPaperId(paperId)).thenReturn(List.of(
            section(listeningSectionId, 2, "Listening", new BigDecimal("0.40")),
            section(speakingSectionId, 1, "Speaking", new BigDecimal("0.60"))));
        when(examPaperItemRepository.findByPaperId(paperId)).thenReturn(List.of(
            new ExamPaperItem(speakingItemId, null, speakingSectionId, paperId, null, 1, new BigDecimal("1.00")),
            new ExamPaperItem(listeningItemId, null, listeningSectionId, paperId, null, 1, new BigDecimal("1.00"))));
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of(
            new ExamItemResponse(speakingResponseId, sessionId, speakingItemId, null, null, null, null, null),
            new ExamItemResponse(listeningResponseId, sessionId, listeningItemId, null, null, null, null, null)));

        when(rubricResultBandRepository.findByRubricVersionId(rubricVersionId)).thenReturn(List.of(
            band("B1", "Trung cấp", "0.00", "6.49", 1),
            band("B2", "Trung cao cấp", "6.50", "7.99", 2),
            band("C1", "Cao cấp", "8.00", "9.00", 3)));
    }

    private ExamPaperSection section(UUID id, int order, String title, BigDecimal weight) {
        var section = new ExamPaperSection();
        section.setId(id);
        section.setPaperId(paperId);
        section.setOrder(order);
        section.setTitle(title);
        section.setWeight(weight);
        return section;
    }

    private RubricResultBand band(String code, String name, String min, String max, int order) {
        var resultBand = new RubricResultBand();
        resultBand.setId(UUID.randomUUID());
        resultBand.setRubricVersionId(rubricVersionId);
        resultBand.setCode(code);
        resultBand.setName(name);
        resultBand.setScoreMin(new BigDecimal(min));
        resultBand.setScoreMax(new BigDecimal(max));
        resultBand.setOrder(order);
        return resultBand;
    }

    private ExamItemEvaluation evaluation(UUID responseId, String itemScore, boolean requiresHumanReview) {
        var evaluation = new ExamItemEvaluation();
        evaluation.setId(UUID.randomUUID());
        evaluation.setResponseId(responseId);
        evaluation.setItemScore(new BigDecimal(itemScore));
        evaluation.setRequiresHumanReview(requiresHumanReview);
        return evaluation;
    }

    private void givenStoredScores(String speaking, String listening) {
        when(examItemEvaluationRepository.findLatestByResponseIdIn(anyCollection())).thenReturn(List.of(
            evaluation(speakingResponseId, speaking, false),
            evaluation(listeningResponseId, listening, false)));
    }

    @Test
    void should_weight_items_into_sections_then_sections_into_total() {
        givenStoredScores("6.00", "8.00");

        var result = calculator.calculate(sessionId);

        // Speaking 6.00*1.00 = 6.00; Listening 8.00*1.00 = 8.00.
        // Tổng = (6.00*0.60 + 8.00*0.40) / 1.00 = 6.80 — trung bình cộng sẽ ra 7.00.
        assertThat(result.totalScore()).isEqualByComparingTo("6.80");
        assertThat(result.sections()).extracting(ExamSessionResultCalculator.SectionScore::score)
            .containsExactly(new BigDecimal("6.00"), new BigDecimal("8.00"));
    }

    @Test
    void should_order_sections_by_their_declared_order() {
        givenStoredScores("6.00", "8.00");

        var result = calculator.calculate(sessionId);

        assertThat(result.sections()).extracting(ExamSessionResultCalculator.SectionScore::title)
            .containsExactly("Speaking", "Listening");
    }

    @Test
    void should_resolve_result_band_from_the_total() {
        givenStoredScores("6.00", "8.00");

        assertThat(calculator.calculate(sessionId).rubricResultBand().getCode()).isEqualTo("B2");
    }

    @Test
    void should_report_when_any_item_still_requires_human_review() {
        when(examItemEvaluationRepository.findLatestByResponseIdIn(anyCollection())).thenReturn(List.of(
            evaluation(speakingResponseId, "6.00", true),
            evaluation(listeningResponseId, "8.00", false)));

        assertThat(calculator.calculate(sessionId).anyRequiresHumanReview()).isTrue();
    }

    @Test
    void should_reject_when_a_response_has_no_evaluation() {
        when(examItemEvaluationRepository.findLatestByResponseIdIn(anyCollection())).thenReturn(List.of(
            evaluation(speakingResponseId, "6.00", false)));

        assertThatThrownBy(() -> calculator.calculate(sessionId))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("evaluation");
    }

    // ---- preview -----------------------------------------------------------

    @Test
    void should_preview_the_same_total_that_calculate_returns_when_nothing_is_overridden() {
        givenStoredScores("6.00", "8.00");

        assertThat(calculator.preview(sessionId, Map.of()).totalScore())
            .isEqualByComparingTo(calculator.calculate(sessionId).totalScore());
    }

    @Test
    void should_preview_the_total_that_calculate_would_return_for_the_overridden_scores() {
        givenStoredScores("6.00", "8.00");
        var previewed = calculator.preview(sessionId, Map.of(
            speakingResponseId, new BigDecimal("9.00"),
            listeningResponseId, new BigDecimal("7.00")));

        // Đây là bài kiểm quan trọng nhất của endpoint preview: con số giáo viên thấy
        // trước khi bấm Nộp phải bằng con số học sinh nhận sau khi nộp.
        givenStoredScores("9.00", "7.00");
        assertThat(previewed.totalScore()).isEqualByComparingTo(calculator.calculate(sessionId).totalScore());
        assertThat(previewed.totalScore()).isEqualByComparingTo("8.20");
    }

    @Test
    void should_fall_back_to_stored_scores_for_parts_not_yet_graded() {
        givenStoredScores("6.00", "8.00");

        // Chấm dở: chỉ nhập Speaking, Listening giữ nguyên điểm AI 8.00.
        var previewed = calculator.preview(sessionId, Map.of(speakingResponseId, new BigDecimal("9.00")));

        assertThat(previewed.totalScore()).isEqualByComparingTo("8.60");
    }

    @Test
    void should_report_the_result_band_name_for_the_previewed_total() {
        givenStoredScores("6.00", "8.00");

        var previewed = calculator.preview(sessionId, Map.of(
            speakingResponseId, new BigDecimal("9.00"),
            listeningResponseId, new BigDecimal("9.00")));

        assertThat(previewed.resultBandName()).isEqualTo("Cao cấp");
    }

    @Test
    void should_expose_previewed_item_scores_per_paper_item() {
        givenStoredScores("6.00", "8.00");

        var previewed = calculator.preview(sessionId, Map.of(speakingResponseId, new BigDecimal("9.00")));

        assertThat(previewed.items()).extracting(ExamSessionResultCalculator.ItemScore::paperItemId)
            .containsExactly(speakingItemId, listeningItemId);
        assertThat(previewed.items()).extracting(ExamSessionResultCalculator.ItemScore::itemScore)
            .containsExactly(new BigDecimal("9.00"), new BigDecimal("8.00"));
    }
}
