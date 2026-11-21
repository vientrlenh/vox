package com.sep.vox.infrastructure.persistence.entity.personalization;

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
    name = "practice_papers",
    indexes = {
        @Index(
            name = "idx_practice_paper_student_created",
            columnList = "student_id, created_at"
        ),
        @Index(
            name = "idx_practice_paper_target_band",
            columnList = "target_framework_band_id"
        )
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticePaperJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "practice_topic_id", nullable = false, updatable = false)
    private UUID practiceTopicId;

    /** The band the student chose for the session. Nullable: papers built before V15 carry no choice. */
    @Column(name = "target_framework_band_id", updatable = false)
    private UUID targetFrameworkBandId;

    @Column(name = "origin", nullable = false, length = 24, updatable = false)
    private String origin;

    @Column(name = "goal_type", nullable = false, length = 24, updatable = false)
    private String goalType;

    @Column(name = "offered_topic_ids_json", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String offeredTopicIdsJson;

    @Column(name = "previous_offered_topic_ids_json", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String previousOfferedTopicIdsJson;

    @Column(name = "planned_seconds", nullable = false, updatable = false)
    private int plannedSeconds;

    @Column(name = "reserved_quota_seconds", nullable = false, updatable = false)
    private int reservedQuotaSeconds;

    @Column(name = "reservation_expires_at", nullable = false, updatable = false)
    private Instant reservationExpiresAt;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
