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
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.interfaces.shared.PageArguments;

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
        PageArguments.validate(page, size);
        return viewMySchoolOrdersUseCase.execute(new ViewMySchoolOrdersQuery(page, size));
    }

    /**
     * @PreAuthorize here is the ONLY thing that restricts access: ViewOrdersUseCase does not check the
     * role itself, and it reads the orders of EVERY school. Without this annotation anyone who can log
     * in -- students included -- could read the whole transaction history of all schools.
     */
    @QueryMapping(name = "orders")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<OrderDto> orders(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "status") OrderStatus status,
            @Argument(name = "type") OrderType type,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewOrdersUseCase.execute(new ViewOrdersQuery(schoolId, status, type, keyword, page, size));
    }

    @QueryMapping(name = "order")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public OrderDto order(@Argument(name = "id") UUID id) {
        ViewOrderDetailsQuery query = new ViewOrderDetailsQuery(id);
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
     * The invoice is a FIELD of the order, not a separate list to query. It has a 1-1 relation with the
     * order and by itself knows only two facts (the number and the issue date) -- the money, what was
     * bought and how it was paid all live on the order itself. Asking for it separately would make the
     * client stitch the pieces together, and myOrders is already the transaction history with exactly
     * the pagination and authorization that are needed.
     *
     * <p>No second authorization check is needed here: reaching this field requires going through
     * myOrders/orders/order, and all three already restrict access with @PreAuthorize and check the
     * owner in the use case.
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
}
