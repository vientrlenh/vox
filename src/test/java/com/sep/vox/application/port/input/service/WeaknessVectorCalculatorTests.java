package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import com.sep.vox.application.common.DateMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.personalization.WeaknessFrequency;
import com.sep.vox.domain.model.personalization.WeaknessScoreObservation;

class WeaknessVectorCalculatorTests {

    private static final List<String> CRITERION_CODES = List.of(
        "PRONUNCIATION",
        "FLUENCY",
        "GRAMMAR",
        "VOCABULARY",
        "COHERENCE"
    );

    private final WeaknessVectorCalculator calculator = new WeaknessVectorCalculator();
    private final WeaknessVectorSettings settings = settings();

    @Test
    void should_center_each_evaluation_and_build_five_reliable_snapshots() {
        var studentId = UUID.randomUUID();
        var scores = fourEvaluations(studentId);

        var result = calculator.calculate(
            scores,
            Set.of(studentId),
            List.of(),
            DateMapper.toInstant("2026-07-29T10:00:00+07:00"),
            settings
        );

        assertThat(result.snapshots()).hasSize(5);
        assertThat(result.snapshots())
            .allSatisfy(snapshot -> {
                assertThat(snapshot.getObservationCount()).isEqualTo(4);
                assertThat(snapshot.isReliable()).isTrue();
            });
        assertThat(result.relativeObservations().stream()
            .map(WeaknessVectorCalculator.RelativeObservation::evaluationId)
            .distinct())
            .hasSize(4);
        for (var evaluationId : scores.stream().map(WeaknessScoreObservation::getEvaluationId).distinct().toList()) {
            var relativeSum = result.relativeObservations().stream()
                .filter(item -> item.evaluationId().equals(evaluationId))
                .mapToDouble(WeaknessVectorCalculator.RelativeObservation::relativeScore)
                .sum();
            assertThat(relativeSum).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-12));
        }
    }

    @Test
    void should_be_idempotent_and_keep_history_across_rubric_versions() {
        var studentId = UUID.randomUUID();
        var scores = fourEvaluations(studentId);
        var now = DateMapper.toInstant("2026-07-29T10:00:00+07:00");

        var first = calculator.calculate(scores, Set.of(studentId), List.of(), now, settings);
        var second = calculator.calculate(scores, Set.of(studentId), List.of(), now, settings);

        // So theo GIA TRI tung field, khong dua vao equals(): model domain o day theo dung
        // convention cua domain/model/exam va framework -- class thuong, khong override
        // equals/hashCode -- nen isEqualTo() se thanh so sanh tham chieu va luon sai.
        // Y nghia can khang dinh van y nguyen: tinh lai lan hai ra dung cung bo gia tri.
        assertThat(second.snapshots())
            .usingRecursiveComparison()
            .isEqualTo(first.snapshots());
        assertThat(second.snapshots())
            .allSatisfy(snapshot -> assertThat(snapshot.getObservationCount()).isEqualTo(4));
    }

    @Test
    void pronunciation_should_reach_point_seven_weight_before_coherence() {
        assertThat(calculator.shrinkageWeight("PRONUNCIATION", 7, settings))
            .isEqualTo(0.7);
        assertThat(calculator.shrinkageWeight("COHERENCE", 7, settings))
            .isEqualTo(0.5);
    }

    @Test
    void should_rank_only_repeated_sub_attributes_and_keep_practiceable_false() {
        var studentId = UUID.randomUUID();
        var scores = fourEvaluations(studentId);
        var criterionId = scores.stream()
            .filter(score -> score.getCriterionCode().equals("GRAMMAR"))
            .findFirst()
            .orElseThrow()
            .getFrameworkCriterionId();
        var frequencies = List.of(
            new WeaknessFrequency(studentId, criterionId, "tense_control", 6, 3),
            new WeaknessFrequency(studentId, criterionId, "article_use", 3, 3),
            new WeaknessFrequency(studentId, criterionId, "word_form", 2, 2)
        );

        var result = calculator.calculate(
            scores,
            Set.of(studentId),
            frequencies,
            DateMapper.toInstant("2026-07-29T10:00:00+07:00"),
            settings
        );

        assertThat(result.priorities())
            .extracting(item -> item.getSubAttribute())
            .containsExactlyInAnyOrder("tense_control", "article_use");
        assertThat(result.priorities())
            .allSatisfy(item -> assertThat(item.isPracticeable()).isFalse());
    }

    private List<WeaknessScoreObservation> fourEvaluations(UUID studentId) {
        var criterionIds = CRITERION_CODES.stream()
            .collect(java.util.stream.Collectors.toMap(code -> code, code -> UUID.randomUUID()));
        var result = new ArrayList<WeaknessScoreObservation>();
        for (var evaluationIndex = 0; evaluationIndex < 4; evaluationIndex++) {
            var evaluationId = UUID.randomUUID();
            for (var criterionIndex = 0; criterionIndex < CRITERION_CODES.size(); criterionIndex++) {
                var code = CRITERION_CODES.get(criterionIndex);
                result.add(new WeaknessScoreObservation(
                    studentId,
                    criterionIds.get(code),
                    code,
                    BigDecimal.valueOf(40 + criterionIndex * 10 + evaluationIndex),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(100),
                    // Instant cộng theo Duration, không có plusDays (nó không mang lịch).
                    DateMapper.toInstant("2026-07-01T10:00:00+07:00")
                        .plus(Duration.ofDays(evaluationIndex)),
                    "EXAM",
                    evaluationId,
                    null,
                    null
                ));
            }
        }
        return result;
    }

    private WeaknessVectorSettings settings() {
        return new WeaknessVectorSettings(
            0.2,
            Map.of(
                "PRONUNCIATION", 3.0,
                "FLUENCY", 4.0,
                "GRAMMAR", 5.0,
                "VOCABULARY", 5.0,
                "COHERENCE", 7.0
            ),
            3,
            3,
            10,
            Duration.ofDays(60),
            Duration.ofDays(14),
            3,
            0.6,
            0.4,
            Duration.ofHours(24),
            200
        );
    }
}
