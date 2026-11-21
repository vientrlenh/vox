package com.sep.vox.infrastructure.persistence.entity.importfile;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
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
@Table(name = "import_rows", indexes = {
    @Index(columnList = "session_id, row_number", name = "idx_import_rows_session_row_number", unique = true),
    @Index(columnList = "session_id, status", name = "idx_import_rows_session_status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportRowJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "row_number", nullable = false, updatable = false, check = {
        @CheckConstraint(
            name = "chk_import_rows_row_number_positive",
            constraint = "row_number > 0"
        )
    })
    private long rowNumber;

    @Column(name = "raw_data_json", nullable = false, columnDefinition = "TEXT")
    private String rawDataJson;

    @Column(name = "mapped_data_json", columnDefinition = "TEXT")
    private String mappedDataJson;

    @Column(name = "errors_json", columnDefinition = "TEXT")
    private String errorsJson;

    @Column(name = "status", nullable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_import_rows_status_valid",
            constraint = "status IN ('PENDING', 'VALID', 'INVALID', 'IMPORTED', 'SKIPPED', 'FAILED')"
        )
    })
    private String status;
}
