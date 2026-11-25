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
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemCriterionScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemEvaluationJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemEvaluationTurnJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemResponseJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperItemJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentPaperSectionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.rubric.RubricCriterionJpaEntity;
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
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;

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

        criterionId = persist(RubricCriterionJpaEntity.builder()
                .rubricVersionId(rubricVersionId)
                .frameworkCriterionId(UUID.randomUUID())
                .code("grammar")
                .name("Ngữ pháp")
                .description(null)
                .examplesJson(null)
                .weight(BigDecimal.ONE)
                .minScore(BigDecimal.ZERO)
                .maxScore(BigDecimal.TEN)
                .order(1)
                .isRequired(true)
                .build()).getId();

        // Part 1 has TWO questions, Part 2 has one -- exactly the shape that makes the tab show
        // "Part 1" twice.
        UUID partOne = persist(AssessmentPaperSectionJpaEntity.builder().paperId(paperId).order(1).title("Part 1").build()).getId();
        UUID partTwo = persist(AssessmentPaperSectionJpaEntity.builder().paperId(paperId).order(2).title("Part 2").build()).getId();
        UUID itemOne = paperItem(paperId, partOne, 1);
        UUID itemTwo = paperItem(paperId, partOne, 2);
        UUID itemThree = paperItem(paperId, partTwo, 1);

        UUID assessmentId = persist(AssessmentJpaEntity.builder()
                .blueprintId(null)
                .blueprintVersionId(null)
                .code("EX-" + UUID.randomUUID())
                .name("Kỳ thi thử")
                .description(null)
                .schoolId(UUID.randomUUID())
                .languageId(UUID.randomUUID())
                .kind("CENTRALIZED")
                .deliveryMode("STUDENT_DEVICE")
                .status("CLOSED")
                .maxAttempt(1)
                .assessmentTimeDurationSecond(900)
                .resultDecisionMethod("HIGHEST")
                .requiredStreamType(null)
                .streamTypePermission(null)
                .openAt(null)
                .closeAt(null)
                .assessmentPolicyId(UUID.randomUUID())
                .requiresOtp(false)
                .build()).getId();
        UUID candidateId = UUID.randomUUID();
        UUID sessionId = persist(AssessmentSessionJpaEntity.builder()
                .id(null)
                .assessmentId(assessmentId)
                .candidateId(candidateId)
                .paperId(paperId)
                .startedAt(now)
                .submittedAt(now)
                .status("GRADED")
                .flagged(false)
                .flagReason(null)
                .build()).getId();
        UUID candidateResultId = persist(AssessmentCandidateResultJpaEntity.builder()
                .assessmentId(assessmentId)
                .candidateId(candidateId)
                .sessionId(sessionId)
                .assessmentPolicyId(UUID.randomUUID())
                .policyVersion(1)
                .rubricVersionId(rubricVersionId)
                .frameworkVersionId(UUID.randomUUID())
                .targetFrameworkBandId(UUID.randomUUID())
                .rubricResultBandId(null)
                .totalScore(new BigDecimal("6.50"))
                .status("PENDING_REVIEW")
                .releasedAt(null)
                .finalizedAt(null)
                .build()).getId();
        assignmentId = persist(AssessmentGradingAssignmentJpaEntity.builder()
                .id(null)
                .candidateResultId(candidateResultId)
                .teacherId(teacherId)
                .roundType("INITIAL")
                .appealId(null)
                .status("ASSIGNED")
                .outcome(null)
                .scoreBefore(new BigDecimal("6.50"))
                .assignedAt(now)
                .assignedBy(teacherId)
                .completedAt(null)
                .deadlineAt(now.plusSeconds(86_400))
                .remindedAt(null)
                .reason(null)
                .activeResultId(candidateResultId)
                .build()).getId();

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
        return assessmentGradingQueryRepository.findTaskDetail(assignmentId, teacherId).orElseThrow();
    }

    private UUID paperItem(UUID paperId, UUID sectionId, int order) {
        return persist(AssessmentPaperItemJpaEntity.builder().sectionId(sectionId).paperId(paperId)
            .questionId(UUID.randomUUID()).order(order).weight(BigDecimal.ONE).build()).getId();
    }

    // The AI service sends the id of a response, and AssignedOrUuidVersion7 keeps it, so this fixture
    // presets an id as production does. A null id would also work: Hibernate would generate a version 7.
    private UUID response(UUID sessionId, UUID paperItemId) {
        return persist(AssessmentItemResponseJpaEntity.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .paperItemId(paperItemId)
                .audioUrl("https://example.test/audio.mp3")
                .durationSeconds(60)
                .transcript("my friends helped me")
                .terminationReason(null)
                .submittedAt(now)
                .build()).getId();
    }

    private UUID evaluation(UUID responseId, UUID paperItemId, String engineType, String status, boolean ai) {
        return persist(AssessmentItemEvaluationJpaEntity.builder()
                .id(null)
                .responseId(responseId)
                .paperItemId(paperItemId)
                .engineType(engineType)
                .gradedByModel(ai ? "gpt-test" : "human")
                .sampleCount(null)
                .reviewerId(null)
                .rawItemScore(new BigDecimal("6.00"))
                .itemScore(new BigDecimal("6.50"))
                .overallConfidence(ai ? new BigDecimal("0.42") : null)
                .requiresHumanReview(ai)
                .reviewReasonCode(ai ? "LOW_CONFIDENCE" : null)
                .markedInvalid(false)
                .requiresRetake(false)
                .signals(ai ? AI_SIGNALS_JSON : null)
                .validityJson(ai ? AI_VALIDITY_JSON : null)
                .feedbackSummary(ai ? "Bài nói rõ ý nhưng còn lỗi thì." : "Nhận xét của giáo viên.")
                .suggestions(null)
                .promptVersion(null)
                .status(status)
                .evaluatedAt(now)
                .build()).getId();
    }

    private void turn(UUID evaluationId) {
        persist(AssessmentItemEvaluationTurnJpaEntity.builder()
                .id(null)
                .evaluationId(evaluationId)
                .turnOrder(1)
                .turnType("MAIN")
                .promptText("Tell me about a friend.")
                .audioUrl("https://example.test/turn.mp3")
                .transcript("my friends helped me")
                .wordCount(109)
                .durationSeconds(42)
                .asrConfidence(0.93)
                .pronunciationOverall("{\"accuracyScore\":74}")
                .wordFeedback(WORD_FEEDBACK_JSON)
                .build());
    }

    private void criterionScore(UUID evaluationId, String rationale) {
        persist(AssessmentItemCriterionScoreJpaEntity.builder()
                .id(null)
                .evaluationId(evaluationId)
                .rubricCriterionId(criterionId)
                .rawScore(new BigDecimal("6.00"))
                .finalScore(new BigDecimal("6.30"))
                .rationale(rationale)
                .build());
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        // Flush từng dòng: id do DB sinh (uuidv7() mặc định, cột insertable=false) nên
        // dòng con chỉ lấy được khoá cha sau khi cha thật sự chạm DB.
        em.flush();
        return entity;
    }
}
