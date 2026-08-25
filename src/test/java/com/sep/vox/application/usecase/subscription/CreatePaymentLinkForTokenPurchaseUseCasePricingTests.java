package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.BuyTokensCommand;
import com.sep.vox.application.port.input.command.TokenPurchaseItemInput;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.subscription.CreatePaymentLinkForTokenPurchaseUseCase;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.domain.model.invoice.Invoice;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.model.subscription.TokenPurchase;
import com.sep.vox.domain.model.subscription.TokenPurchaseItem;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

/**
 * Đối xứng với BuyTokensUseCasePricingTests, nhưng cho đường online (PayOS/SePay): giá tạo payment
 * link phải theo tỷ giá SỐNG (QuotaPricingService.tokenUnitPriceFor), không dùng
 * planQuota.getTokenUnitPrice() đã đóng băng lúc gói còn DRAFT.
 */
class CreatePaymentLinkForTokenPurchaseUseCasePricingTests {

    private static final BigDecimal FROZEN_PRICE_AT_PLAN_CREATION = new BigDecimal("26000");
    private static final BigDecimal LIVE_PRICE_TODAY = new BigDecimal("31200");

    private QuotaPricingPort quotaPricingService;
    private TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private InvoiceRepository invoiceRepository;
    private CreatePaymentLinkForTokenPurchaseUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        var subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        var planQuotaRepository = mock(SubscriptionPlanQuotaRepository.class);
        quotaPricingService = mock(QuotaPricingPort.class);
        var tokenPurchaseRepository = mock(TokenPurchaseRepository.class);
        tokenPurchaseItemRepository = mock(TokenPurchaseItemRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        var paymentProcessResolver = mock(PaymentProcessResolver.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new CreatePaymentLinkForTokenPurchaseUseCase(
            schoolSubscriptionRepository,
            subscriptionPlanRepository,
            planQuotaRepository,
            quotaPricingService,
            tokenPurchaseRepository,
            tokenPurchaseItemRepository,
            invoiceRepository,
            paymentProcessResolver,
            userContextPort
        );

        when(userContextPort.isSystemAdmin()).thenReturn(true);

        var subscription = new SchoolSubscription(
            subscriptionId, schoolId, planId, null, null,
            SubscriptionStatus.ACTIVE, new BigDecimal("5000000"), null, Instant.now(), 0L, null, null, null
        );
        var plan = new SubscriptionPlan(
            planId, "Gói Trường", null, new BigDecimal("5000000"), 365, 60, PlanStatus.ACTIVE, 1,
            Instant.now(), null, null, new BigDecimal("0.20")
        );
        when(schoolSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        // Giá đóng băng trong subscription_plan_quotas lúc tạo gói -- KHÔNG được dùng để tính tiền mua thêm nữa.
        when(planQuotaRepository.findAllByPlanId(planId)).thenReturn(List.of(
            new SubscriptionPlanQuota(planId, QuotaType.GRADING, BigDecimal.valueOf(100), FROZEN_PRICE_AT_PLAN_CREATION)
        ));
        // Giá sống hôm nay theo tỷ giá hiện tại, khác hẳn giá đóng băng để phân biệt rõ trong assertion.
        when(quotaPricingService.tokenUnitPriceFor(eq(new BigDecimal("0.20")))).thenReturn(LIVE_PRICE_TODAY);
        when(tokenPurchaseRepository.save(any(TokenPurchase.class))).thenAnswer(call -> {
            TokenPurchase purchase = call.getArgument(0);
            if (purchase.getId() == null) {
                purchase.setId(UUID.randomUUID());
            }
            return purchase;
        });
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> {
            Invoice invoice = call.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });

        var paymentProcessPort = mock(PaymentProcessPort.class);
        when(paymentProcessResolver.resolve(any())).thenReturn(paymentProcessPort);
        when(paymentProcessPort.newOrderRef(any())).thenReturn("ORDER-REF");
        when(paymentProcessPort.createPaymentLink(any()))
            .thenReturn(PaymentCheckoutResult.redirect("https://pay.example/checkout", "LINK-1"));
    }

    @Test
    void pricesPurchaseUsingLiveExchangeRateNotTheFrozenPlanQuotaPrice() {
        var quantity = BigDecimal.valueOf(10);
        var command = new BuyTokensCommand(
            schoolId, subscriptionId, List.of(new TokenPurchaseItemInput(QuotaType.GRADING, quantity)), "PAYOS"
        );

        useCase.execute(command);

        var expectedTotal = LIVE_PRICE_TODAY.multiply(quantity);
        var invoiceCaptor = org.mockito.ArgumentCaptor.forClass(Invoice.class);
        org.mockito.Mockito.verify(invoiceRepository, org.mockito.Mockito.atLeastOnce()).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(invoiceCaptor.getValue().getAmount())
            .isNotEqualByComparingTo(FROZEN_PRICE_AT_PLAN_CREATION.multiply(quantity));

        var itemCaptor = org.mockito.ArgumentCaptor.forClass(TokenPurchaseItem.class);
        org.mockito.Mockito.verify(tokenPurchaseItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getUnitPriceSnapshot()).isEqualByComparingTo(LIVE_PRICE_TODAY);
    }
}
