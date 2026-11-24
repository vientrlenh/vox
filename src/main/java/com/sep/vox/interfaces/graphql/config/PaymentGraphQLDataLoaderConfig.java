package com.sep.vox.interfaces.graphql.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.PaymentDto;
import com.sep.vox.domain.repository.PaymentRecordRepository;

import reactor.core.publisher.Mono;

@Configuration
public class PaymentGraphQLDataLoaderConfig {
    public PaymentGraphQLDataLoaderConfig(
        BatchLoaderRegistry registry,
        PaymentRecordRepository paymentRecordRepository
    ) {
        registry.<UUID, List<PaymentDto>>forName("paymentsByOrderId")
            .registerMappedBatchLoader((Set<UUID> orderIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    // Pre-fill List.of() for the same reason as itemsByOrderId: an order that was just created
                    // and for which the school has not pressed pay even once has no payment_records rows yet.
                    Map<UUID, List<PaymentDto>> result = new HashMap<>();
                    orderIds.forEach(id -> result.put(id, List.of()));

                    // groupingBy keeps the stream order, so the "newest first" order fixed by the query
                    // (findByOrderIdInOrderByIdDesc) is preserved up to here.
                    Map<UUID, List<PaymentDto>> paymentsByOrderId = paymentRecordRepository.findByOrderIdIn(orderIds).stream()
                        .map(PaymentDto::toDto)
                        .collect(Collectors.groupingBy(payment -> payment.orderId()));

                    result.putAll(paymentsByOrderId);
                    return result;
                })
        );
    }
}
