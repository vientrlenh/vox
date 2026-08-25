package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.infrastructure.persistence.entity.InvoiceJpaEntity;

public final class InvoiceMapper {

    private InvoiceMapper() {}

    public static Invoice toDomain(InvoiceJpaEntity jpa) {
        return new Invoice(
            jpa.getId(),
            jpa.getInvoiceNumber(),
            jpa.getSchoolId(),
            jpa.getSubscriptionId(),
            InvoiceSourceType.valueOf(jpa.getSourceType()),
            jpa.getSourceId(),
            jpa.getIssueDate(),
            jpa.getAmount(),
            InvoiceStatus.valueOf(jpa.getStatus()),
            PaymentMethod.valueOf(jpa.getPaymentProvider()),
            jpa.getProviderOrderRef(),
            jpa.getPaymentLinkId(),
            jpa.getCheckoutUrl(),
            jpa.getPaidAt(),
            jpa.getResolvedPlanId(),
            jpa.getCreatedBy()
        );
    }

    public static InvoiceJpaEntity toJpa(Invoice domain) {
        return new InvoiceJpaEntity(
            domain.getId(),
            domain.getInvoiceNumber(),
            domain.getSchoolId(),
            domain.getSubscriptionId(),
            domain.getSourceType().name(),
            domain.getSourceId(),
            domain.getIssueDate(),
            domain.getAmount(),
            domain.getStatus().name(),
            domain.getPaymentProvider().name(),
            domain.getProviderOrderRef(),
            domain.getPaymentLinkId(),
            domain.getCheckoutUrl(),
            domain.getPaidAt(),
            domain.getResolvedPlanId(),
            domain.getCreatedBy()
        );
    }
}
