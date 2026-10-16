package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
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
@Table(name = "rubric_criterions", indexes = {
    @Index(columnList = "rubric_version_id, code", name = "idx_rubric_criterions_version_code", unique = true),
    @Index(columnList = "rubric_version_id, framework_criterion_id", name = "idx_rubric_criterions_version_framework_criterion", unique = true),
    @Index(columnList = "framework_criterion_id", name = "idx_rubric_criterions_framework_criterion")
}, check = {
    @CheckConstraint(
        name = "chk_rubric_criterions_score_range_valid",
        constraint = "min_score <= max_score"
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class RubricCriterionJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "framework_criterion_id", nullable = false, updatable = false)
    private UUID frameworkCriterionId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "examples_json", columnDefinition = "TEXT")
    private String examplesJson;

    @Column(name = "weight", nullable = false, precision = 6, scale = 2, check = {
        @CheckConstraint(
            name = "chk_rubric_criterions_weight_non_negative",
            constraint = "weight >= 0"
        )
    })
    private BigDecimal weight;

    @Column(name = "min_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal minScore;

    @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "is_required", nullable = false)
    private Boolean required;

}
