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

import com.sep.vox.domain.dto.OrderItemDto;
import com.sep.vox.domain.repository.OrderItemRepository;

import reactor.core.publisher.Mono;

@Configuration
public class OrderGraphQLDataLoaderConfig {

    public OrderGraphQLDataLoaderConfig(
            BatchLoaderRegistry registry,
            OrderItemRepository orderItemRepository) {

        registry.<UUID, List<OrderItemDto>>forName("itemsByOrderId")
            .registerMappedBatchLoader((Set<UUID> orderIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    // Pre-fill List.of() for EVERY requested id. Required: a TOPUP order has no item rows, and
                    // for any key a mapped batch loader misses, the DataLoader returns null for that key -- the
                    // schema declares items: [OrderItem!], so null is still readable, but "the order has no
                    // items" and "the items could not be loaded" are two different things, so do not make the
                    // client guess.
                    Map<UUID, List<OrderItemDto>> result = new HashMap<>();
                    orderIds.forEach(id -> result.put(id, List.of()));

                    Map<UUID, List<OrderItemDto>> itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds).stream()
                        .map(OrderItemDto::toDto)
                        .collect(Collectors.groupingBy(item -> item.orderId()));

                    result.putAll(itemsByOrderId);
                    return result;
                })
        );
    }
}
