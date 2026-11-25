package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "practice_item_evaluations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_evaluation_response",
        columnNames = "practice_response_id"
    ),
    indexes = @Index(name = "idx_practice_evaluation_time", columnList = "evaluated_at")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeItemEvaluationJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_response_id", nullable = false, updatable = false)
    private UUID practiceResponseId;
    @Column(name = "item_score", precision = 5, scale = 2)
    private BigDecimal itemScore;
    @Column(name = "marked_invalid", nullable = false)
    private boolean markedInvalid;
    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;
}
