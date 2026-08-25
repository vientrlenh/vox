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
@Table(name = "order_items")
public class OrderItemJpaEntity {

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

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_order_items_type_valid",
            constraint = "type IN ('SUBSCRIPTION')"
        )
    })
    private String type;

    // Trỏ tới thực thể tương ứng với `type` (hiện chỉ SUBSCRIPTION -> subscription_plans.id).
    // KHÔNG FK được vì đa hình -- nếu về sau chỉ còn đúng một loại, đổi thành FK thật.
    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Column(name = "unit_price_vnd", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal unitPriceVnd;

    @Column(name = "amount_vnd", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal amountVnd;

    @Column(name = "quantity", nullable = false, updatable = false)
    private Integer quantity;

    protected OrderItemJpaEntity() {}

    public OrderItemJpaEntity(UUID id, UUID orderId, String type, UUID itemId, BigDecimal unitPriceVnd,
            BigDecimal amountVnd, Integer quantity) {
        this.id = id;
        this.orderId = orderId;
        this.type = type;
        this.itemId = itemId;
        this.unitPriceVnd = unitPriceVnd;
        this.amountVnd = amountVnd;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getUnitPriceVnd() {
        return unitPriceVnd;
    }

    public void setUnitPriceVnd(BigDecimal unitPriceVnd) {
        this.unitPriceVnd = unitPriceVnd;
    }

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public void setAmountVnd(BigDecimal amountVnd) {
        this.amountVnd = amountVnd;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
