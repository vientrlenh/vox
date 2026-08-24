package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

// Catalog khối lớp dùng chung TOÀN HỆ THỐNG (trước đây mỗi trường tự khai một bộ trong
// school_grade_levels) -- vì vậy unique giờ chỉ trên code/order, không còn kèm school_id.
@Entity
@Table(name = "grade_levels", indexes = {
    @Index(columnList = "code", name = "idx_grade_levels_code", unique = true),
    @Index(columnList = "grade_level_order", name = "idx_grade_levels_order", unique = true)
})
public class GradeLevelJpaEntity {
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

    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    // "order" là từ khoá SQL nên cột giữ tên grade_level_order như bảng cũ.
    @Column(name = "grade_level_order", nullable = false, check = {
        @CheckConstraint(
            name = "chk_grade_levels_order_positive",
            constraint = "grade_level_order > 0"
        )
    })
    private int order;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_grade_levels_status_valid",
            constraint = "status IN ('ACTIVE', 'INACTIVE')"
        )
    })
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected GradeLevelJpaEntity() {}

    public GradeLevelJpaEntity(UUID id, String code, String name, String description, int order,
            String status, Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
}
