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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "schools", indexes = {
    @Index(columnList = "code", name = "idx_schools_code", unique = true),
    @Index(columnList = "name", name = "idx_schools_name"),
    @Index(columnList = "code, domain", name = "idx_schools_code_domain", unique = true)
})
@Getter 
@NoArgsConstructor 
@SuperBuilder 
public class SchoolJpaEntity extends BaseEntity {

    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "ward_id", nullable = false)
    private UUID wardId;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "province_id", nullable = false)
    private UUID provinceId;

    @Column(name = "contact_phone", length = 20, nullable = false)
    private String contactPhone;

    @Column(name = "contact_email", length = 255, nullable = false)
    private String contactEmail;

    @Column(name = "domain", length = 100)
    private String domain;

    @Column(name = "street_address", length = 512, nullable = false)
    private String streetAddress;

    @Column(name = "student_count", nullable = false, check = @CheckConstraint(
        name = "chk_student_count_positive", 
        constraint = "student_count >= 0"
    ))
    private Integer studentCount;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_schools_status_valid", 
            constraint = "status IN ('INACTIVE', 'ACTIVE')"
        )
    })
    private String status;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp 
    private Instant updatedAt;

    @Column(name = "registered_by", nullable = false, updatable = false)
    private UUID registeredBy;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    
}
