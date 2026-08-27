package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.invoice.Invoice;
import com.sep.vox.infrastructure.persistence.entity.InvoiceJpaEntity;

public final class InvoiceMapper {

    private InvoiceMapper() {}

    public static Invoice toDomain(InvoiceJpaEntity jpa) {
        return new Invoice(
            jpa.getId(),
            jpa.getOrderId(), 
            jpa.getPaymentId(), 
            jpa.getInvoiceNumber(), 
            jpa.getIssueDate()
        );
    }

    public static InvoiceJpaEntity toJpa(Invoice domain) {
        return new InvoiceJpaEntity(
            domain.getId(),
            domain.getOrderId(), 
            domain.getPaymentId(), 
            domain.getInvoiceNumber(), 
            domain.getIssueDate()
        );
    }
}
