package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_blueprint_versions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentBlueprintVersionJpaEntity extends BaseEntity {

    @Column(name = "blue_print_id", nullable = false, updatable = false)
    private UUID blueprintId;

    @Column(name = "version", nullable = false, updatable = false)
    private int version;

    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_blueprint_versions_status_valid",
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(name = "total_time_limit_seconds", check = {
        @CheckConstraint(
            name = "chk_assessment_blueprint_versions_total_time_limit_valid",
            constraint = "total_time_limit_seconds > 0"
        )
    })
    private Integer totalTimeLimitSeconds;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

}
