package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.model.subscription.Invoice;

public final class InvoiceDtoMapper {

    private InvoiceDtoMapper() {}

    public static InvoiceDto toDto(Invoice invoice) {
        return new InvoiceDto(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getSubscriptionId(),
            invoice.getSourceType() != null ? invoice.getSourceType().name() : null,
            invoice.getSourceId(),
            invoice.getIssueDate() != null ? invoice.getIssueDate().toString() : null,
            invoice.getAmount(),
            invoice.getStatus() != null ? invoice.getStatus().name() : null,
            invoice.getPaymentLinkId(),
            invoice.getCheckoutUrl(),
            invoice.getPaidAt() != null ? invoice.getPaidAt().toString() : null
        );
    }
}