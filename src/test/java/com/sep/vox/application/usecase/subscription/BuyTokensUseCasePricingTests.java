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
import com.sep.vox.application.port.input.service.SchoolDebtNotificationService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.subscription.BuyTokensUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.model.subscription.TokenPurchase;
import com.sep.vox.domain.model.subscription.TokenPurchaseItem;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

/**
 * Xác nhận mua thêm quota tính tiền theo tỷ giá USD->VND SỐNG (QuotaPricingService.tokenUnitPriceFor,
 * đọc snapshot mới nhất) chứ không dùng planQuota.getTokenUnitPrice() đã đóng băng lúc gói còn DRAFT.
 */
class BuyTokensUseCasePricingTests {

    private static final BigDecimal FROZEN_PRICE_AT_PLAN_CREATION = new BigDecimal("26000");
    private static final BigDecimal LIVE_PRICE_TODAY = new BigDecimal("31200");

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private SubscriptionPlanQuotaRepository planQuotaRepository;
    private QuotaPricingPort quotaPricingService;
    private SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private TokenPurchaseRepository tokenPurchaseRepository;
    private TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private BuyTokensUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        planQuotaRepository = mock(SubscriptionPlanQuotaRepository.class);
        quotaPricingService = mock(QuotaPricingPort.class);
        subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        tokenPurchaseRepository = mock(TokenPurchaseRepository.class);
        tokenPurchaseItemRepository = mock(TokenPurchaseItemRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new BuyTokensUseCase(
            schoolSubscriptionRepository,
            subscriptionPlanRepository,
            planQuotaRepository,
            quotaPricingService,
            subscriptionQuotaRepository,
            tokenPurchaseRepository,
            tokenPurchaseItemRepository,
            mock(InvoiceRepository.class),
            mock(FinancialEventRepository.class),
            userContextPort,
            mock(SchoolSubscriptionDebtGuardService.class),
            mock(SchoolDebtNotificationService.class)
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
        when(subscriptionQuotaRepository.findAllBySubscriptionId(subscriptionId)).thenReturn(List.of(
            new SchoolSubscriptionQuotaRecord(UUID.randomUUID(), subscriptionId, QuotaType.GRADING, BigDecimal.valueOf(100), BigDecimal.ZERO)
        ));
        when(tokenPurchaseRepository.save(any(TokenPurchase.class))).thenAnswer(call -> {
            TokenPurchase purchase = call.getArgument(0);
            if (purchase.getId() == null) {
                purchase.setId(UUID.randomUUID());
            }
            return purchase;
        });
        when(tokenPurchaseItemRepository.findAllByPurchaseId(any())).thenReturn(List.of());
    }

    @Test
    void pricesPurchaseUsingLiveExchangeRateNotTheFrozenPlanQuotaPrice() {
        var quantity = BigDecimal.valueOf(10);
        var command = new BuyTokensCommand(
            schoolId, subscriptionId, List.of(new TokenPurchaseItemInput(QuotaType.GRADING, quantity)), "MANUAL"
        );

        var result = useCase.execute(command);

        var expectedTotal = LIVE_PRICE_TODAY.multiply(quantity);
        assertThat(result.totalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(result.totalAmount()).isNotEqualByComparingTo(FROZEN_PRICE_AT_PLAN_CREATION.multiply(quantity));

        var itemCaptor = org.mockito.ArgumentCaptor.forClass(TokenPurchaseItem.class);
        org.mockito.Mockito.verify(tokenPurchaseItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getUnitPriceSnapshot()).isEqualByComparingTo(LIVE_PRICE_TODAY);
    }
}
