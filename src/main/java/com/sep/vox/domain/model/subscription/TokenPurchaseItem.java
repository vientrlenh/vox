package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

public class TokenPurchaseItem {
    private UUID id;
    private UUID purchaseId;
    private QuotaType quotaType;
    private BigDecimal quantity;
    private BigDecimal unitPriceSnapshot;
    private BigDecimal subtotal;

    public TokenPurchaseItem() {}

    public TokenPurchaseItem(UUID id, UUID purchaseId, QuotaType quotaType, BigDecimal quantity,
            BigDecimal unitPriceSnapshot, BigDecimal subtotal) {
        this.id = id;
        this.purchaseId = purchaseId;
        this.quotaType = quotaType;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.subtotal = subtotal;
    }

    public TokenPurchaseItem(UUID purchaseId, QuotaType quotaType, BigDecimal quantity,
            BigDecimal unitPriceSnapshot, BigDecimal subtotal) {
        this.purchaseId = purchaseId;
        this.quotaType = quotaType;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.subtotal = subtotal;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(UUID purchaseId) {
        this.purchaseId = purchaseId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) {
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
