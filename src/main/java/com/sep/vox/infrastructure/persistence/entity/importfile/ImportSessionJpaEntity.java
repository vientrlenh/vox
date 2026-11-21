package com.sep.vox.infrastructure.persistence.entity.importfile;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "import_sessions", indexes = {
    @Index(columnList = "school_id, type, status", name = "idx_import_sessions_school_type_status"),
    @Index(columnList = "school_id, created_at", name = "idx_import_sessions_school_created_at"),
    @Index(columnList = "expires_at", name = "idx_import_sessions_expires_at")
}, check = {
    @CheckConstraint(
        name = "chk_import_sessions_row_counts_non_negative",
        constraint = "valid_rows >= 0 AND invalid_rows >= 0 AND imported_rows >= 0 AND skipped_rows >= 0 AND total_rows >= 0"
    )
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportSessionJpaEntity extends BaseEntity {

    @Column(name = "school_id", updatable = false)
    private UUID schoolId;

    @Column(name = "type", nullable = false, updatable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_import_sessions_type_valid",
            constraint = "type IN ('USER', 'SCHOOL_CLASS', 'SCHOOL_CLASS_USER','QUESTION','QUESTION_BANK','QUESTION_TOPIC', 'SCHOOL_DIRECTORY', 'SCHOOL_GRADE_LEVEL', 'SCHOOL_GRADE','SCHOOL_ROOM','RUBRIC_VERSION','RUBRIC_CRITERION','RUBRIC_RESULT_BAND','ASSESSMENT_POLICY')"
        )
    })
    private String type;

    @Column(name = "file_name", nullable = false, updatable = false, length = 255)
    private String fileName;

    @Column(name = "original_headers_json", nullable = false, columnDefinition = "TEXT")
    private String originalHeadersJson;

    @Column(name = "suggested_mapping_json", columnDefinition = "TEXT")
    private String suggestedMappingJson;

    @Column(name = "confirmed_mapping_json", columnDefinition = "TEXT")
    private String confirmedMappingJson;

    @Column(name = "valid_rows", nullable = false)
    private long validRows;

    @Column(name = "invalid_rows", nullable = false)
    private long invalidRows;

    @Column(name = "imported_rows", nullable = false)
    private long importedRows;

    @Column(name = "skipped_rows", nullable = false)
    private long skippedRows;

    @Column(name = "total_rows", nullable = false)
    private long totalRows;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "status", nullable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_import_sessions_status_valid",
            constraint = "status IN ('PREVIEWED', 'VALIDATING', 'IMPORTING', 'QUEUED', 'COMPLETED', 'FAILED', 'EXPIRED', 'CANCELLED')"
        ),
    })
    private String status;

    @Column(name = "imported_entity_id")
    private UUID importedEntityId;

    @Column(name = "expires_at", nullable = false) 
    private Instant expiresAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by")
    private UUID claimedBy; 

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;
}
