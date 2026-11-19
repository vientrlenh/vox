package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskItemInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamItemCriterionScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamItemEvaluationJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamItemEvaluationTurnJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamItemResponseJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamPaperItemJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamPaperSectionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.RubricCriterionJpaEntity;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Màn chấm dựng bằng chứng của AI (chấm phát âm từng chữ, vi phạm quy tắc, độ tin cậy)
 * từ BẢN AI, không phải bản chấm đang có hiệu lực. Hai chỗ dễ hỏng mà chỉ Postgres thật
 * mới bắt được:
 *
 * <ul>
 *   <li>Sau khi giáo viên nộp điểm, bản hiệu lực là bản HUMAN — bản này không có turn,
 *       không có signals, và rationale là của giáo viên. Lấy nhầm nguồn thì tới vòng
 *       phúc khảo màn chấm trắng dữ liệu, mà mọi test mock vẫn xanh.</li>
 *   <li>{@code orderInSection} đếm dồn theo section trên danh sách đã sắp xếp — sai
 *       {@code ORDER BY} thì số thứ tự câu nhảy lung tung chứ không lỗi.</li>
 * </ul>
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class GradingTaskDetailQueryTests {

    private static final String AI_SIGNALS_JSON = "{\"audioQuality\":0.82,\"audioGateStatus\":\"PASS\"}";
    private static final String AI_VALIDITY_JSON =
        "{\"validForScoring\":true,\"ruleResults\":[{\"ruleId\":\"answer_length.too_short\"}]}";
    private static final String WORD_FEEDBACK_JSON =
        "[{\"word\":\"friends\",\"accuracyScore\":49,\"color\":\"red\"}]";

    @Autowired
    private ExamGradingQueryRepository examGradingQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private UUID assignmentId;
    private UUID criterionId;

    @BeforeEach
    void setUp() {
        UUID paperId = UUID.randomUUID();
        UUID rubricVersionId = UUID.randomUUID();

        criterionId = persist(new RubricCriterionJpaEntity(null, rubricVersionId, UUID.randomUUID(),
            "grammar", "Ngữ pháp", null, null, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN,
            1, true, now, now, null, null)).getId();

        // Part 1 has TWO questions, Part 2 has one -- exactly the shape that makes the tab show
        // "Part 1" twice.
        UUID partOne = persist(new ExamPaperSectionJpaEntity(null, paperId, 1, "Part 1", null,
            null, null, now, now, null, null)).getId();
        UUID partTwo = persist(new ExamPaperSectionJpaEntity(null, paperId, 2, "Part 2", null,
            null, null, now, now, null, null)).getId();
        UUID itemOne = paperItem(paperId, partOne, 1);
        UUID itemTwo = paperItem(paperId, partOne, 2);
        UUID itemThree = paperItem(paperId, partTwo, 1);

        UUID examId = persist(new ExamJpaEntity(null, null, null, "EX-" + UUID.randomUUID(), "Kỳ thi thử",
            null, UUID.randomUUID(), UUID.randomUUID(), "CENTRALIZED", "STUDENT_DEVICE", "CLOSED",
            1, 900, "HIGHEST", null, null, null, null, UUID.randomUUID(), false,
            now, now, null, null)).getId();
        UUID candidateId = UUID.randomUUID();
        UUID sessionId = persist(new ExamSessionJpaEntity(null, examId, candidateId, paperId, now, now,
            "GRADED", false, null)).getId();
        UUID candidateResultId = persist(new ExamCandidateResultJpaEntity(null, examId, candidateId, sessionId,
            UUID.randomUUID(), 1, rubricVersionId, UUID.randomUUID(), UUID.randomUUID(), null,
            new BigDecimal("6.50"), "PENDING_REVIEW", null, null, now, now, null, null)).getId();
        assignmentId = persist(new ExamGradingAssignmentJpaEntity(null, candidateResultId, teacherId,
            "INITIAL", null, "ASSIGNED", null, new BigDecimal("6.50"), now, teacherId, null,
            now.plusSeconds(86_400), null, null, candidateResultId)).getId();

        // Question 1: already graded by hand -- the AI version is SUPERSEDED, the effective version
        // is the HUMAN one.
        UUID responseOne = response(sessionId, itemOne);
        UUID aiEvaluation = evaluation(responseOne, itemOne, "AI_SINGLE", "SUPERSEDED", true);
        turn(aiEvaluation);
        criterionScore(aiEvaluation, "AI thấy thí sinh dùng thì quá khứ chưa nhất quán.");
        UUID humanEvaluation = evaluation(responseOne, itemOne, "HUMAN", "FINALIZED", false);
        criterionScore(humanEvaluation, "Giáo viên chấm lại: ngữ pháp ổn hơn AI đánh giá.");

        // Question 2: only has the AI version so far.
        UUID responseTwo = response(sessionId, itemTwo);
        UUID aiOnly = evaluation(responseTwo, itemTwo, "AI_SINGLE", "AUTO_GRADED", true);
        turn(aiOnly);
        criterionScore(aiOnly, "AI: từ vựng đủ dùng.");

        // Question 3: not graded at all -- the grading screen must still be able to open.
        response(sessionId, itemThree);

        em.flush();
        em.clear();
    }

    @Test
    void should_number_questions_within_their_section() {
        List<GradingTaskItemInfo> items = detail().items();

        assertThat(items).hasSize(3);
        assertThat(items).extracting(item -> item.partLabel())
            .containsExactly("Part 1", "Part 1", "Part 2");
        assertThat(items).extracting(item -> item.orderInSection())
            .containsExactly(1, 2, 1);
        assertThat(items.get(0).sectionId())
            .isEqualTo(items.get(1).sectionId())
            .isNotEqualTo(items.get(2).sectionId());
    }

    @Test
    void should_keep_ai_evidence_after_the_question_has_been_graded_by_a_teacher() {
        GradingTaskItemInfo item = detail().items().get(0);

        assertThat(item.aiScores()).singleElement()
            .satisfies(score -> assertThat(score.rationale())
                .isEqualTo("AI thấy thí sinh dùng thì quá khứ chưa nhất quán."));
        assertThat(item.currentScores()).singleElement()
            .satisfies(score -> assertThat(score.rationale())
                .isEqualTo("Giáo viên chấm lại: ngữ pháp ổn hơn AI đánh giá."));
        assertThat(item.aiSignals()).isEqualTo(AI_SIGNALS_JSON);
        assertThat(item.aiValidity()).isEqualTo(AI_VALIDITY_JSON);
        assertThat(item.aiOverallConfidence()).isEqualByComparingTo("0.42");
        assertThat(item.aiRequiresHumanReview()).isTrue();
        assertThat(item.aiReviewReasonCode()).isEqualTo("LOW_CONFIDENCE");
        assertThat(item.aiFeedbackSummary()).isEqualTo("Bài nói rõ ý nhưng còn lỗi thì.");
    }

    @Test
    void should_expose_word_feedback_json_untouched() {
        assertThat(detail().items().get(0).turns()).singleElement()
            .satisfies(turn -> {
                assertThat(turn.wordFeedback()).isEqualTo(WORD_FEEDBACK_JSON);
                assertThat(turn.wordCount()).isEqualTo(109);
                assertThat(turn.asrConfidence()).isEqualTo(0.93);
                assertThat(turn.transcript()).isEqualTo("my friends helped me");
            });
    }

    @Test
    void should_return_an_openable_item_when_the_question_has_no_ai_evaluation() {
        GradingTaskItemInfo item = detail().items().get(2);

        assertThat(item.turns()).isEmpty();
        assertThat(item.aiScores()).isEmpty();
        assertThat(item.aiSignals()).isNull();
        assertThat(item.aiValidity()).isNull();
        assertThat(item.aiOverallConfidence()).isNull();
        assertThat(item.aiRequiresHumanReview()).isFalse();
        assertThat(item.aiMarkedInvalid()).isFalse();
        assertThat(item.aiRequiresRetake()).isFalse();
    }

    private GradingTaskDetailInfo detail() {
        return examGradingQueryRepository.findTaskDetail(assignmentId, teacherId).orElseThrow();
    }

    private UUID paperItem(UUID paperId, UUID sectionId, int order) {
        return persist(new ExamPaperItemJpaEntity(null, null, sectionId, paperId, UUID.randomUUID(),
            order, BigDecimal.ONE)).getId();
    }

    // Hai entity dưới đây có khoá do ứng dụng gán (cột id insertable), khác với phần
    // còn lại của fixture vốn để Postgres sinh uuidv7() — gán null vào đây sẽ nổ
    // IdentifierGenerationException.
    private UUID response(UUID sessionId, UUID paperItemId) {
        return persist(new ExamItemResponseJpaEntity(UUID.randomUUID(), sessionId, paperItemId,
            "https://example.test/audio.mp3", 60, "my friends helped me", null, now)).getId();
    }

    private UUID evaluation(UUID responseId, UUID paperItemId, String engineType, String status, boolean ai) {
        return persist(new ExamItemEvaluationJpaEntity(null, responseId, paperItemId, engineType,
            ai ? "gpt-test" : "human", null, null, new BigDecimal("6.00"), new BigDecimal("6.50"),
            ai ? new BigDecimal("0.42") : null, ai, ai ? "LOW_CONFIDENCE" : null, false, false,
            ai ? AI_SIGNALS_JSON : null, ai ? AI_VALIDITY_JSON : null,
            ai ? "Bài nói rõ ý nhưng còn lỗi thì." : "Nhận xét của giáo viên.",
            null, null, status, now)).getId();
    }

    private void turn(UUID evaluationId) {
        persist(new ExamItemEvaluationTurnJpaEntity(UUID.randomUUID(), evaluationId, 1, "MAIN", "Tell me about a friend.",
            "https://example.test/turn.mp3", "my friends helped me", 109, 42, 0.93,
            "{\"accuracyScore\":74}", WORD_FEEDBACK_JSON));
    }

    private void criterionScore(UUID evaluationId, String rationale) {
        persist(new ExamItemCriterionScoreJpaEntity(null, evaluationId, criterionId,
            new BigDecimal("6.00"), new BigDecimal("6.30"), rationale));
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        // Flush từng dòng: id do DB sinh (uuidv7() mặc định, cột insertable=false) nên
        // dòng con chỉ lấy được khoá cha sau khi cha thật sự chạm DB.
        em.flush();
        return entity;
    }
}
