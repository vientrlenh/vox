package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FinancialEvent {
    private UUID id;
    private UUID schoolId;
    private UUID subscriptionId;
    private FinancialEventType eventType;
    private BigDecimal amountSigned;
    private String currency;
    private PaymentMethod paymentMethod;
    private UUID actorId;
    private String payload;
    private Instant occurredAt;

    public FinancialEvent() {}

    public FinancialEvent(UUID id, UUID schoolId, UUID subscriptionId, FinancialEventType eventType,
            BigDecimal amountSigned, String currency, PaymentMethod paymentMethod, UUID actorId, String payload, Instant occurredAt) {
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

    public FinancialEvent(UUID schoolId, UUID subscriptionId, FinancialEventType eventType,
            BigDecimal amountSigned, String currency, PaymentMethod paymentMethod, UUID actorId, String payload, Instant occurredAt) {
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
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

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
