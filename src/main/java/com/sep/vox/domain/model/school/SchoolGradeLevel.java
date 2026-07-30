package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

public class SchoolGradeLevel {
    private UUID id;
    private UUID schoolId;
    private String code;
    private String name;
    private String description;
    private int order;
    private SchoolGradeLevelStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public SchoolGradeLevel() {}

    public SchoolGradeLevel(UUID id, UUID schoolId, String code, String name, String description, int order, 
            SchoolGradeLevelStatus status, Instant createdAt, Instant updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.order = order;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public SchoolGradeLevel(UUID schoolId, String code, String name, String description, int order, SchoolGradeLevelStatus status,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.schoolId = schoolId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.order = order;
        this.status = status;
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

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public SchoolGradeLevelStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolGradeLevelStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    
}
