package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "practice_criterion_scores",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_criterion_score_evaluation_code",
        columnNames = {"practice_evaluation_id", "criterion_code"}
    ),
    indexes = @Index(name = "idx_practice_criterion_score_evaluation", columnList = "practice_evaluation_id")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeCriterionScoreJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_evaluation_id", nullable = false, updatable = false)
    private UUID practiceEvaluationId;
    /** Upper-case criterion code (GRAMMAR, PRONUNCIATION...) -- the real identifying key, replacing the rubric id. */
    @Column(name = "criterion_code", nullable = false, length = 32, updatable = false)
    private String criterionCode;
    @Column(name = "final_score", precision = 7, scale = 3)
    private BigDecimal finalScore;
}
