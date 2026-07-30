package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "framework_criteria", indexes = {
    @Index(columnList = "framework_version_id, code", name = "idx_framework_criteria_version_code", unique = true)
})
public class FrameworkCriterionJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "criteria_order", nullable = false)
    private int order;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected FrameworkCriterionJpaEntity() {}

    public FrameworkCriterionJpaEntity(UUID id, UUID frameworkVersionId, String code, String name,
            String description, int order, Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.frameworkVersionId = frameworkVersionId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.order = order;
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

    public UUID getFrameworkVersionId() {
        return frameworkVersionId; 
    }
    
    public void setFrameworkVersionId(UUID frameworkVersionId) { 
        this.frameworkVersionId = frameworkVersionId; 
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
