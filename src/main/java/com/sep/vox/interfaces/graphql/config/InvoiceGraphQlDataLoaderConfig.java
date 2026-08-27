package com.sep.vox.interfaces.graphql.config;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.repository.InvoiceRepository;

import reactor.core.publisher.Mono;

@Configuration
public class InvoiceGraphQlDataLoaderConfig {
    
    public InvoiceGraphQlDataLoaderConfig(
        BatchLoaderRegistry registry, 
        InvoiceRepository invoiceRepository
    ) {
        registry.<UUID, InvoiceDto>forName("invoiceByOrderId")
            .registerMappedBatchLoader((Set<UUID> orderIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() ->
                    invoiceRepository.findByOrderIdIn(orderIds).stream()
                        // Gom theo domain model chứ không theo DTO vì InvoiceDto cố ý không mang
                        // orderId -- xem InvoiceDto. toMap không có merge function là CỐ Ý: quan hệ
                        // 1-1 được uq_invoices_order chặn ở DB, nên hai hóa đơn trên cùng một đơn là
                        // dữ liệu hỏng chứ không phải trường hợp cần chọn một dòng để hiển thị.
                        .collect(Collectors.toMap(invoice -> invoice.getOrderId(), InvoiceDto::toDto))
            )
        );
    }

       
}
