package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_level_versions", indexes = {
    @Index(columnList = "school_level_id, version", name = "idx_school_level_versions_level_version", unique = true)
}, check = {
    @CheckConstraint(
        name = "chk_school_level_versions_effective_to_is_null_or_greater_than_effective_from", 
        constraint = "effective_to IS NULL OR effective_to > effective_from"
    ),
    @CheckConstraint(
        name = "chk_school_level_versions_mapping_fields_match_mapping_type",
        constraint = """
            (
                status = 'DRAFT'
                AND (
                    mapping_type IS NULL 
                    OR mapping_type IN ('EXACT', 'RANGE')
                )
            )
            OR
            (
                status <> 'DRAFT'
                AND mapping_type = 'EXACT' 
                AND mapped_standard_level_version_id IS NOT NULL
                AND mapped_standard_level_min_version_id IS NULL 
                AND mapped_standard_level_max_version_id IS NULL
            )
            OR
            (
                status <> 'DRAFT'
                AND mapping_type = 'RANGE' 
                AND mapped_standard_level_version_id IS NULL 
                AND mapped_standard_level_min_version_id IS NOT NULL 
                AND mapped_standard_level_max_version_id IS NOT NULL
            )
        """
    ),
})
public class SchoolLevelVersionJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "school_level_id", nullable = false, updatable = false)
    private UUID schoolLevelId;

    @Column(
        name = "version",
        nullable = false, 
        updatable = false, 
        check = @CheckConstraint(
            name = "chk_school_level_versions_version_positive",
            constraint = "version > 0"
        )
    )
    private int version;

    @Column(name = "name", nullable = false, length = 100, updatable = false)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "mapping_type", length = 20, check = {
        @CheckConstraint(
            name = "chk_mapping_type_valid", 
            constraint = "mapping_type IN ('EXACT', 'RANGE')"
        )
    })
    private String mappingType;

    @Column(name = "mapped_standard_level_version_id")
    private UUID mappedStandardLevelVersionId;

    @Column(name = "mapped_standard_level_min_version_id")
    private UUID mappedStandardLevelMinVersionId;

    @Column(name = "mapped_standard_level_max_version_id")
    private UUID mappedStandardLevelMaxVersionId;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_status_valid", 
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(
        name = "level_order", 
        nullable = false, 
        updatable = false, 
        check = @CheckConstraint(
            name = "chk_school_level_versions_order_positive", 
            constraint = "level_order > 0"
        )
    )
    private int order;

    @Column(name = "expected_abilities", length = 4096)
    private String expectedAbilities;

    @Column(name = "limitations", length = 4096)
    private String limitations;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected SchoolLevelVersionJpaEntity() {}

    public SchoolLevelVersionJpaEntity(UUID id, UUID schoolLevelId, int version, String name, String description,
            String mappingType, UUID mappedStandardLevelVersionId, UUID mappedStandardLevelMinVersionId,
            UUID mappedStandardLevelMaxVersionId, String status, int order, String expectedAbilities,
            String limitations, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.schoolLevelId = schoolLevelId;
        this.version = version;
        this.name = name;
        this.description = description;
        this.mappingType = mappingType;
        this.mappedStandardLevelVersionId = mappedStandardLevelVersionId;
        this.mappedStandardLevelMinVersionId = mappedStandardLevelMinVersionId;
        this.mappedStandardLevelMaxVersionId = mappedStandardLevelMaxVersionId;
        this.status = status;
        this.order = order;
        this.expectedAbilities = expectedAbilities;
        this.limitations = limitations;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public SchoolLevelVersionJpaEntity(UUID schoolLevelId, int version, String name, String description,
            String mappingType, UUID mappedStandardLevelVersionId, UUID mappedStandardLevelMinVersionId,
            UUID mappedStandardLevelMaxVersionId, String status, int order, String expectedAbilities,
            String limitations, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.schoolLevelId = schoolLevelId;
        this.version = version;
        this.name = name;
        this.description = description;
        this.mappingType = mappingType;
        this.mappedStandardLevelVersionId = mappedStandardLevelVersionId;
        this.mappedStandardLevelMinVersionId = mappedStandardLevelMinVersionId;
        this.mappedStandardLevelMaxVersionId = mappedStandardLevelMaxVersionId;
        this.status = status;
        this.order = order;
        this.expectedAbilities = expectedAbilities;
        this.limitations = limitations;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolLevelId() {
        return schoolLevelId;
    }

    public void setSchoolLevelId(UUID schoolLevelId) {
        this.schoolLevelId = schoolLevelId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMappingType() {
        return mappingType;
    }

    public void setMappingType(String mappingType) {
        this.mappingType = mappingType;
    }

    public UUID getMappedStandardLevelVersionId() {
        return mappedStandardLevelVersionId;
    }

    public void setMappedStandardLevelVersionId(UUID mappedStandardLevelVersionId) {
        this.mappedStandardLevelVersionId = mappedStandardLevelVersionId;
    }

    public UUID getMappedStandardLevelMinVersionId() {
        return mappedStandardLevelMinVersionId;
    }

    public void setMappedStandardLevelMinVersionId(UUID mappedStandardLevelMinVersionId) {
        this.mappedStandardLevelMinVersionId = mappedStandardLevelMinVersionId;
    }

    public UUID getMappedStandardLevelMaxVersionId() {
        return mappedStandardLevelMaxVersionId;
    }

    public void setMappedStandardLevelMaxVersionId(UUID mappedStandardLevelMaxVersionId) {
        this.mappedStandardLevelMaxVersionId = mappedStandardLevelMaxVersionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getExpectedAbilities() {
        return expectedAbilities;
    }

    public void setExpectedAbilities(String expectedAbilities) {
        this.expectedAbilities = expectedAbilities;
    }

    public String getLimitations() {
        return limitations;
    }

    public void setLimitations(String limitations) {
        this.limitations = limitations;
    }

    public OffsetDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(OffsetDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public OffsetDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(OffsetDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    

}
