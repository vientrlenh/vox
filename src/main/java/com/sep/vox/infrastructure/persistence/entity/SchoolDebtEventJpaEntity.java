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

    @Column(name = "subscription_plan_id", nullable = false, updatable = false)
    private UUID subscriptionPlanId;

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
            constraint = "quota_type IN ('GRADING', 'CLASS_TEST', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "trigger_exam_session_id", updatable = false)
    private UUID triggerExamSessionId;

    @Column(name = "trigger_amount_usd", updatable = false, precision = 18, scale = 6)
    private BigDecimal triggerAmountUsd;

    @Column(name = "total_allocated_usd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal totalAllocatedUsd;

    @Column(name = "used_quantity_usd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal usedQuantityUsd;

    @Column(name = "overage_usd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal overageUsd;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SchoolDebtEventJpaEntity() {}

    public SchoolDebtEventJpaEntity(UUID id, UUID schoolId, UUID subscriptionPlanId, String eventType, String quotaType,
            UUID triggerExamSessionId, BigDecimal triggerAmountUsd, BigDecimal totalAllocatedUsd,
            BigDecimal usedQuantityUsd, BigDecimal overageUsd, Instant occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionPlanId = subscriptionPlanId;
        this.eventType = eventType;
        this.quotaType = quotaType;
        this.triggerExamSessionId = triggerExamSessionId;
        this.triggerAmountUsd = triggerAmountUsd;
        this.totalAllocatedUsd = totalAllocatedUsd;
        this.usedQuantityUsd = usedQuantityUsd;
        this.overageUsd = overageUsd;
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

    public UUID getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(UUID subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
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

    public BigDecimal getTriggerAmountUsd() {
        return triggerAmountUsd;
    }

    public void setTriggerAmountUsd(BigDecimal triggerAmountUsd) {
        this.triggerAmountUsd = triggerAmountUsd;
    }

    public BigDecimal getTotalAllocatedUsd() {
        return totalAllocatedUsd;
    }

    public void setTotalAllocatedUsd(BigDecimal totalAllocatedUsd) {
        this.totalAllocatedUsd = totalAllocatedUsd;
    }

    public BigDecimal getUsedQuantityUsd() {
        return usedQuantityUsd;
    }

    public void setUsedQuantityUsd(BigDecimal usedQuantityUsd) {
        this.usedQuantityUsd = usedQuantityUsd;
    }

    public BigDecimal getOverageUsd() {
        return overageUsd;
    }

    public void setOverageUsd(BigDecimal overageUsd) {
        this.overageUsd = overageUsd;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
