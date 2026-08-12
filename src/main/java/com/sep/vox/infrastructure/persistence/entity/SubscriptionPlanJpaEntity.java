package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscription_plan")
public class SubscriptionPlanJpaEntity {

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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tagline")
    private String tagline;

    @Column(name = "price_per_year", nullable = false, precision = 15, scale = 0)
    private BigDecimal pricePerYear;

    @Column(name = "validity_days", nullable = false)
    private Integer validityDays;

    @Column(name = "max_time_per_attempt_min", nullable = false)
    private Integer maxTimePerAttemptMin;

    @Column(name = "max_student_count", nullable = false)
    private Integer maxStudentCount;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_subscription_plan_status_valid",
            constraint = "status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "replaced_by_plan_id")
    private UUID replacedByPlanId;

    protected SubscriptionPlanJpaEntity() {}

    public SubscriptionPlanJpaEntity(UUID id, String name, String tagline, BigDecimal pricePerYear, Integer validityDays,
            Integer maxTimePerAttemptMin, Integer maxStudentCount, String status, Integer version,
            Instant createdAt, UUID createdBy, UUID replacedByPlanId) {
        this.id = id;
        this.name = name;
        this.tagline = tagline;
        this.pricePerYear = pricePerYear;
        this.validityDays = validityDays;
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
        this.maxStudentCount = maxStudentCount;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.replacedByPlanId = replacedByPlanId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public BigDecimal getPricePerYear() {
        return pricePerYear;
    }

    public void setPricePerYear(BigDecimal pricePerYear) {
        this.pricePerYear = pricePerYear;
    }

    public Integer getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;
    }

    public Integer getMaxTimePerAttemptMin() {
        return maxTimePerAttemptMin;
    }

    public void setMaxTimePerAttemptMin(Integer maxTimePerAttemptMin) {
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
    }

    public Integer getMaxStudentCount() {
        return maxStudentCount;
    }

    public void setMaxStudentCount(Integer maxStudentCount) {
        this.maxStudentCount = maxStudentCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getReplacedByPlanId() {
        return replacedByPlanId;
    }

    public void setReplacedByPlanId(UUID replacedByPlanId) {
        this.replacedByPlanId = replacedByPlanId;
    }
}
