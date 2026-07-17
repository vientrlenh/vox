package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class FinancialEvent {
    private UUID id;
    private UUID schoolId;
    private UUID subscriptionId;
    private FinancialEventType eventType;
    private BigDecimal amountSigned;
    private String currency;
    private UUID actorId;
    private String payload;
    private OffsetDateTime occurredAt;

    public FinancialEvent() {}

    public FinancialEvent(UUID id, UUID schoolId, UUID subscriptionId, FinancialEventType eventType,
            BigDecimal amountSigned, String currency, UUID actorId, String payload, OffsetDateTime occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.amountSigned = amountSigned;
        this.currency = currency;
        this.actorId = actorId;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public FinancialEvent(UUID schoolId, UUID subscriptionId, FinancialEventType eventType,
            BigDecimal amountSigned, String currency, UUID actorId, String payload, OffsetDateTime occurredAt) {
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.amountSigned = amountSigned;
        this.currency = currency;
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

    public FinancialEventType getEventType() {
        return eventType;
    }

    public void setEventType(FinancialEventType eventType) {
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
