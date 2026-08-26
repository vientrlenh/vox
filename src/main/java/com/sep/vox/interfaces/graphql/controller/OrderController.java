package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewMySchoolOrdersQuery;
import com.sep.vox.application.port.input.query.ViewOrderDetailsQuery;
import com.sep.vox.application.port.input.query.ViewOrdersQuery;
import com.sep.vox.application.port.input.usecase.order.ViewMySchoolOrdersUseCase;
import com.sep.vox.application.port.input.usecase.order.ViewOrderDetailsUseCase;
import com.sep.vox.application.port.input.usecase.order.ViewOrdersUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.dto.OrderDto;
import com.sep.vox.domain.dto.OrderItemDto;
import com.sep.vox.domain.dto.PaymentDto;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlOrderController")
public class OrderController {

    private final ViewMySchoolOrdersUseCase viewMySchoolOrdersUseCase;
    private final ViewOrdersUseCase viewOrdersUseCase;
    private final ViewOrderDetailsUseCase viewOrderDetailsUseCase;

    public OrderController(
            ViewMySchoolOrdersUseCase viewMySchoolOrdersUseCase,
            ViewOrdersUseCase viewOrdersUseCase, ViewOrderDetailsUseCase viewOrderDetailsUseCase) {
        this.viewMySchoolOrdersUseCase = viewMySchoolOrdersUseCase;
        this.viewOrdersUseCase = viewOrdersUseCase;
        this.viewOrderDetailsUseCase = viewOrderDetailsUseCase;
    }

    @QueryMapping(name = "myOrders")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<OrderDto> myOrders(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePaging(page, size);
        return viewMySchoolOrdersUseCase.execute(new ViewMySchoolOrdersQuery(page, size));
    }

    /**
     * @PreAuthorize ở đây là thứ DUY NHẤT chặn quyền: ViewOrdersUseCase không tự kiểm role, và nó đọc
     * đơn của MỌI trường. Thiếu annotation này thì bất kỳ ai đăng nhập được -- kể cả học sinh -- cũng
     * đọc được toàn bộ lịch sử giao dịch của tất cả các trường.
     */
    @QueryMapping(name = "orders")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<OrderDto> orders(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "status") String status,
            @Argument(name = "type") String type,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePaging(page, size);
        return viewOrdersUseCase.execute(new ViewOrdersQuery(schoolId, status, type, keyword, page, size));
    }

    @QueryMapping(name = "order")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public OrderDto order(@Argument(name = "id") UUID id) {
        var query = new ViewOrderDetailsQuery(id);
        return viewOrderDetailsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "Order", field = "items")
    public CompletableFuture<List<OrderItemDto>> items(OrderDto order, DataFetchingEnvironment env) {
        DataLoader<UUID, List<OrderItemDto>> loader = env.getDataLoader("itemsByOrderId");
        if (loader == null) {
            throw new IllegalStateException("Không tìm thấy data loader itemsByOrderId");
        }
        return loader.load(order.id());
    }

    /**
     * Hóa đơn là TRƯỜNG của đơn chứ không phải một danh sách riêng để hỏi. Nó quan hệ 1-1 với đơn và
     * tự nó chỉ biết đúng hai dữ kiện (số, ngày phát hành) -- tiền, mua gì, trả thế nào đều nằm ở
     * chính đơn này. Hỏi riêng thì client phải tự ghép lại, mà myOrders vốn đã là lịch sử giao dịch
     * với đúng phân trang và phân quyền cần có.
     *
     * <p>Không cần kiểm quyền lại ở đây: muốn tới được trường này thì phải qua myOrders/orders/order,
     * và cả ba đều đã chặn ở @PreAuthorize cùng kiểm chủ sở hữu trong use case.
     */
    @SchemaMapping(typeName = "Order", field = "invoice")
    public CompletableFuture<InvoiceDto> invoice(OrderDto order, DataFetchingEnvironment env) {
        DataLoader<UUID, InvoiceDto> loader = env.getDataLoader("invoiceByOrderId");
        if (loader == null) {
            throw new IllegalStateException("Không tìm thấy data loader invoiceByOrderId");
        }
        return loader.load(order.id());
    }

    @SchemaMapping(typeName = "Order", field = "payments")
    public CompletableFuture<List<PaymentDto>> payments(OrderDto order, DataFetchingEnvironment env) {
        DataLoader<UUID, List<PaymentDto>> loader = env.getDataLoader("paymentsByOrderId");
        if (loader == null) {
            throw new IllegalStateException("Không tìm thấy data loader paymentsByOrderId");
        }
        return loader.load(order.id());
    }

    // page ĐẾM TỪ 1 theo quy ước chung của dự án; OrderRepositoryImpl trừ 1 trước khi xuống
    // PageRequest. Chặn cả 0 để không có hai cách gọi trang đầu (0 và 1) cho ra hai kết quả khác nhau.
    private static void validatePaging(Integer page, Integer size) {
        if (page == null || page < 1) {
            throw new IllegalArgumentException("Số trang phải lớn hơn hoặc bằng 1");
        }
        if (size == null || size <= 0) {
            throw new IllegalArgumentException("Kích thước trang phải lớn hơn 0");
        }
    }
}
