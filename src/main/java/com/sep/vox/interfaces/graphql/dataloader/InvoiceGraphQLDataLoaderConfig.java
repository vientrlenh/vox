package com.sep.vox.interfaces.graphql.dataloader;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.repository.InvoiceRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class InvoiceGraphQLDataLoaderConfig {

    public static final String INVOICE_BY_ORDER_ID = "invoiceByOrderId";

    private final BatchLoaderRegistry registry;
    private final InvoiceRepository invoiceRepository;

    @PostConstruct
    void registerLoaders() {
        registerInvoiceByOrderId();
    }

    private void registerInvoiceByOrderId() {
        registry.<UUID, InvoiceDto>forName(INVOICE_BY_ORDER_ID)
            .registerMappedBatchLoader((Set<UUID> orderIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() ->
                    invoiceRepository.findByOrderIdIn(orderIds).stream()
                        // Group by the domain model and not by the DTO, because InvoiceDto deliberately does not
                        // carry orderId -- see InvoiceDto. toMap has no merge function ON PURPOSE: the 1-1
                        // relation is enforced by uq_invoices_order in the database, so two invoices on the same
                        // order are corrupt data and not a case where one row should be picked for display.
                        .collect(Collectors.toMap(invoice -> invoice.getOrderId(), InvoiceDto::toDto))
                )
            );
    }
}
