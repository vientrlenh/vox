package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "dimension_interest_scores",
    indexes = @Index(
        name = "idx_dimension_interest_profile_dimension",
        columnList = "learner_profile_id, dimension",
        unique = true
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DimensionInterestScoreJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "learner_profile_id", nullable = false, updatable = false)
    private UUID learnerProfileId;

    @Column(name = "dimension", nullable = false, length = 32, updatable = false)
    private String dimension;

    @Column(name = "score", nullable = false, precision = 5, scale = 4, updatable = false)
    private BigDecimal score;

    @Column(name = "baseline_score", precision = 5, scale = 4, updatable = false)
    private BigDecimal baselineScore;
}
