package com.sep.vox.domain.model.order;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {
    private UUID id;
    private UUID orderId;
    private OrderItemType type;
    private UUID itemId;
    private BigDecimal unitPriceVnd;
    private BigDecimal amountVnd;
    private Integer quantity;

    public OrderItem() {}

    public OrderItem(UUID id, UUID orderId, OrderItemType type, UUID itemId, BigDecimal unitPriceVnd,
            BigDecimal amountVnd, Integer quantity) {
        this.id = id;
        this.orderId = orderId;
        this.type = type;
        this.itemId = itemId;
        this.unitPriceVnd = unitPriceVnd;
        this.amountVnd = amountVnd;
        this.quantity = quantity;
    }

    public OrderItem(UUID orderId, OrderItemType type, UUID itemId, BigDecimal unitPriceVnd, BigDecimal amountVnd,
            Integer quantity) {
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

    public OrderItemType getType() {
        return type;
    }

    public void setType(OrderItemType type) {
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
