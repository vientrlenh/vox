package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TokenPurchase {
    private UUID id;
    private UUID subscriptionId;
    private BigDecimal totalAmount;
    private PurchaseStatus status;
    private Instant purchasedAt;

    public TokenPurchase() {}

    public TokenPurchase(UUID id, UUID subscriptionId, BigDecimal totalAmount, PurchaseStatus status, Instant purchasedAt) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.purchasedAt = purchasedAt;
    }

    public TokenPurchase(UUID subscriptionId, BigDecimal totalAmount, PurchaseStatus status, Instant purchasedAt) {
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

    public Instant getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(Instant purchasedAt) {
        this.purchasedAt = purchasedAt;
    }
}
