package com.sep.vox.infrastructure.persistence.entity.rubric;

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
@Table(name = "rubrics", indexes = {
    @Index(columnList = "owner_type, school_id, language_id, framework_id, code", name = "idx_rubrics_owner_scope_code", unique = true),
    @Index(columnList = "language_id, framework_id", name = "idx_rubrics_language_framework")
}, check = {
    @CheckConstraint(
        name = "chk_rubrics_owner_school_valid",
        constraint = "(owner_type = 'SYSTEM' AND school_id IS NULL) OR (owner_type = 'SCHOOL' AND school_id IS NOT NULL)"
    )
})

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RubricJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "language_id", nullable = false, updatable = false)
    private UUID languageId;

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
