package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_item_criterion_scores")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentItemCriterionScoreJpaEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "evaluation_id", nullable = false, updatable = false)
    private UUID evaluationId;

    @Column(name = "rubric_criterion_id", nullable = false, updatable = false)
    private UUID rubricCriterionId;

    @Column(name = "raw_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal rawScore;

    @Column(name = "final_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal finalScore;

    // TEXT, not varchar(512): the AI rationale grows with the number of speaking turns of the question and has no ceiling.
    // The old ceiling broke the WHOLE grading, not just truncated the text -- see
    // V14__widen_criterion_rationale.sql.
    @Column(name = "rationale", columnDefinition = "TEXT", updatable = false)
    private String rationale;

    @Column(name = "matched_band_code", length = 64, updatable = false)
    private String matchedBandCode;
}
