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
@Table(name = "school_classes", indexes = {
    @Index(columnList = "school_id, code", name = "idx_school_class_code", unique = true),
    @Index(columnList = "school_id, name", name = "idx_school_class_name"), 
    @Index(columnList = "id, school_id, language", name = "idx_school_class_class_school_language", unique = true)
})
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder 
public class SchoolClassJpaEntity extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "language", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_classes_language", 
            constraint = "language IN ('ENGLISH')"
        )
    })
    private String language;

    @Column(name = "school_grade_id", nullable = false, updatable = false)
    private UUID schoolGradeId;

    @Column(name = "code", nullable = false, length = 100, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_class_status_valid", 
            constraint = "status IN ('INACTIVE', 'ACTIVE', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp 
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

}
