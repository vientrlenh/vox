package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.query.order.ViewOrdersQuery;
import com.sep.vox.application.port.input.usecase.order.ViewMySchoolOrdersUseCase;
import com.sep.vox.application.port.input.usecase.order.ViewOrderDetailsUseCase;
import com.sep.vox.application.port.input.usecase.order.ViewOrdersUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.OrderDto;
import com.sep.vox.domain.model.invoice.Invoice;
import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.model.order.OrderItemType;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.payment.PaymentMethod;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.OrderItemRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;
import com.sep.vox.interfaces.graphql.dataloader.InvoiceGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.OrderGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.PaymentGraphQLDataLoaderConfig;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends order queries through the real schema and asserts the typed values: the {@code OrderType},
 * {@code OrderStatus}, {@code OrderItemType}, {@code PaymentStatus}, {@code PaymentMethod} and
 * {@code PaymentProvider} enums and the {@code DateTime} scalar, on the order and on the invoice and
 * payments nested under it.
 */
@GraphQLControllerTest(GraphQLOrderController.class)
@Import({ OrderGraphQLDataLoaderConfig.class, InvoiceGraphQLDataLoaderConfig.class, PaymentGraphQLDataLoaderConfig.class })
class OrderGraphQLTests {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-07-28T02:30:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewMySchoolOrdersUseCase viewMySchoolOrdersUseCase;
    @MockitoBean
    private ViewOrdersUseCase viewOrdersUseCase;
    @MockitoBean
    private ViewOrderDetailsUseCase viewOrderDetailsUseCase;
    @MockitoBean
    private OrderItemRepository orderItemRepository;
    @MockitoBean
    private InvoiceRepository invoiceRepository;
    @MockitoBean
    private PaymentRecordRepository paymentRecordRepository;

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void order_should_return_enums_and_date_time_values() {
        when(viewOrderDetailsUseCase.execute(any())).thenReturn(order());

        graphQlTester.document("{ order(id: \"%s\") { type status createdAt updatedAt expiresAt } }".formatted(ORDER_ID))
            .execute()
            .errors().verify()
            .path("order.type").entity(String.class).isEqualTo("SUBSCRIPTION_UPGRADE")
            .path("order.status").entity(String.class).isEqualTo("PENDING")
            .path("order.createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("order.updatedAt").valueIsNull()
            .path("order.expiresAt").entity(String.class).isEqualTo("2026-07-28T03:00:00Z");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void order_should_return_item_type_invoice_and_payments_with_typed_values() {
        when(viewOrderDetailsUseCase.execute(any())).thenReturn(order());
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(List.of(
            OrderItem.builder()
                .orderId(ORDER_ID)
                .type(OrderItemType.SUBSCRIPTION)
                .itemId(UUID.randomUUID())
                .unitPriceVnd(BigDecimal.TEN)
                .amountVnd(BigDecimal.TEN)
                .quantity(1)
                .build()));
        when(invoiceRepository.findByOrderIdIn(any())).thenReturn(List.of(
            Invoice.builder()
                .orderId(ORDER_ID)
                .paymentId(UUID.randomUUID())
                .invoiceNumber("INV-2026-0123456789AB")
                .issueDate(CREATED_AT)
                .build()));
        when(paymentRecordRepository.findByOrderIdIn(any())).thenReturn(List.of(
            payment(PaymentStatus.PAID, null), payment(PaymentStatus.PENDING, "https://pay.example/checkout")));

        graphQlTester.document("""
                { order(id: "%s") {
                    items { type }
                    invoice { invoiceNumber issueDate }
                    payments { method provider status checkoutUrl paidAt createdAt }
                } }""".formatted(ORDER_ID))
            .execute()
            .errors().verify()
            .path("order.items[0].type").entity(String.class).isEqualTo("SUBSCRIPTION")
            .path("order.invoice.issueDate").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("order.payments[0].method").entity(String.class).isEqualTo("E_BANKING")
            .path("order.payments[0].provider").entity(String.class).isEqualTo("PAYOS")
            .path("order.payments[0].status").entity(String.class).isEqualTo("PAID")
            .path("order.payments[0].checkoutUrl").valueIsNull()
            .path("order.payments[0].paidAt").entity(String.class).isEqualTo("2026-07-28T02:35:00Z")
            .path("order.payments[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("order.payments[1].status").entity(String.class).isEqualTo("PENDING")
            .path("order.payments[1].checkoutUrl").entity(String.class).isEqualTo("https://pay.example/checkout")
            .path("order.payments[1].paidAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void orders_should_bind_enum_variables_into_the_query() {
        when(viewOrdersUseCase.execute(any())).thenReturn(new PageResult<>(List.of(order()), 1, 20, 1L, 1));

        graphQlTester.document("""
                query($status: OrderStatus, $type: OrderType) {
                    orders(status: $status, type: $type) { content { type status } }
                }""")
            .variable("status", "PENDING")
            .variable("type", "SUBSCRIPTION_UPGRADE")
            .execute()
            .errors().verify()
            .path("orders.content[0].type").entity(String.class).isEqualTo("SUBSCRIPTION_UPGRADE")
            .path("orders.content[0].status").entity(String.class).isEqualTo("PENDING");

        verify(viewOrdersUseCase).execute(
            new ViewOrdersQuery(null, OrderStatus.PENDING, OrderType.SUBSCRIPTION_UPGRADE, null, 1, 20));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void orders_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ orders(status: PAID) { totalElements } }")
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("PAID"))
            .verify();

        verifyNoInteractions(viewOrdersUseCase);
    }

    private static OrderDto order() {
        return new OrderDto(ORDER_ID, UUID.randomUUID(), OrderType.SUBSCRIPTION_UPGRADE, "Upgrade", BigDecimal.TEN,
            BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, OrderStatus.PENDING, null, CREATED_AT, null,
            Instant.parse("2026-07-28T03:00:00Z"));
    }

    private static PaymentRecord payment(PaymentStatus status, String checkoutUrl) {
        Instant paidAt = status == PaymentStatus.PAID ? Instant.parse("2026-07-28T02:35:00Z") : null;
        return PaymentRecord.builder()
            .id(UUID.randomUUID())
            .orderId(ORDER_ID)
            .amountVnd(BigDecimal.TEN)
            .method(PaymentMethod.E_BANKING)
            .provider(PaymentProvider.PAYOS)
            .status(status)
            .providerOrderRef("ref-1")
            .checkoutUrl(checkoutUrl)
            .providerPayloadJson(null)
            .paidAt(paidAt)
            .createdAt(CREATED_AT)
            .build();
    }
}
