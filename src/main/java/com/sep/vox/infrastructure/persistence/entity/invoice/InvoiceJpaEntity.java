package com.sep.vox.infrastructure.persistence.entity.invoice;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
 * Only completed orders are invoiced. The invoice NO LONGER carries the payment lifecycle (moved to Order) or the
 * gateway session (moved to PaymentRecord) -- it is just a document issued after the money has arrived.
 *
 * <p>Append-only: every column is {@code updatable = false}. An invoice that has been issued must not be edited; if
 * it is wrong, issue a new adjustment invoice.
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    // The SUCCESSFUL payment attempt of the order. There is exactly one PAID row per order (see
    // uq_payment_records_one_paid_per_order), so this column is not ambiguous.
    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "invoice_number", nullable = false, updatable = false, length = 255)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false, updatable = false)
    private Instant issueDate;
}
