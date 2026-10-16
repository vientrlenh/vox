package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_policies", indexes = {
    @Index(columnList = "school_id, grade_level_id, school_grade_id, school_class_id, learning_language_id, framework_version_id, version",
        name = "idx_assessment_policies_scope_version", unique = true),
    @Index(columnList = "school_id, grade_level_id, school_grade_id, learning_language_id, framework_version_id, status",
        name = "idx_assessment_policies_grade_status"),
    @Index(columnList = "rubric_version_id", name = "idx_assessment_policies_rubric_version"),
    @Index(columnList = "target_framework_band_id", name = "idx_assessment_policies_target_band")
}, check = {
    @CheckConstraint(
        name = "chk_assessment_policies_effective_range_valid",
        constraint = "effective_to IS NULL OR effective_from <= effective_to"
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class AssessmentPolicyJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_id", updatable = false)
    private UUID schoolId;

    @Column(name = "grade_level_id", updatable = false)
    private UUID gradeLevelId;

    @Column(name = "school_grade_id", updatable = false)
    private UUID schoolGradeId;

    @Column(name = "school_class_id", updatable = false)
    private UUID schoolClassId;

    @Column(name = "learning_language_id", nullable = false, updatable = false)
    private UUID learningLanguageId;

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "target_framework_band_id", nullable = false)
    private UUID targetFrameworkBandId;

    @Column(name = "passing_score", precision = 6, scale = 2, check = {
        @CheckConstraint(
            name = "chk_assessment_policies_passing_score_non_negative",
            constraint = "passing_score IS NULL OR passing_score >= 0"
        )
    })
    private BigDecimal passingScore;

    @Column(name = "strictness", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_policies_strictness_valid",
            constraint = "strictness IN ('LENIENT', 'STANDARD', 'STRICT')"
        )
    })
    private String strictness;

    @Column(name = "version", nullable = false, check = {
        @CheckConstraint(
            name = "chk_assessment_policies_version_positive",
            constraint = "version > 0"
        )
    })
    private Integer version;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_policies_status_valid",
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

}
