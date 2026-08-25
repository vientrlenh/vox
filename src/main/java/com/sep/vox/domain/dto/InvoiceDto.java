package com.sep.vox.domain.dto;

import java.util.UUID;

import com.sep.vox.domain.model.invoice.Invoice;

public record InvoiceDto(
    UUID id, 
    UUID orderId, 
    UUID paymentId, 
    String invoiceNumber, 
    String issueDate
) {

    public static InvoiceDto toDto(Invoice invoice) {
        return new InvoiceDto(
            invoice.getId(), 
            invoice.getOrderId(), 
            invoice.getPaymentId(), 
            invoice.getInvoiceNumber(), 
            invoice.getIssueDate().toString()
        );
    }
}
