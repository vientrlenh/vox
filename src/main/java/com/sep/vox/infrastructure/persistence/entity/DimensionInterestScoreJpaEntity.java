package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "dimension_interest_score",
    indexes = @Index(
        name = "idx_dimension_interest_profile_dimension",
        columnList = "learner_profile_id, dimension",
        unique = true
    )
)
public class DimensionInterestScoreJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "learner_profile_id", nullable = false, updatable = false)
    private UUID learnerProfileId;

    @Column(name = "dimension", nullable = false, length = 32, updatable = false)
    private String dimension;

    @Column(name = "score", nullable = false, precision = 5, scale = 4, updatable = false)
    private BigDecimal score;

    @Column(name = "baseline_score", precision = 5, scale = 4, updatable = false)
    private BigDecimal baselineScore;

    protected DimensionInterestScoreJpaEntity() {
    }

    public DimensionInterestScoreJpaEntity(
            UUID learnerProfileId,
            String dimension,
            BigDecimal score,
            BigDecimal baselineScore) {
        this.learnerProfileId = learnerProfileId;
        this.dimension = dimension;
        this.score = score;
        this.baselineScore = baselineScore;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLearnerProfileId() {
        return learnerProfileId;
    }

    public String getDimension() {
        return dimension;
    }

    public BigDecimal getScore() {
        return score;
    }

    public BigDecimal getBaselineScore() {
        return baselineScore;
    }
}
