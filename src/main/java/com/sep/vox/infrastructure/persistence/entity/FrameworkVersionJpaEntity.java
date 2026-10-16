package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "framework_versions", indexes = {
    @Index(columnList = "framework_id, version", name = "idx_framework_versions_framework_version", unique = true),
    @Index(columnList = "framework_id, code", name = "idx_framework_versions_framework_code", unique = true),
    @Index(columnList = "status", name = "idx_framework_versions_status")
}, check = {
    @CheckConstraint(
        name = "chk_framework_versions_effective_range_valid",
        constraint = "effective_to IS NULL OR effective_from <= effective_to"
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class FrameworkVersionJpaEntity extends BaseEntity {

    @Column(name = "framework_id", nullable = false, updatable = false)
    private UUID frameworkId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "version", nullable = false, check = {
        @CheckConstraint(
            name = "chk_framework_versions_version_positive",
            constraint = "version > 0"
        )
    })
    private Long version;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_framework_versions_status_valid",
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

}
