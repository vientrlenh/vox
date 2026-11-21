package com.sep.vox.infrastructure.persistence.entity.school;

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
@Table(name = "schools", indexes = {
    @Index(columnList = "code", name = "idx_schools_code", unique = true),
    @Index(columnList = "name", name = "idx_schools_name"),
    @Index(columnList = "code, domain", name = "idx_schools_code_domain", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolJpaEntity extends BaseEntity {

    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "contact_phone", length = 20, nullable = false)
    private String contactPhone;

    @Column(name = "contact_email", length = 255, nullable = false)
    private String contactEmail;

    @Column(name = "domain", length = 100)
    private String domain;

    @Column(name = "address", length = 512, nullable = false)
    private String address;

    @Column(name = "student_count", nullable = false, check = @CheckConstraint(
        name = "chk_student_count_positive", 
        constraint = "student_count > 0"
    ))
    private int studentCount;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
