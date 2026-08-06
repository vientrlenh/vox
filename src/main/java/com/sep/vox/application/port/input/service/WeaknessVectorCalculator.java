package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import com.sep.vox.domain.service.personalization.SubAttributePolicy;
import com.sep.vox.domain.model.personalization.WeaknessFrequency;
import com.sep.vox.domain.model.personalization.WeaknessScoreObservation;

@Service
public class WeaknessVectorCalculator {

    public CalculationResult calculate(
            List<WeaknessScoreObservation> scores,
            Set<UUID> targetStudentIds,
            List<WeaknessFrequency> frequencies,
            Instant computedAt,
            WeaknessVectorSettings settings) {
        var relatives = centerScores(scores, settings.minimumCriteriaPerEvaluation());
        var snapshots = calculateSnapshots(relatives, targetStudentIds, computedAt, settings);
        // KHÔNG nhận snapshots nữa: hai nửa của lớp này đã độc lập -- xem calculatePriorities.
        var priorities = calculatePriorities(frequencies, computedAt, settings);
        return new CalculationResult(snapshots, priorities, relatives);
    }

    List<RelativeObservation> centerScores(
            List<WeaknessScoreObservation> scores,
            int minimumCriteriaPerEvaluation) {
        var byEvaluation = scores.stream().collect(Collectors.groupingBy(
            score -> new EvaluationKey(score.getSourceType(), score.getEvaluationId()),
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
                    source.getStudentId(),
                    source.getFrameworkCriterionId(),
                    source.getCriterionCode().toUpperCase(Locale.ROOT),
                    score.value() - center,
                    source.getEvaluatedAt(),
                    source.getSourceType(),
                    source.getEvaluationId(),
                    source.getSchoolClassId(),
                    source.getSchoolGradeId()
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
            Instant computedAt,
            WeaknessVectorSettings settings) {
        // CÙNG ngưỡng cho cả hai tầng cohort. Trước đây tầng khối chỉ đòi 1 học sinh, và một
        // tập gồm đúng một người thì trung bình của tập CHÍNH LÀ người đó:
        //
        //     estimate = w·ema + (1−w)·ema = ema
        //
        // tức co Bayes co về chính mình = không co gì cả. Đo được trên dữ liệu thật
        // (2026-08-05, n=4, k=5 nên w=0,44): weakness GRAMMAR trong DB là 0,1798 trong khi
        // rel thô là 0,180 -- lệch 0,2%, đúng bằng "không co".
        //
        // Dưới ngưỡng thì prior về 0,0, và 0,0 ở đây KHÔNG phải giá trị mặc định tuỳ tiện: rel
        // đã trừ trung bình nên 0 nghĩa là "không lệch về phía nào", tức đúng cái ta muốn nói
        // khi chưa đủ bằng chứng.
        var classPriors = priorsByCohort(
            relatives,
            RelativeObservation::schoolClassId,
            settings.minimumCohortStudents()
        );
        var gradePriors = priorsByCohort(
            relatives,
            RelativeObservation::schoolGradeId,
            settings.minimumCohortStudents()
        );
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
            Instant computedAt,
            WeaknessVectorSettings settings) {
        return frequencies.stream()
            .filter(item -> item.getFrequency() >= settings.minimumSubAttributeFrequency())
            .map(item -> new SubAttributePriority(
                stableId(
                    "sub-attribute-priority",
                    item.getStudentId(),
                    item.getFrameworkCriterionId(),
                    item.getSubAttribute()
                ),
                item.getStudentId(),
                item.getFrameworkCriterionId(),
                item.getSubAttribute(),
                item.getFrequency(),
                decimal(item.getDecayedFrequency()),
                // practiceable = "việc sinh câu hỏi có nhắm được đúng nhãn này không", chứ
                // không phải "nhãn này có đáng luyện không". Phép thử là taxonomy đóng: nhãn
                // nào prompt sinh câu hiểu được thì nhắm được.
                SubAttributePolicy.criterionForSubAttribute(item.getSubAttribute()) != null,
                computedAt
            ))
            .toList();
    }

    private boolean hasValidScale(WeaknessScoreObservation score) {
        return score.getFinalScore() != null
            && score.getMinScore() != null
            && score.getMaxScore() != null
            && score.getMaxScore().compareTo(score.getMinScore()) > 0;
    }

    private double normalize(WeaknessScoreObservation score) {
        return score.getFinalScore().subtract(score.getMinScore())
            .divide(score.getMaxScore().subtract(score.getMinScore()), 12, RoundingMode.HALF_UP)
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
        Instant evaluatedAt,
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

}
