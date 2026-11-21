package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.invoice.Invoice;
import com.sep.vox.infrastructure.persistence.entity.invoice.InvoiceJpaEntity;

public final class InvoiceMapper {

    private InvoiceMapper() {}

    public static Invoice toDomain(InvoiceJpaEntity jpa) {
        return Invoice.builder()
            .id(jpa.getId())
            .orderId(jpa.getOrderId())
            .paymentId(jpa.getPaymentId())
            .invoiceNumber(jpa.getInvoiceNumber())
            .issueDate(jpa.getIssueDate())
            .build();
    }

    public static InvoiceJpaEntity toJpa(Invoice domain) {
        return InvoiceJpaEntity.builder()
            .id(domain.getId())
            .orderId(domain.getOrderId())
            .paymentId(domain.getPaymentId())
            .invoiceNumber(domain.getInvoiceNumber())
            .issueDate(domain.getIssueDate())
            .build();
    }
}
