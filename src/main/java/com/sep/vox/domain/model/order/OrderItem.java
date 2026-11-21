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

    

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final OrderItem target = new OrderItem();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder orderId(UUID orderId) {
            target.orderId = orderId;
            return this;
        }

        public Builder type(OrderItemType type) {
            target.type = type;
            return this;
        }

        public Builder itemId(UUID itemId) {
            target.itemId = itemId;
            return this;
        }

        public Builder unitPriceVnd(BigDecimal unitPriceVnd) {
            target.unitPriceVnd = unitPriceVnd;
            return this;
        }

        public Builder amountVnd(BigDecimal amountVnd) {
            target.amountVnd = amountVnd;
            return this;
        }

        public Builder quantity(Integer quantity) {
            target.quantity = quantity;
            return this;
        }

        public OrderItem build() {
            return target;
        }

    }
}
