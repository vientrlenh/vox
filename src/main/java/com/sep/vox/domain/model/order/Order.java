package com.sep.vox.domain.model.order;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class Order extends BaseModel {

    /**
     * How long a pending order lives before it is considered abandoned. 24 hours because schools mostly pay by bank
     * transfer -- an order placed late in the afternoon is only approved by accounting the next morning.
     *
     * <p>The constant lives on the model, not in config: it is a number that TRAVELS WITH each order (written to
     * expires_at at creation and sent to the gateway), so changing it only affects new orders. If it ever needs to be
     * tuned per environment, turn it into a property and pass it to the factory; do not read config when checking expiry
     * -- that would change the deadline of orders that are already pending.
     */
    public static final Duration PENDING_TTL = Duration.ofHours(24);

    private UUID schoolId;
    private OrderType type;
    private String description;
    // Goods total BEFORE fee and discount. Invariant: totalAmountVnd = subtotalAmountVnd + chargedFeeVnd
    // - discountAmountVnd (chk_orders_amounts_balance in the database enforces it as well, to be safe).
    private BigDecimal subtotalAmountVnd;
    private BigDecimal totalAmountVnd;
    private BigDecimal chargedFeeVnd;
    private BigDecimal discountAmountVnd;
    private OrderStatus status;
    private String notes;
    // Payment deadline, fixed when the order is created -- see the expires_at column in V2.
    private Instant expiresAt;
    // Must be carried on the domain model, not only on the JpaEntity: the mapper builds a NEW entity on every save, so the
    // entity is always detached -- without the version, Hibernate treats it as transient and INSERTs over the existing id.
    private Long version;

    public Order() {}



    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public BigDecimal getSubtotalAmountVnd() {
        return subtotalAmountVnd;
    }

    public void setSubtotalAmountVnd(BigDecimal subtotalAmountVnd) {
        this.subtotalAmountVnd = subtotalAmountVnd;
    }

    public BigDecimal getTotalAmountVnd() {
        return totalAmountVnd;
    }

    public void setTotalAmountVnd(BigDecimal totalAmountVnd) {
        this.totalAmountVnd = totalAmountVnd;
    }

    public BigDecimal getChargedFeeVnd() {
        return chargedFeeVnd;
    }

    public void setChargedFeeVnd(BigDecimal chargedFeeVnd) {
        this.chargedFeeVnd = chargedFeeVnd;
    }

    public BigDecimal getDiscountAmountVnd() {
        return discountAmountVnd;
    }

    public void setDiscountAmountVnd(BigDecimal discountAmountVnd) {
        this.discountAmountVnd = discountAmountVnd;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * An order for one billing period of a plan. The order amount equals the plan's list price exactly, WITHOUT a service
     * fee: the plan's margin is already inside priceVnd, which the admin sets (selling 10 million for 8 million of quota).
     */
    public static Order forSubscription(UUID schoolId, OrderType type, String description, BigDecimal planPriceVnd, BigDecimal serviceFeeVnd,
            Instant now) {
        return builder()
            .schoolId(schoolId)
            .type(type)
            .description(description)
            .subtotalAmountVnd(planPriceVnd)
            .totalAmountVnd(planPriceVnd.add(serviceFeeVnd))
            .chargedFeeVnd(serviceFeeVnd)
            .discountAmountVnd(BigDecimal.ZERO)
            .status(OrderStatus.PENDING)
            .expiresAt(now.plus(PENDING_TTL))
            .build();
    }

    /**
     * An UPGRADE order: buying a new plan that takes effect immediately, minus the unused part of the running period.
     *
     * <p>The credit goes into {@code discountAmountVnd} rather than being subtracted from the subtotal: the subtotal must
     * keep the new plan's list price for the invoice to be readable -- "plan 12,000,000, minus 700,000 unused from the old
     * plan, 11,300,000 to pay". Subtracting from the subtotal would record a plan price that never existed.
     *
     * <p>A separate factory instead of a discount parameter on {@link #forSubscription}: registration and renewal NEVER
     * get this kind of discount (the new period is chained, so no day is lost), and giving them a parameter that is
     * always 0 invites someone to pass the wrong value one day.
     */
    public static Order forSubscriptionUpgrade(UUID schoolId, String description, BigDecimal planPriceVnd,
            BigDecimal unusedCreditVnd, BigDecimal serviceFeeVnd, Instant now) {
        return builder()
            .schoolId(schoolId)
            .type(OrderType.SUBSCRIPTION_UPGRADE)
            .description(description)
            .subtotalAmountVnd(planPriceVnd)
            .totalAmountVnd(planPriceVnd.subtract(unusedCreditVnd).add(serviceFeeVnd))
            .chargedFeeVnd(serviceFeeVnd)
            .discountAmountVnd(unusedCreditVnd)
            .status(OrderStatus.PENDING)
            .expiresAt(now.plus(PENDING_TTL))
            .build();
    }

    /**
     * A top-up order for the balance. The service fee is ADDED, not carved out: the school receives exactly
     * creditAmountVnd in the wallet and pays creditAmountVnd + serviceFeeVnd.
     *
     * <p>The amount credited is subtotalAmountVnd, WRITTEN DIRECTLY instead of derived from total - fee: a top-up order
     * has no order_items to add back up, so that back-calculation would be the only source -- and it is wrong as soon as
     * discountAmountVnd is non-zero, wrong in the direction of crediting the school too much.
     */
    public static Order forTopUp(UUID schoolId, String description, BigDecimal creditAmountVnd,
            BigDecimal serviceFeeVnd, Instant now) {
        return builder()
            .schoolId(schoolId)
            .type(OrderType.TOPUP)
            .description(description)
            .subtotalAmountVnd(creditAmountVnd)
            .totalAmountVnd(creditAmountVnd.add(serviceFeeVnd))
            .chargedFeeVnd(serviceFeeVnd)
            .discountAmountVnd(BigDecimal.ZERO)
            .status(OrderStatus.PENDING)
            .expiresAt(now.plus(PENDING_TTL))
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<Order, Builder> {

        private Builder() {
            super(new Order());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder type(OrderType type) {
            target.type = type;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder subtotalAmountVnd(BigDecimal subtotalAmountVnd) {
            target.subtotalAmountVnd = subtotalAmountVnd;
            return this;
        }

        public Builder totalAmountVnd(BigDecimal totalAmountVnd) {
            target.totalAmountVnd = totalAmountVnd;
            return this;
        }

        public Builder chargedFeeVnd(BigDecimal chargedFeeVnd) {
            target.chargedFeeVnd = chargedFeeVnd;
            return this;
        }

        public Builder discountAmountVnd(BigDecimal discountAmountVnd) {
            target.discountAmountVnd = discountAmountVnd;
            return this;
        }

        public Builder status(OrderStatus status) {
            target.status = status;
            return this;
        }

        public Builder notes(String notes) {
            target.notes = notes;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            target.expiresAt = expiresAt;
            return this;
        }

        public Builder version(Long version) {
            target.version = version;
            return this;
        }

    }
}
