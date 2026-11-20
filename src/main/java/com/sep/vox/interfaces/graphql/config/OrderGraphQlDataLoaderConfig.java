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
public class OrderGraphQlDataLoaderConfig {

    public OrderGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            OrderItemRepository orderItemRepository) {

        registry.<UUID, List<OrderItemDto>>forName("itemsByOrderId")
            .registerMappedBatchLoader((Set<UUID> orderIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    // Mồi sẵn List.of() cho MỌI id được hỏi. Bắt buộc: đơn TOPUP không có dòng nào, và
                    // mapped batch loader thiếu key nào thì DataLoader trả null cho key đó -- schema
                    // khai items: [OrderItem!] nên null còn đọc được, nhưng "đơn không có món" và "chưa
                    // nạp được" là hai chuyện khác nhau, đừng để client phải đoán.
                    Map<UUID, List<OrderItemDto>> result = new HashMap<>();
                    orderIds.forEach(id -> result.put(id, List.of()));

                    var itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds).stream()
                        .map(OrderItemDto::toDto)
                        .collect(Collectors.groupingBy(item -> item.orderId()));

                    result.putAll(itemsByOrderId);
                    return result;
                })
        );
    }
}
