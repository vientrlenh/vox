package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

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
@Table(name = "rubrics", indexes = {
    @Index(columnList = "owner_type, school_id, learning_language_id, framework_id, name", name = "idx_rubrics_owner_scope_name", unique = true),
    @Index(columnList = "learning_language_id, framework_id", name = "idx_rubrics_language_framework")
}, check = {
    @CheckConstraint(
        name = "chk_rubrics_owner_school_valid",
        constraint = "(owner_type = 'SYSTEM' AND school_id IS NULL) OR (owner_type = 'SCHOOL' AND school_id IS NOT NULL)"
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class RubricJpaEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "learning_language_id", nullable = false, updatable = false)
    private UUID learningLanguageId;

    @Column(name = "framework_id", nullable = false)
    private UUID frameworkId;

    @Column(name = "owner_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_rubrics_owner_type_valid",
            constraint = "owner_type IN ('SYSTEM', 'SCHOOL')"
        )
    })
    private String ownerType;

    @Column(name = "school_id")
    private UUID schoolId;

}
