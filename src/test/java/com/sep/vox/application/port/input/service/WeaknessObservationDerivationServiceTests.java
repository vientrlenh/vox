package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.examevaluation.CriterionScoreInput;
import com.sep.vox.application.port.input.command.examevaluation.EvaluationSignalsInput;
import com.sep.vox.application.port.input.command.examevaluation.PhonemeFeedbackInput;
import com.sep.vox.application.port.input.command.examevaluation.TurnDetailInput;
import com.sep.vox.application.port.input.command.examevaluation.WordFeedbackInput;
import com.sep.vox.domain.model.rubric.RubricCriterion;

class WeaknessObservationDerivationServiceTests {

    private WeaknessObservationDerivationService service;
    private UUID studentId;
    private UUID evaluationId;
    private OffsetDateTime observedAt;
    private RubricCriterion grammar;
    private RubricCriterion pronunciation;
    private RubricCriterion fluency;

    @BeforeEach
    void setUp() {
        service = new WeaknessObservationDerivationService(60, 60, 0.35);
        studentId = UUID.randomUUID();
        evaluationId = UUID.randomUUID();
        observedAt = OffsetDateTime.now();
        grammar = criterion("GRAMMAR");
        pronunciation = criterion("PRONUNCIATION");
        fluency = criterion("FLUENCY");
    }

    @Test
    void should_derive_llm_pronunciation_and_fluency_observations() {
        var criteria = Map.of(
            "grammar", scored(List.of("tense_control"), List.of("I go yesterday")),
            "pronunciation", scored(List.of(), List.of()),
            "fluency", scored(List.of(), List.of())
        );
        var rubrics = Map.of(
            "grammar", grammar,
            "pronunciation", pronunciation,
            "fluency", fluency
        );
        var words = List.of(
            word("cats", 55, "s", 40),
            word("books", 55, "s", 35)
        );
        var turns = List.of(new TurnDetailInput(
            null, 1, "MAIN", null, null, "", 0, 0, null, null, words
        ));
        var signals = new EvaluationSignalsInput(
            null, null, null, null, null, null, null, null, null,
            55.0, null, 0.40, null, null, null
        );

        var observations = service.derive(
            studentId, evaluationId, observedAt, false, false,
            criteria, rubrics, turns, signals
        );

        assertThat(observations)
            .extracting(item -> item.subAttribute() + ":" + item.evidenceSpan())
            .containsExactlyInAnyOrder(
                "tense_control:I go yesterday",
                "phoneme_s:cats",
                "phoneme_s:books",
                "slow_rate:",
                "long_pause:"
            );
        assertThat(observations)
            .allMatch(item -> item.studentId().equals(studentId))
            .allMatch(item -> item.sourceEvaluationId().equals(evaluationId));
    }

    @Test
    void should_skip_all_observations_for_invalid_or_blocked_candidates() {
        var criteria = Map.of("grammar", scored(List.of("tense_control"), List.of("evidence")));
        var rubrics = Map.of("grammar", grammar);

        assertThat(service.derive(
            studentId, evaluationId, observedAt, true, false,
            criteria, rubrics, List.of(), null
        )).isEmpty();
        assertThat(service.derive(
            studentId, evaluationId, observedAt, false, true,
            criteria, rubrics, List.of(), null
        )).isEmpty();
    }

    @Test
    void should_skip_zeroed_and_not_scored_criteria() {
        var criteria = Map.of(
            "grammar", scoreWithStatus("zeroed", List.of("tense_control")),
            "pronunciation", scoreWithStatus("not_scored", List.of()),
            "fluency", scoreWithStatus("zeroed", List.of())
        );
        var rubrics = Map.of(
            "grammar", grammar,
            "pronunciation", pronunciation,
            "fluency", fluency
        );

        var observations = service.derive(
            studentId, evaluationId, observedAt, false, false,
            criteria, rubrics, List.of(), null
        );

        assertThat(observations).isEmpty();
    }

    private RubricCriterion criterion(String code) {
        var criterion = new RubricCriterion();
        criterion.setId(UUID.randomUUID());
        criterion.setFrameworkCriterionId(UUID.randomUUID());
        criterion.setCode(code);
        return criterion;
    }

    private CriterionScoreInput scored(List<String> labels, List<String> spans) {
        return new CriterionScoreInput(
            70.0, "fair", "scored", "llm", Map.of(), "", "",
            labels, spans, "", "BAC_3"
        );
    }

    private CriterionScoreInput scoreWithStatus(String status, List<String> labels) {
        return new CriterionScoreInput(
            0.0, "not_scored", status, "system", Map.of(), "", "",
            labels, List.of(), "", ""
        );
    }

    private WordFeedbackInput word(String word, double wordScore, String phoneme, double phonemeScore) {
        return new WordFeedbackInput(
            word,
            wordScore,
            false,
            List.of(new PhonemeFeedbackInput(phoneme, phonemeScore))
        );
    }
}
