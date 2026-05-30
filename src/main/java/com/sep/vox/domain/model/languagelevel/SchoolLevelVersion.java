package com.sep.vox.domain.model.languagelevel;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.LevelOrder;
import com.sep.vox.domain.valueobject.LevelVersion;

public class SchoolLevelVersion {
    private UUID id;
    private UUID schoolLevelId;
    private LevelVersion version;
    private String name;
    private String description;
    private LevelMappingType mappingType;
    private UUID mappedStandardLevelVersionId;
    private UUID mappedStandardLevelMinVersionId;
    private UUID mappedStandardLevelMaxVersionId;
    private LevelStatus status;
    private LevelOrder order;
    private String expectedAbilities;
    private String limitations;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public SchoolLevelVersion() {}


    public SchoolLevelVersion(UUID id, UUID schoolLevelId, LevelVersion version, String name, String description,
            LevelMappingType mappingType, UUID mappedStandardLevelVersionId, UUID mappedStandardLevelMinVersionId,
            UUID mappedStandardLevelMaxVersionId, LevelStatus status, LevelOrder order, String expectedAbilities,
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


    public SchoolLevelVersion(UUID schoolLevelId, LevelVersion version, String name, String description,
            LevelMappingType mappingType, UUID mappedStandardLevelVersionId, UUID mappedStandardLevelMinVersionId,
            UUID mappedStandardLevelMaxVersionId, LevelStatus status, LevelOrder order, String expectedAbilities,
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


    public LevelVersion getVersion() {
        return version;
    }


    public void setVersion(LevelVersion version) {
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


    public LevelMappingType getMappingType() {
        return mappingType;
    }


    public void setMappingType(LevelMappingType mappingType) {
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


    public LevelStatus getStatus() {
        return status;
    }


    public void setStatus(LevelStatus status) {
        this.status = status;
    }


    public LevelOrder getOrder() {
        return order;
    }


    public void setOrder(LevelOrder order) {
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
