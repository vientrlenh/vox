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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "topic_interest_scores",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_topic_interest_score_student_topic",
        columnNames = {"student_id", "practice_topic_id"}
    ),
    indexes = @Index(
        name = "idx_topic_interest_score_student",
        columnList = "student_id"
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicInterestScoreJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "practice_topic_id", nullable = false)
    private UUID practiceTopicId;

    @Column(name = "score", nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Column(name = "sessions_mentioned", nullable = false)
    private int sessionsMentioned;

    @Column(name = "last_mentioned_at")
    private Instant lastMentionedAt;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
