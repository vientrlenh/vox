package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
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
@Table(name = "rubric_result_bands", indexes = {
    @Index(columnList = "rubric_version_id, code", name = "idx_rubric_result_bands_version_id_code", unique = true),
    @Index(columnList = "rubric_version_id, framework_result_band_id", name = "idx_rubric_result_bands_version_framework_band", unique = true),
    @Index(columnList = "framework_result_band_id", name = "idx_rubric_result_bands_framework_band")
}, check = {
    @CheckConstraint(
        name = "chk_rubric_result_bands_mapped_score_min_max_valid", 
        constraint = "mapped_score_min <= mapped_score_max"
    )
})
public class RubricResultBandJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        updatable = false, 
        nullable = false, 
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "framework_result_band_id", nullable = false, updatable = false)
    private UUID frameworkResultBandId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "mapped_score_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal mappedScoreMin;

    @Column(name = "mapped_score_max", nullable = false, precision = 6, scale = 2)
    private BigDecimal mappedScoreMax;

    @Column(name = "result_order", nullable = false, check = {
        @CheckConstraint(
            name = "chk_order_valid", 
            constraint = "result_order > 0"
        )
    })
    private int order;

    @Column(name = "is_passing")
    private Boolean isPassing;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    
    protected RubricResultBandJpaEntity() {}


    public RubricResultBandJpaEntity(UUID id, UUID rubricVersionId, String code, String name, String description,
            BigDecimal mappedScoreMin, BigDecimal mappedScoreMax, int order, Boolean isPassing,
            UUID frameworkResultBandId, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.id = id;
        this.rubricVersionId = rubricVersionId;
        this.frameworkResultBandId = frameworkResultBandId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.mappedScoreMin = mappedScoreMin;
        this.mappedScoreMax = mappedScoreMax;
        this.order = order;
        this.isPassing = isPassing;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }


    public RubricResultBandJpaEntity(UUID rubricVersionId, String code, String name, String description,
            BigDecimal mappedScoreMin, BigDecimal mappedScoreMax, int order, Boolean isPassing,
            UUID frameworkResultBandId, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.rubricVersionId = rubricVersionId;
        this.frameworkResultBandId = frameworkResultBandId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.mappedScoreMin = mappedScoreMin;
        this.mappedScoreMax = mappedScoreMax;
        this.order = order;
        this.isPassing = isPassing;
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


    public UUID getRubricVersionId() {
        return rubricVersionId;
    }


    public void setRubricVersionId(UUID rubricVersionId) {
        this.rubricVersionId = rubricVersionId;
    }

    public UUID getFrameworkResultBandId() {
        return frameworkResultBandId;
    }

    public void setFrameworkResultBandId(UUID frameworkResultBandId) {
        this.frameworkResultBandId = frameworkResultBandId;
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


    public BigDecimal getMappedScoreMin() {
        return mappedScoreMin;
    }


    public void setMappedScoreMin(BigDecimal mappedScoreMin) {
        this.mappedScoreMin = mappedScoreMin;
    }


    public BigDecimal getMappedScoreMax() {
        return mappedScoreMax;
    }


    public void setMappedScoreMax(BigDecimal mappedScoreMax) {
        this.mappedScoreMax = mappedScoreMax;
    }


    public int getOrder() {
        return order;
    }


    public void setOrder(int order) {
        this.order = order;
    }


    public Boolean getIsPassing() {
        return isPassing;
    }


    public void setIsPassing(Boolean isPassing) {
        this.isPassing = isPassing;
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
