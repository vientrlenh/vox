package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TokenPurchase {
    private UUID id;
    private UUID subscriptionId;
    private BigDecimal totalAmount;
    private PurchaseStatus status;
    private OffsetDateTime purchasedAt;

    public TokenPurchase() {}

    public TokenPurchase(UUID id, UUID subscriptionId, BigDecimal totalAmount, PurchaseStatus status, OffsetDateTime purchasedAt) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.purchasedAt = purchasedAt;
    }

    public TokenPurchase(UUID subscriptionId, BigDecimal totalAmount, PurchaseStatus status, OffsetDateTime purchasedAt) {
        this.subscriptionId = subscriptionId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.purchasedAt = purchasedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }

    public OffsetDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(OffsetDateTime purchasedAt) {
        this.purchasedAt = purchasedAt;
    }
}
