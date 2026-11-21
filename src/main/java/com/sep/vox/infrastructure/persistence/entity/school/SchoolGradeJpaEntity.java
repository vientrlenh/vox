package com.sep.vox.infrastructure.persistence.entity.school;

import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
// grade_level_id points to the GLOBAL catalog (grade_levels), so the unique index must include school_id; with
// only (grade_level_id, code) as in the old version, two different schools could not both have a class "10A1" in grade 10.
@Table(name = "school_grades", indexes = {
    @Index(columnList = "school_id, grade_level_id, code", name = "idx_school_grades_school_level_code", unique = true),
    @Index(columnList = "school_id", name = "idx_school_grades_school"),
    @Index(columnList = "grade_level_id", name = "idx_school_grades_level")
}, check = {
    @CheckConstraint(
        name = "chk_school_grades_start_end_date_valid", 
        constraint = "start_date < end_date"
    )
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolGradeJpaEntity extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "grade_level_id", nullable = false, updatable = false)
    private UUID gradeLevelId;

    
    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code; 

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_status_valid", 
            constraint = "status IN ('INACTIVE', 'ACTIVE', 'ARCHIVED')"
        )
    })
    private String status;
}
