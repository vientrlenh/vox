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
@Table(name = "school_debt_events")
public class SchoolDebtEventJpaEntity {

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

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID subscriptionId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_debt_event_event_type_valid",
            constraint = "event_type IN ('LOCKED', 'CAP_EXCEEDED', 'CLEARED')"
        )
    })
    private String eventType;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_debt_events_quota_type_valid",
            constraint = "quota_type IN ('EXAM', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "trigger_exam_session_id", updatable = false)
    private UUID triggerExamSessionId;

    @Column(name = "trigger_amount_vnd", updatable = false, precision = 18, scale = 6)
    private BigDecimal triggerAmountVnd;

    @Column(name = "total_allocated_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal totalAllocatedVnd;

    @Column(name = "used_amount_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal usedAmountVnd;

    @Column(name = "overage_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal overageVnd;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SchoolDebtEventJpaEntity() {}

    public SchoolDebtEventJpaEntity(UUID id, UUID schoolId, UUID subscriptionId, String eventType, String quotaType,
            UUID triggerExamSessionId, BigDecimal triggerAmountVnd, BigDecimal totalAllocatedVnd,
            BigDecimal usedAmountVnd, BigDecimal overageVnd, Instant occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.quotaType = quotaType;
        this.triggerExamSessionId = triggerExamSessionId;
        this.triggerAmountVnd = triggerAmountVnd;
        this.totalAllocatedVnd = totalAllocatedVnd;
        this.usedAmountVnd = usedAmountVnd;
        this.overageVnd = overageVnd;
        this.occurredAt = occurredAt;
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

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
        this.quotaType = quotaType;
    }

    public UUID getTriggerExamSessionId() {
        return triggerExamSessionId;
    }

    public void setTriggerExamSessionId(UUID triggerExamSessionId) {
        this.triggerExamSessionId = triggerExamSessionId;
    }

    public BigDecimal getTriggerAmountVnd() {
        return triggerAmountVnd;
    }

    public void setTriggerAmountVnd(BigDecimal triggerAmountVnd) {
        this.triggerAmountVnd = triggerAmountVnd;
    }

    public BigDecimal getTotalAllocatedVnd() {
        return totalAllocatedVnd;
    }

    public void setTotalAllocatedVnd(BigDecimal totalAllocatedVnd) {
        this.totalAllocatedVnd = totalAllocatedVnd;
    }

    public BigDecimal getUsedAmountVnd() {
        return usedAmountVnd;
    }

    public void setUsedAmountVnd(BigDecimal usedAmountVnd) {
        this.usedAmountVnd = usedAmountVnd;
    }

    public BigDecimal getOverageVnd() {
        return overageVnd;
    }

    public void setOverageVnd(BigDecimal overageVnd) {
        this.overageVnd = overageVnd;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
