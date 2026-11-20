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
public class PaymentGraphQlDataLoaderConfig {
    public PaymentGraphQlDataLoaderConfig(
        BatchLoaderRegistry registry,
        PaymentRecordRepository paymentRecordRepository
    ) {
        registry.<UUID, List<PaymentDto>>forName("paymentsByOrderId")
            .registerMappedBatchLoader((Set<UUID> orderIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    // Mồi List.of() vì lý do giống itemsByOrderId: đơn vừa tạo mà trường chưa bấm
                    // thanh toán lần nào thì chưa có dòng payment_records nào cả.
                    Map<UUID, List<PaymentDto>> result = new HashMap<>();
                    orderIds.forEach(id -> result.put(id, List.of()));

                    // groupingBy giữ nguyên thứ tự stream, nên thứ tự "mới nhất trước" đã chốt từ câu
                    // truy vấn (findByOrderIdInOrderByIdDesc) được bảo toàn tới đây.
                    var paymentsByOrderId = paymentRecordRepository.findByOrderIdIn(orderIds).stream()
                        .map(PaymentDto::toDto)
                        .collect(Collectors.groupingBy(payment -> payment.orderId()));

                    result.putAll(paymentsByOrderId);
                    return result;
                })
        );
    }
}
