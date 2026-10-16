package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
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
@Table(name = "rubric_versions", indexes = {
    @Index(columnList = "rubric_id, version", name = "idx_rubric_versions_rubric_id_version", unique = true),
    @Index(columnList = "status", name = "idx_rubric_versions_status")
}, check = {
    @CheckConstraint(
        name = "chk_rubric_versions_scoring_scale_valid",
        constraint = "scoring_scale_min <= scoring_scale_max"
    ),
    @CheckConstraint(
        name = "chk_rubric_versions_effective_range_valid",
        constraint = "effective_to IS NULL OR effective_from <= effective_to"
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class RubricVersionJpaEntity extends BaseEntity {

    @Column(name = "rubric_id", nullable = false, updatable = false)
    private UUID rubricId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "version", nullable = false, check = {
        @CheckConstraint(
            name = "chk_rubric_versions_version_positive",
            constraint = "version > 0"
        )
    })
    private Integer version;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(name = "chk_rubric_versions_status_valid", constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')")
    })
    private String status;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "scoring_scale_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoringScaleMin;

    @Column(name = "scoring_scale_max", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoringScaleMax;

    @Column(name = "total_score_method", nullable = false, length = 30, check = {
        @CheckConstraint(name = "chk_rubric_versions_total_score_method_valid", constraint = "total_score_method IN ('WEIGHTED_AVERAGE', 'SUM')")
    })
    private String totalScoreMethod;

    /**
     * The version this one was copied from. Deliberately not a foreign key: system template versions
     * have their own lifecycle and can be archived or deleted, while a school's copy must keep living
     * normally.
     */
    @Column(name = "source_rubric_version_id", updatable = false)
    private UUID sourceRubricVersionId;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

}
