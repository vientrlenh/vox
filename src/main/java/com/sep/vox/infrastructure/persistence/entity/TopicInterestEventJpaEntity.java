package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "topic_interest_events",
    indexes = @Index(
        name = "idx_topic_interest_event_student_time",
        columnList = "student_id, occurred_at"
    )
)
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

    protected TopicInterestEventJpaEntity() {
    }

    public TopicInterestEventJpaEntity(
            UUID id,
            UUID studentId,
            UUID practiceTopicId,
            UUID practiceSessionId,
            String eventType,
            BigDecimal signal,
            Instant occurredAt) {
        this.id = id;
        this.studentId = studentId;
        this.practiceTopicId = practiceTopicId;
        this.practiceSessionId = practiceSessionId;
        this.eventType = eventType;
        this.signal = signal;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getPracticeTopicId() {
        return practiceTopicId;
    }

    public UUID getPracticeSessionId() {
        return practiceSessionId;
    }

    public String getEventType() {
        return eventType;
    }

    public BigDecimal getSignal() {
        return signal;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
