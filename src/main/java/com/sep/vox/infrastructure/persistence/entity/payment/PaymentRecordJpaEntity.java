package com.sep.vox.infrastructure.persistence.entity.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ONE PAYMENT ATTEMPT. The row is created when the link is issued (PENDING) and exactly two columns may change when
 * the gateway reports the result: {@code status} and {@code paid_at}. Every other column is {@code updatable = false}
 * -- the amount, gateway and order code fixed when the link was issued must not be edited; if they are wrong, it is a
 * DIFFERENT attempt.
 *
 * <p>Paying again after a failure creates a NEW row with a new order code: both PayOS and SePay require their side's
 * order code to be unique, so there is no reusing the old code for the next attempt.
 */
@Entity
@Table(name = "payment_records")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRecordJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "amount_vnd", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal amountVnd;

    @Column(name = "method", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_payment_records_method_valid",
            constraint = "method IN ('E_BANKING', 'CARD')"
        )
    })
    private String method;

    @Column(name = "provider", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_payment_records_provider_valid",
            constraint = "provider IN ('PAYOS', 'SEPAY')"
        )
    })
    private String provider;

    // NOT updatable=false: this is the only column that carries the attempt's lifecycle (PENDING -> PAID/FAILED).
    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_payment_records_status_valid",
            constraint = "status IN ('PENDING', 'PAID', 'FAILED')"
        )
    })
    private String status;

    // Reconciliation key back to the gateway dashboard. Unique together with provider: the same transaction on the gateway
    // side must not be recorded twice when the webhook and PendingOrderReconciler process it at the same time.
    @Column(name = "provider_order_ref", nullable = false, updatable = false, length = 100)
    private String providerOrderRef;

    // The moment the GATEWAY recorded the transaction, different from created_at (when WE ISSUED THE LINK). Null until this
    // attempt brings in money, so it must be updatable -- filled in together with status = PAID.
    // The link issued for this attempt -- when the school clicks again, the same old link is returned instead of issuing a
    // new one, because uq_payment_records_one_pending_per_order only allows one pending attempt.
    //
    // MUST be updatable, for the same reason as paid_at below: the value only exists AFTER the gateway answers, while this
    // row was already INSERTed earlier (see CreatePaymentCheckoutUrlUseCase -- commit first, call the gateway after). With
    // updatable = false Hibernate silently drops the column from the UPDATE of attachCheckoutUrl: no error, no warning, the
    // column stays NULL forever. As a result the "return the same old link" branch in resolvePendingAttempt can never run --
    // a school that leaves the gateway page and comes back to pay again would hit the error "a payment session is already
    // waiting to be processed" instead of getting its own link back.
    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    // Gateway-specific data as JSON (the VietQR string + PayOS transfer information).
    // Does NOT contain signatures -- the SePay FORM_POST field set deliberately never goes in here.
    //
    // MUST be updatable, for the same reason as checkout_url: the value only exists AFTER the gateway answers, while this row
    // was already INSERTed earlier. This is the only place that keeps the QR code so it can be rebuilt when the school
    // returns to an old order -- asking for the status only returns the status, the gateway does not issue the code again.
    @Column(name = "provider_payload_json")
    private String providerPayloadJson;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
