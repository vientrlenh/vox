package com.sep.vox.domain.dto;

import java.time.Instant;

import com.sep.vox.domain.model.invoice.Invoice;

/**
 * The receipt of an order that has been fully paid. It has only two fields, and that is everything
 * the invoice knows by itself -- the amount, what was bought and how it was paid all live on Order
 * and PaymentRecord. That is why this DTO is NOT able to stand alone: it always appears as a field
 * of an order (see {@code Order.invoice}).
 *
 * <p>There is NO {@code orderId}: the invoice is nested inside its own order, so copying the id of
 * the parent is redundant. This is where it differs from {@link OrderItemDto} -- an order line has to
 * keep {@code orderId} because the batch loader groups by it, while the invoice is a 1-1 relation, so
 * it is grouped by the domain model itself before being mapped to the DTO.
 *
 * <p>There is NO {@code paymentId}: the reader does not need a pointer, they need the facts of the
 * payment -- {@code Order.payments} answers that.
 *
 * <p>There is NO {@code id}: {@code invoiceNumber} is already unique and is what people read out to
 * each other over the phone, so if a document download path appears later, its key should be the
 * invoice number ({@code InvoiceRepository.findByInvoiceNumber} is already there) rather than a UUID.
 */
public record InvoiceDto(
    String invoiceNumber,
    Instant issueDate
) {

    public static InvoiceDto toDto(Invoice invoice) {
        return new InvoiceDto(
            invoice.getInvoiceNumber(),
            invoice.getIssueDate()
        );
    }
}
