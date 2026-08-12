package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "token_purchase_item")
public class TokenPurchaseItemJpaEntity {

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

    @Column(name = "purchase_id", nullable = false, updatable = false)
    private UUID purchaseId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_token_purchase_item_quota_type_valid",
            constraint = "quota_type IN ('GRADING', 'CLASS_TEST', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_price_snapshot", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "subtotal", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal subtotal;

    protected TokenPurchaseItemJpaEntity() {}

    public TokenPurchaseItemJpaEntity(UUID id, UUID purchaseId, String quotaType, BigDecimal quantity,
            BigDecimal unitPriceSnapshot, BigDecimal subtotal) {
        this.id = id;
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

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
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
