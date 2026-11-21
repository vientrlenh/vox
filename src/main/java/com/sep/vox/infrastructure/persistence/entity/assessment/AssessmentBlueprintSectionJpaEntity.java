package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_blueprint_sections")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentBlueprintSectionJpaEntity extends BaseEntity {

    @Column(name = "blueprint_version_id", nullable = false, updatable = false)
    private UUID blueprintVersionId;

    @Column(name = "section_order", nullable = false)
    private int order;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "instruction", length = 512)
    private String instruction;

    @Column(name = "section_time_limits_seconds")
    private Integer sectionTimeLimitSeconds;

    @Column(name = "section_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal sectionWeight;

}
