package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
    name = "assessment_proctoring_alerts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_assessment_proctoring_alert_event_id",
        columnNames = {"event_id"}
    ),
    indexes = {
        @Index(name = "idx_assessment_proctoring_alert_session", columnList = "assessment_session_id, captured_at"),
        @Index(name = "idx_assessment_proctoring_alert_candidate", columnList = "candidate_id, captured_at")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentProctoringAlertJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false, length = 64)
    private String eventId;

    @Column(name = "assessment_session_id", nullable = false, updatable = false)
    private UUID assessmentSessionId;

    @Column(name = "candidate_id", updatable = false)
    private UUID candidateId;

    @Column(name = "stream_id", updatable = false, length = 64)
    private String streamId;

    @Column(name = "stream_type", updatable = false, length = 20)
    private String streamType;

    @Column(name = "alert_type", nullable = false, updatable = false, length = 64)
    private String alertType;

    @Column(name = "level", updatable = false, length = 16)
    private String level;

    @Column(name = "source", updatable = false, length = 32)
    private String source;

    @Column(name = "detail", updatable = false, length = 1024)
    private String detail;

    @Column(name = "confidence", updatable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "sequence_no", updatable = false)
    private Long sequenceNo;

    @Column(name = "captured_at", nullable = false, updatable = false)
    private Instant capturedAt;

    @Column(name = "raised_at", nullable = false, updatable = false)
    private Instant raisedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
