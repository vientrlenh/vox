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

import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.dto.OrderItemDto;
import com.sep.vox.domain.dto.PaymentDto;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.OrderItemRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;

import reactor.core.publisher.Mono;

@Configuration
public class OrderGraphQlDataLoaderConfig {

    public OrderGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            OrderItemRepository orderItemRepository,
            InvoiceRepository invoiceRepository,
            PaymentRecordRepository paymentRecordRepository) {

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

        // KHÔNG mồi sẵn như hai loader kia, và đây là chỗ khác nhau có chủ đích: hóa đơn chỉ phát cho
        // đơn đã thu đủ tiền, nên phần lớn đơn KHÔNG có hóa đơn và null chính là câu trả lời đúng
        // ("đơn này chưa ra tiền"), chứ không phải dấu hiệu nạp hụt. Schema khai invoice: Invoice
        // (nullable) đúng theo nghĩa đó.
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
