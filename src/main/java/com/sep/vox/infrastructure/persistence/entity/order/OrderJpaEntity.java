package com.sep.vox.infrastructure.persistence.entity.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The table name MUST be PLURAL: {@code order} is an SQL keyword and cannot be used as a bare table name.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(columnList = "school_id", name = "idx_orders_school_id")
}, check = {
    @CheckConstraint(
        name = "chk_orders_total_amount_vnd_matches_amount_combination", 
        constraint = "total_amount_vnd = subtotal_amount_vnd + charged_fee_vnd - discount_amount_vnd"
    ), 
    @CheckConstraint(
        name = "chk_orders_discount_amount_vnd_lower_or_equals_than_subtotal_and_charged_fee", 
        constraint = "discount_amount_vnd <= subtotal_amount_vnd + charged_fee_vnd"
    )
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_orders_type_valid",
            constraint = "type IN ('SUBSCRIPTION_REQUEST', 'SUBSCRIPTION_UPGRADE', 'TOPUP')"
        )
    })
    private String type;

    // Description shown to the school (and sent to the gateway as the transfer content) -- fixed when the order is created
    // so that invoices printed later do not change with the plan's current name.
    @Column(name = "description", updatable = false, length = 512)
    private String description;

    // Goods total BEFORE fee and discount; for a TOPUP order this is exactly the balance the school receives.
    @Column(name = "subtotal_amount_vnd", nullable = false, precision = 15, scale = 0, check = {
        @CheckConstraint(
            name = "chk_orders_subtotal_amount_vnd_non_negative", 
            constraint = "subtotal_amount_vnd >= 0"
        )
    })
    private BigDecimal subtotalAmountVnd;

    // numeric(15,0): this is REAL money passing through the payment gateway, and PayOS/SePay cannot transact fractional
    // dong. Different from school_balance_entries.amount_vnd -- that is a usage-measured balance, so it needs decimals.
    @Column(name = "total_amount_vnd", nullable = false, precision = 15, scale = 0, check = {
        @CheckConstraint(
            name = "chk_orders_total_amount_vnd_non_negative", 
            constraint = "total_amount_vnd >= 0"
        )
    })
    private BigDecimal totalAmountVnd;

    @Column(name = "charged_fee_vnd", nullable = false, precision = 15, scale = 0, check = {
        @CheckConstraint(
            name = "chk_orders_charged_fee_vnd_non_negative", 
            constraint = "charged_fee_vnd >= 0"
        )
    })
    private BigDecimal chargedFeeVnd;

    @Column(name = "discount_amount_vnd", nullable = false, precision = 15, scale = 0, check = {
        @CheckConstraint(
            name = "chk_orders_discount_amount_vnd_non_negative", 
            constraint = "discount_amount_vnd >= 0"
        )
    })
    private BigDecimal discountAmountVnd;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_orders_status_valid",
            constraint = "status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED')"
        )
    })
    private String status;

    // Internal note by the System Admin (e.g. the reason an order was cancelled/rejected) -- NOT updatable=false because
    // this is the only part of an order that may be edited after creation.
    @Column(name = "notes", length = 2048)
    private String notes;

    // updatable = false: the deadline is a commitment fixed when the order is created and already sent to the gateway --
    // to extend it, cancel the order and place it again; do not quietly change the deadline of a pending order.
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    // The gateway webhook and PendingOrderReconciler may both move an order to SUCCESS a few minutes apart -- @Version
    // makes the second write fail instead of silently overwriting.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
