package com.sep.vox.application.port.input.service;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.command.examevaluation.CriterionScoreInput;
import com.sep.vox.application.port.input.command.examevaluation.EvaluationSignalsInput;
import com.sep.vox.application.port.input.command.examevaluation.TurnDetailInput;
import com.sep.vox.domain.model.personalization.WeaknessObservation;
import com.sep.vox.domain.model.personalization.WeaknessObservationSourceType;
import com.sep.vox.domain.model.rubric.RubricCriterion;

@Service
public class WeaknessObservationDerivationService {

    private static final int MAX_EVIDENCE_LENGTH = 200;

    private final double pronunciationAccuracyThreshold;
    private final double fluencySlowRateWordsPerMinute;
    private final double fluencyLongPauseRatio;

    public WeaknessObservationDerivationService(
            @Value("${app.personalization.pronunciation-accuracy-threshold:60}")
            double pronunciationAccuracyThreshold,
            @Value("${app.personalization.fluency-slow-rate-words-per-minute:60}")
            double fluencySlowRateWordsPerMinute,
            @Value("${app.personalization.fluency-long-pause-ratio:0.35}")
            double fluencyLongPauseRatio) {
        this.pronunciationAccuracyThreshold = pronunciationAccuracyThreshold;
        this.fluencySlowRateWordsPerMinute = fluencySlowRateWordsPerMinute;
        this.fluencyLongPauseRatio = fluencyLongPauseRatio;
    }

    public List<WeaknessObservation> derive(
            UUID studentId,
            UUID evaluationId,
            OffsetDateTime observedAt,
            boolean markedInvalid,
            boolean candidateBlocked,
            Map<String, CriterionScoreInput> criteria,
            Map<String, RubricCriterion> rubricCriteriaByCode,
            List<TurnDetailInput> turns,
            EvaluationSignalsInput signals) {
        if (markedInvalid || candidateBlocked || studentId == null || evaluationId == null) {
            return List.of();
        }

        var observations = new ArrayList<WeaknessObservation>();
        var safeCriteria = criteria == null ? Map.<String, CriterionScoreInput>of() : criteria;
        for (var entry : safeCriteria.entrySet()) {
            var score = entry.getValue();
            if (!hasScoredEvidence(score)) {
                continue;
            }
            var criterion = rubricCriteriaByCode.get(normalizeCode(entry.getKey()));
            if (criterion == null) {
                continue;
            }
            var labels = score.weaknessLabels() == null ? List.<String>of() : score.weaknessLabels();
            var spans = score.evidenceSpans() == null ? List.<String>of() : score.evidenceSpans();
            for (int index = 0; index < labels.size(); index++) {
                var label = labels.get(index);
                if (label == null || label.isBlank()) {
                    continue;
                }
                var evidence = index < spans.size() ? spans.get(index) : "";
                observations.add(observation(
                    studentId,
                    evaluationId,
                    criterion,
                    entry.getKey(),
                    label,
                    evidence,
                    observedAt
                ));
            }
        }

        derivePronunciation(
            observations,
            studentId,
            evaluationId,
            observedAt,
            safeCriteria,
            rubricCriteriaByCode,
            turns
        );
        deriveFluency(
            observations,
            studentId,
            evaluationId,
            observedAt,
            safeCriteria,
            rubricCriteriaByCode,
            signals
        );
        return observations;
    }

    private void derivePronunciation(
            List<WeaknessObservation> observations,
            UUID studentId,
            UUID evaluationId,
            OffsetDateTime observedAt,
            Map<String, CriterionScoreInput> criteria,
            Map<String, RubricCriterion> rubricCriteriaByCode,
            List<TurnDetailInput> turns) {
        var criterionEntry = findCriterionEntry(criteria, rubricCriteriaByCode, "pronunciation");
        if (criterionEntry == null || !hasScoredEvidence(criterionEntry.score())) {
            return;
        }
        for (var turn : turns == null ? List.<TurnDetailInput>of() : turns) {
            for (var word : turn.wordFeedback() == null ? List.<com.sep.vox.application.port.input.command.examevaluation.WordFeedbackInput>of() : turn.wordFeedback()) {
                var wordNeedsReview = Boolean.TRUE.equals(word.hasCriticalIssue())
                    || below(word.accuracyScore(), pronunciationAccuracyThreshold);
                if (!wordNeedsReview) {
                    continue;
                }
                for (var phoneme : word.phonemes() == null ? List.<com.sep.vox.application.port.input.command.examevaluation.PhonemeFeedbackInput>of() : word.phonemes()) {
                    if (!below(phoneme.accuracyScore(), pronunciationAccuracyThreshold)) {
                        continue;
                    }
                    var normalized = normalizePhoneme(phoneme.phoneme());
                    if (normalized.isBlank()) {
                        continue;
                    }
                    observations.add(observation(
                        studentId,
                        evaluationId,
                        criterionEntry.criterion(),
                        criterionEntry.key(),
                        "phoneme_" + normalized,
                        word.word(),
                        observedAt
                    ));
                }
            }
        }
    }

    private void deriveFluency(
            List<WeaknessObservation> observations,
            UUID studentId,
            UUID evaluationId,
            OffsetDateTime observedAt,
            Map<String, CriterionScoreInput> criteria,
            Map<String, RubricCriterion> rubricCriteriaByCode,
            EvaluationSignalsInput signals) {
        if (signals == null) {
            return;
        }
        var criterionEntry = findCriterionEntry(criteria, rubricCriteriaByCode, "fluency");
        if (criterionEntry == null || !hasScoredEvidence(criterionEntry.score())) {
            return;
        }
        if (below(signals.speechRate(), fluencySlowRateWordsPerMinute)) {
            observations.add(observation(
                studentId,
                evaluationId,
                criterionEntry.criterion(),
                criterionEntry.key(),
                "slow_rate",
                "",
                observedAt
            ));
        }
        if (signals.silenceRatio() != null && signals.silenceRatio() > fluencyLongPauseRatio) {
            observations.add(observation(
                studentId,
                evaluationId,
                criterionEntry.criterion(),
                criterionEntry.key(),
                "long_pause",
                "",
                observedAt
            ));
        }
    }

    private CriterionEntry findCriterionEntry(
            Map<String, CriterionScoreInput> criteria,
            Map<String, RubricCriterion> rubricCriteriaByCode,
            String expectedKey) {
        return criteria.entrySet().stream()
            .filter(entry -> normalizeCode(entry.getKey()).equals(normalizeCode(expectedKey)))
            .map(entry -> {
                var criterion = rubricCriteriaByCode.get(normalizeCode(entry.getKey()));
                return criterion == null ? null : new CriterionEntry(entry.getKey(), entry.getValue(), criterion);
            })
            .filter(entry -> entry != null)
            .findFirst()
            .orElse(null);
    }

    private WeaknessObservation observation(
            UUID studentId,
            UUID evaluationId,
            RubricCriterion criterion,
            String criterionCode,
            String subAttribute,
            String evidence,
            OffsetDateTime observedAt) {
        return new WeaknessObservation(
            studentId,
            WeaknessObservationSourceType.EXAM,
            evaluationId,
            criterion.getFrameworkCriterionId(),
            truncate(criterionCode, 32),
            truncate(subAttribute, 64),
            truncate(evidence, MAX_EVIDENCE_LENGTH),
            observedAt
        );
    }

    private boolean hasScoredEvidence(CriterionScoreInput score) {
        if (score == null) {
            return false;
        }
        var status = score.status() == null ? "" : score.status().trim().toLowerCase(Locale.ROOT);
        return !"zeroed".equals(status) && !"not_scored".equals(status);
    }

    private boolean below(Double value, double threshold) {
        return value != null && value < threshold;
    }

    private String normalizePhoneme(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", "_")
            .replaceAll("^_+|_+$", "");
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        var safe = value == null ? "" : value;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private record CriterionEntry(
        String key,
        CriterionScoreInput score,
        RubricCriterion criterion
    ) {
    }
}
