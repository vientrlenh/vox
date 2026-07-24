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
import jakarta.persistence.Table;

@Entity
@Table(name = "financial_event")
public class FinancialEventJpaEntity {

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

    @Column(name = "subscription_id", updatable = false)
    private UUID subscriptionId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_financial_event_event_type_valid",
            constraint = "event_type IN ('SUB_PURCHASED', 'SUB_RENEWED', 'SUB_CANCELLED', 'SUB_UPGRADED', 'TOKEN_PURCHASED', 'TOKEN_CONSUMED', 'REFUND_ISSUED')"
        )
    })
    private String eventType;

    @Column(name = "amount_signed", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal amountSigned;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "payment_method", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_financial_event_payment_method_valid",
            constraint = "payment_method IN ('PAYOS', 'MANUAL')"
        )
    })
    private String paymentMethod;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "payload", updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    protected FinancialEventJpaEntity() {}

    public FinancialEventJpaEntity(UUID id, UUID schoolId, UUID subscriptionId, String eventType, BigDecimal amountSigned,
            String currency, String paymentMethod, UUID actorId, String payload, OffsetDateTime occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.amountSigned = amountSigned;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.actorId = actorId;
        this.payload = payload;
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

    public BigDecimal getAmountSigned() {
        return amountSigned;
    }

    public void setAmountSigned(BigDecimal amountSigned) {
        this.amountSigned = amountSigned;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
