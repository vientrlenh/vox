package com.sep.vox.infrastructure.persistence.entity.assessment;

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
@Table(name = "assessment_blueprints")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentBlueprintJpaEntity extends BaseEntity {

    @Column(name = "school_id", updatable = false)
    private UUID schoolId;

    @Column(name = "language_id", nullable = false, updatable = false)
    private UUID languageId;

    @Column(name = "grade_level_id", updatable = false)
    private UUID gradeLevelId;

    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

}
