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
    name = "practice_sessions",
    indexes = {
        @Index(name = "idx_practice_session_student_started", columnList = "student_id, started_at"),
        @Index(name = "idx_practice_session_heartbeat", columnList = "status, last_heartbeat_at")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;
    @Column(name = "practice_paper_id", nullable = false, updatable = false)
    private UUID practicePaperId;
    @Column(name = "target_framework_band_id", nullable = false, updatable = false)
    private UUID targetFrameworkBandId;
    @Column(name = "chosen_practice_topic_id", nullable = false, updatable = false)
    private UUID chosenPracticeTopicId;
    @Column(name = "target_sub_attributes_json", columnDefinition = "TEXT", updatable = false)
    private String targetSubAttributesJson;
    @Column(name = "origin", nullable = false, length = 24, updatable = false)
    private String origin;
    @Column(name = "offered_topic_ids_json", columnDefinition = "TEXT", updatable = false)
    private String offeredTopicIdsJson;
    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;
    @Column(name = "ended_at")
    private Instant endedAt;
    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;
    @Column(name = "graded_seconds", nullable = false)
    private int gradedSeconds;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "abandon_diagnosis", length = 16)
    private String abandonDiagnosis;
    @Column(name = "help_request_count", nullable = false)
    private int helpRequestCount;
    @Column(name = "long_pause_count", nullable = false)
    private int longPauseCount;
}
