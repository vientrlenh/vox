package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "assessment_recordings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_assessment_recordings_session_stream_source",
        columnNames = {"assessment_session_id", "stream_type", "source"}
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentRecordingJpaEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "assessment_session_id", nullable = false, updatable = false)
    private UUID assessmentSessionId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Column(name = "stream_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_recordings_stream_type_valid", 
            constraint = "stream_type IN ('SCREEN', 'CAMERA')"
        )
    })
    private String streamType;

    @Column(name = "bucket", nullable = false, length = 2048)
    private String bucket;

    @Column(name = "s3_key", nullable = false, length = 1024)
    private String s3Key;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_recordings_status_valid", 
            constraint = "status IN ('PROCESSING', 'READY', 'PARTIAL', 'FAILED', 'ABANDONED')"
        )
    })
    private String status;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // Nullable so a schema update stays compatible with old recordings that have no source. A new event is always
    // normalized by the mapper into a concrete source or UNKNOWN.
    @Column(name = "source", updatable = false, length = 32)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "assembled_at")
    private Instant assembledAt;
}
