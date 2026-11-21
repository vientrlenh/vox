package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
    name = "topic_interest_events",
    indexes = @Index(
        name = "idx_topic_interest_event_student_time",
        columnList = "student_id, occurred_at"
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicInterestEventJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "practice_topic_id", nullable = false, updatable = false)
    private UUID practiceTopicId;

    @Column(name = "practice_session_id", updatable = false)
    private UUID practiceSessionId;

    @Column(name = "event_type", nullable = false, length = 32, updatable = false)
    private String eventType;

    @Column(name = "signal", nullable = false, precision = 4, scale = 3, updatable = false)
    private BigDecimal signal;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
