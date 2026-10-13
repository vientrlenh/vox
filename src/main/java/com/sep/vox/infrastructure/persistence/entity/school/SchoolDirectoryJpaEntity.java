package com.sep.vox.infrastructure.persistence.entity.school;

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
@Table(name = "school_directories", indexes = {
    @Index(columnList = "code", name = "idx_school_directories_code", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class SchoolDirectoryJpaEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "province_code", nullable = false, length = 100)
    private String provinceCode;

    @Column(name = "province_name", nullable = false, length = 255)
    private String provinceName;

    @Column(name = "district_name", nullable = false, length = 255)
    private String districtName;

    @Column(name = "domain", length = 100)
    private String domain;

    @Column(name = "address", nullable = false, length = 512)
    private String address;

    @Column(name = "source", nullable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_school_directories_source_valid",
            constraint = "source IN ('OFFICIAL_IMPORT', 'ADMIN_CREATED', 'USER_SUBMITTED')"
        )
    })
    private String origin;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

}
