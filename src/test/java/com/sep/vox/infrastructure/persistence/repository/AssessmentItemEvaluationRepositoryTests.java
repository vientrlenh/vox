package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.infrastructure.persistence.adapter.AssessmentItemEvaluationRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

/**
 * {@code findLatestAiByResponseId} intentionally does NOT filter by status -- that is the only difference from
 * {@code findLatestByResponseId}, and also the reason it exists: after a teacher
 * regrades, the AI version carries SUPERSEDED but it is still the only place that has the speaking turns and the AI
 * evidence. Written with Testcontainers because what needs checking is the real JPQL, not a mock.
 */
@RepositoryTest
@Import(AssessmentItemEvaluationRepositoryImpl.class)
class AssessmentItemEvaluationRepositoryTests {

    @Autowired
    private AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;

    @Test
    void should_return_superseded_ai_evaluation_as_the_latest_ai() {
        UUID responseId = UUID.randomUUID();
        AssessmentItemEvaluation ai = assessmentItemEvaluationRepository.save(evaluation(responseId,
            AssessmentEvaluationEngineType.AI_SINGLE, AssessmentItemEvaluationStatus.SUPERSEDED, hoursAgo(2)));
        assessmentItemEvaluationRepository.save(evaluation(responseId,
            AssessmentEvaluationEngineType.HUMAN, AssessmentItemEvaluationStatus.FINALIZED, hoursAgo(1)));

        Optional<AssessmentItemEvaluation> found = assessmentItemEvaluationRepository.findLatestAiByResponseId(responseId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(ai.getId());
        assertThat(found.get().getStatus()).isEqualTo(AssessmentItemEvaluationStatus.SUPERSEDED);
    }

    /**
     * The effective version must still be the manual grade -- the two queries must not get mixed
     * up.
     */
    @Test
    void should_keep_returning_the_human_row_as_the_authoritative_evaluation() {
        UUID responseId = UUID.randomUUID();
        assessmentItemEvaluationRepository.save(evaluation(responseId,
            AssessmentEvaluationEngineType.AI_SINGLE, AssessmentItemEvaluationStatus.SUPERSEDED, hoursAgo(2)));
        AssessmentItemEvaluation human = assessmentItemEvaluationRepository.save(evaluation(responseId,
            AssessmentEvaluationEngineType.HUMAN, AssessmentItemEvaluationStatus.FINALIZED, hoursAgo(1)));

        Optional<AssessmentItemEvaluation> found = assessmentItemEvaluationRepository.findLatestByResponseId(responseId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(human.getId());
    }

    @Test
    void should_prefer_the_newest_ai_evaluation_when_several_exist() {
        UUID responseId = UUID.randomUUID();
        assessmentItemEvaluationRepository.save(evaluation(responseId,
            AssessmentEvaluationEngineType.AI_SINGLE, AssessmentItemEvaluationStatus.SUPERSEDED, hoursAgo(5)));
        AssessmentItemEvaluation newest = assessmentItemEvaluationRepository.save(evaluation(responseId,
            AssessmentEvaluationEngineType.AI_ENSEMBLE, AssessmentItemEvaluationStatus.AUTO_GRADED, hoursAgo(1)));

        Optional<AssessmentItemEvaluation> found = assessmentItemEvaluationRepository.findLatestAiByResponseId(responseId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(newest.getId());
    }

    /**
     * UNDER_REVIEW is an appeal report that has NOT been published. Normally it carries the HUMAN
     * engine so it does not fall into this query, but the filter is written explicitly so that if
     * someone later writes a review version with the AI engine, the student still does not see an
     * unpublished score.
     */
    @Test
    void should_ignore_an_under_review_ai_row() {
        UUID responseId = UUID.randomUUID();
        AssessmentItemEvaluation published = assessmentItemEvaluationRepository.save(evaluation(responseId,
            AssessmentEvaluationEngineType.AI_SINGLE, AssessmentItemEvaluationStatus.SUPERSEDED, hoursAgo(3)));
        assessmentItemEvaluationRepository.save(evaluation(responseId,
            AssessmentEvaluationEngineType.AI_SINGLE, AssessmentItemEvaluationStatus.UNDER_REVIEW, hoursAgo(1)));

        Optional<AssessmentItemEvaluation> found = assessmentItemEvaluationRepository.findLatestAiByResponseId(responseId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(published.getId());
    }

    @Test
    void should_return_empty_when_the_response_only_has_a_human_evaluation() {
        UUID responseId = UUID.randomUUID();
        assessmentItemEvaluationRepository.save(evaluation(responseId,
            AssessmentEvaluationEngineType.HUMAN, AssessmentItemEvaluationStatus.FINALIZED, hoursAgo(1)));

        assertThat(assessmentItemEvaluationRepository.findLatestAiByResponseId(responseId)).isEmpty();
    }

    @Test
    void should_not_leak_an_ai_evaluation_of_another_response() {
        UUID responseId = UUID.randomUUID();
        assessmentItemEvaluationRepository.save(evaluation(UUID.randomUUID(),
            AssessmentEvaluationEngineType.AI_SINGLE, AssessmentItemEvaluationStatus.AUTO_GRADED, hoursAgo(1)));

        assertThat(assessmentItemEvaluationRepository.findLatestAiByResponseId(responseId)).isEmpty();
    }

    private static Instant hoursAgo(int hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS);
    }

    private static AssessmentItemEvaluation evaluation(
            UUID responseId,
            AssessmentEvaluationEngineType engineType,
            AssessmentItemEvaluationStatus status,
            Instant evaluatedAt) {
        return AssessmentItemEvaluation.builder()
                .id(null)
                .responseId(responseId)
                .paperItemId(UUID.randomUUID())
                .engineType(engineType)
                .gradedByModel(engineType == AssessmentEvaluationEngineType.HUMAN ? "HUMAN" : "gpt-x")
                .sampleCount(null)
                .reviewerId(null)
                .rawItemScore(new BigDecimal("6.00"))
                .itemScore(new BigDecimal("6.00"))
                .overallConfidence(null)
                .requiresHumanReview(false)
                .reviewReasonCode(null)
                .markedInvalid(false)
                .requiresRetake(false)
                .signals(null)
                .validityJson(null)
                .feedbackSummary(null)
                .suggestionsJson(null)
                .promptVersion(null)
                .status(status)
                .evaluatedAt(evaluatedAt)
                .build();
    }
}
