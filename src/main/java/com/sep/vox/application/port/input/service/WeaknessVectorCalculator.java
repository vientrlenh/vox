package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.personalization.LearnerWeaknessSnapshot;
import com.sep.vox.domain.model.personalization.SubAttributePriority;
import com.sep.vox.domain.model.personalization.WeaknessFrequency;
import com.sep.vox.domain.model.personalization.WeaknessScoreObservation;

@Service
public class WeaknessVectorCalculator {

    public CalculationResult calculate(
            List<WeaknessScoreObservation> scores,
            Set<UUID> targetStudentIds,
            List<WeaknessFrequency> frequencies,
            OffsetDateTime computedAt,
            WeaknessVectorSettings settings) {
        var relatives = centerScores(scores, settings.minimumCriteriaPerEvaluation());
        var snapshots = calculateSnapshots(relatives, targetStudentIds, computedAt, settings);
        var priorities = calculatePriorities(frequencies, snapshots, computedAt, settings);
        return new CalculationResult(snapshots, priorities, relatives);
    }

    List<RelativeObservation> centerScores(
            List<WeaknessScoreObservation> scores,
            int minimumCriteriaPerEvaluation) {
        var byEvaluation = scores.stream().collect(Collectors.groupingBy(
            score -> new EvaluationKey(score.sourceType(), score.evaluationId()),
            LinkedHashMap::new,
            Collectors.toList()
        ));
        var relatives = new ArrayList<RelativeObservation>();

        for (var evaluationScores : byEvaluation.values()) {
            var normalized = evaluationScores.stream()
                .filter(this::hasValidScale)
                .map(score -> new NormalizedScore(score, normalize(score)))
                .toList();
            if (normalized.size() < minimumCriteriaPerEvaluation) {
                continue;
            }
            var center = normalized.stream()
                .mapToDouble(NormalizedScore::value)
                .average()
                .orElseThrow();
            for (var score : normalized) {
                var source = score.source();
                relatives.add(new RelativeObservation(
                    source.studentId(),
                    source.frameworkCriterionId(),
                    source.criterionCode().toUpperCase(Locale.ROOT),
                    score.value() - center,
                    source.evaluatedAt(),
                    source.sourceType(),
                    source.evaluationId(),
                    source.schoolClassId(),
                    source.schoolGradeId()
                ));
            }
        }
        return relatives;
    }

    double shrinkageWeight(String criterionCode, int observationCount, WeaknessVectorSettings settings) {
        var k = settings.shrinkageFor(criterionCode);
        return observationCount / (observationCount + k);
    }

    private List<LearnerWeaknessSnapshot> calculateSnapshots(
            List<RelativeObservation> relatives,
            Set<UUID> targetStudentIds,
            OffsetDateTime computedAt,
            WeaknessVectorSettings settings) {
        var classPriors = priorsByCohort(
            relatives,
            RelativeObservation::schoolClassId,
            settings.minimumClassPriorStudents()
        );
        var gradePriors = priorsByCohort(relatives, RelativeObservation::schoolGradeId, 1);
        var byStudentCriterion = relatives.stream()
            .filter(item -> targetStudentIds.contains(item.studentId()))
            .collect(Collectors.groupingBy(
                item -> new StudentCriterionKey(item.studentId(), item.frameworkCriterionId()),
                LinkedHashMap::new,
                Collectors.toList()
            ));
        var snapshots = new ArrayList<LearnerWeaknessSnapshot>();

        for (var entry : byStudentCriterion.entrySet()) {
            var ordered = entry.getValue().stream()
                .sorted(Comparator.comparing(RelativeObservation::evaluatedAt)
                    .thenComparing(RelativeObservation::sourceType)
                    .thenComparing(RelativeObservation::evaluationId))
                .toList();
            var ema = ordered.getFirst().relativeScore();
            for (var index = 1; index < ordered.size(); index++) {
                ema = settings.alpha() * ordered.get(index).relativeScore()
                    + (1.0 - settings.alpha()) * ema;
            }
            var latest = ordered.getLast();
            var prior = priorFor(latest, classPriors, gradePriors);
            var weight = shrinkageWeight(latest.criterionCode(), ordered.size(), settings);
            var estimate = weight * ema + (1.0 - weight) * prior;
            snapshots.add(new LearnerWeaknessSnapshot(
                stableId(
                    "weakness-snapshot",
                    entry.getKey().studentId(),
                    entry.getKey().frameworkCriterionId()
                ),
                entry.getKey().studentId(),
                entry.getKey().frameworkCriterionId(),
                decimal(estimate),
                decimal(-estimate),
                ordered.size(),
                ordered.size() >= settings.reliableObservationCount(),
                computedAt
            ));
        }
        return snapshots;
    }

    private Map<CohortCriterionKey, Double> priorsByCohort(
            List<RelativeObservation> relatives,
            Function<RelativeObservation, UUID> cohortSelector,
            int minimumStudents) {
        var grouped = relatives.stream()
            .filter(item -> cohortSelector.apply(item) != null)
            .collect(Collectors.groupingBy(
                item -> new CohortCriterionKey(
                    cohortSelector.apply(item),
                    item.frameworkCriterionId()
                )
            ));
        var priors = new HashMap<CohortCriterionKey, Double>();
        for (var entry : grouped.entrySet()) {
            var distinctStudents = entry.getValue().stream()
                .map(RelativeObservation::studentId)
                .collect(Collectors.toCollection(HashSet::new))
                .size();
            if (distinctStudents >= minimumStudents) {
                var meanByStudent = entry.getValue().stream()
                    .collect(Collectors.groupingBy(RelativeObservation::studentId))
                    .values().stream()
                    .mapToDouble(studentValues -> studentValues.stream()
                        .mapToDouble(RelativeObservation::relativeScore)
                        .average()
                        .orElse(0.0))
                    .average()
                    .orElse(0.0);
                priors.put(entry.getKey(), meanByStudent);
            }
        }
        return priors;
    }

    private double priorFor(
            RelativeObservation latest,
            Map<CohortCriterionKey, Double> classPriors,
            Map<CohortCriterionKey, Double> gradePriors) {
        if (latest.schoolClassId() != null) {
            var classPrior = classPriors.get(
                new CohortCriterionKey(latest.schoolClassId(), latest.frameworkCriterionId())
            );
            if (classPrior != null) {
                return classPrior;
            }
        }
        if (latest.schoolGradeId() != null) {
            return gradePriors.getOrDefault(
                new CohortCriterionKey(latest.schoolGradeId(), latest.frameworkCriterionId()),
                0.0
            );
        }
        return 0.0;
    }

    private List<SubAttributePriority> calculatePriorities(
            List<WeaknessFrequency> frequencies,
            List<LearnerWeaknessSnapshot> snapshots,
            OffsetDateTime computedAt,
            WeaknessVectorSettings settings) {
        var snapshotByKey = snapshots.stream().collect(Collectors.toMap(
            item -> new StudentCriterionKey(item.studentId(), item.frameworkCriterionId()),
            Function.identity()
        ));
        var eligible = frequencies.stream()
            .filter(item -> item.frequency() >= settings.minimumSubAttributeFrequency())
            .toList();
        var maxima = eligible.stream().collect(Collectors.groupingBy(
            item -> new StudentCriterionKey(item.studentId(), item.frameworkCriterionId()),
            Collectors.collectingAndThen(
                Collectors.toList(),
                values -> new FrequencyMax(
                    values.stream().mapToInt(WeaknessFrequency::frequency).max().orElse(1),
                    values.stream().mapToInt(WeaknessFrequency::recentFrequency).max().orElse(0)
                )
            )
        ));
        var priorities = new ArrayList<SubAttributePriority>();
        for (var item : eligible) {
            var key = new StudentCriterionKey(item.studentId(), item.frameworkCriterionId());
            var snapshot = snapshotByKey.get(key);
            if (snapshot == null) {
                continue;
            }
            var max = maxima.get(key);
            var normalizedFrequency = (double) item.frequency() / max.frequency();
            var normalizedRecent = max.recentFrequency() == 0
                ? 0.0
                : (double) item.recentFrequency() / max.recentFrequency();
            var weightedFrequency = settings.frequencyWeight() * normalizedFrequency
                + settings.recentFrequencyWeight() * normalizedRecent;
            var priority = weightedFrequency * snapshot.weakness().doubleValue();
            priorities.add(new SubAttributePriority(
                stableId(
                    "sub-attribute-priority",
                    item.studentId(),
                    item.frameworkCriterionId(),
                    item.subAttribute()
                ),
                item.studentId(),
                item.frameworkCriterionId(),
                item.subAttribute(),
                item.frequency(),
                item.recentFrequency(),
                decimal(priority),
                false,
                computedAt
            ));
        }
        return priorities;
    }

    private boolean hasValidScale(WeaknessScoreObservation score) {
        return score.finalScore() != null
            && score.minScore() != null
            && score.maxScore() != null
            && score.maxScore().compareTo(score.minScore()) > 0;
    }

    private double normalize(WeaknessScoreObservation score) {
        return score.finalScore().subtract(score.minScore())
            .divide(score.maxScore().subtract(score.minScore()), 12, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private UUID stableId(String namespace, Object... parts) {
        var value = namespace + ":" + java.util.Arrays.stream(parts)
            .map(String::valueOf)
            .collect(Collectors.joining(":"));
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    public record CalculationResult(
        List<LearnerWeaknessSnapshot> snapshots,
        List<SubAttributePriority> priorities,
        List<RelativeObservation> relativeObservations
    ) {
    }

    record RelativeObservation(
        UUID studentId,
        UUID frameworkCriterionId,
        String criterionCode,
        double relativeScore,
        OffsetDateTime evaluatedAt,
        String sourceType,
        UUID evaluationId,
        UUID schoolClassId,
        UUID schoolGradeId
    ) {
    }

    private record EvaluationKey(String sourceType, UUID evaluationId) {
    }

    private record NormalizedScore(WeaknessScoreObservation source, double value) {
    }

    private record StudentCriterionKey(UUID studentId, UUID frameworkCriterionId) {
    }

    private record CohortCriterionKey(UUID cohortId, UUID frameworkCriterionId) {
    }

    private record FrequencyMax(int frequency, int recentFrequency) {
    }
}
