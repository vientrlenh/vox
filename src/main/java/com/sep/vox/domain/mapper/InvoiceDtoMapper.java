package com.sep.vox.domain.mapper;

import java.time.LocalDate;
import java.util.List;

import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.model.subscription.Invoice;

public final class InvoiceDtoMapper {

    private InvoiceDtoMapper() {
    }

    public static InvoiceDto toDto(Invoice domain) {
        return new InvoiceDto(
            domain.getId(),
            domain.getInvoiceNumber(),
            domain.getSubscriptionId(),
            domain.getSourceType().name(),
            domain.getSourceId(),
            valueOf(domain.getIssueDate()),
            domain.getAmount(),
            domain.getStatus().name()
        );
    }

    public static List<InvoiceDto> toDtoList(List<Invoice> domains) {
        return domains.stream().map(InvoiceDtoMapper::toDto).toList();
    }

    private static String valueOf(LocalDate value) {
        return value == null ? null : value.toString();
    }
}
