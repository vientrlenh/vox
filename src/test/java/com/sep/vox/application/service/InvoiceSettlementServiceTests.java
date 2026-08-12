package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.InvoicePaidEvent;
import com.sep.vox.application.port.input.service.InvoiceSettlementService;
import com.sep.vox.application.port.input.service.SubscriptionPlanResolver;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.PurchaseStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.RequestType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.model.subscription.TokenPurchase;
import com.sep.vox.domain.model.subscription.TokenPurchaseItem;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

/**
 * Điểm chốt tiền của hệ thống: callback của mọi cổng thanh toán, luồng sync theo yêu cầu FE
 * và job đối soát định kỳ đều đổ về đây, và cả ba đều có thể chạy đồng thời trên cùng một
 * hóa đơn. Hai thứ phải giữ bằng mọi giá là <em>chốt đúng một lần</em> và <em>không cấp
 * quota khi chưa thu được tiền</em>.
 */
class InvoiceSettlementServiceTests {

    private InvoiceRepository invoiceRepository;
    private SubscriptionRequestRepository subscriptionRequestRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private PlanQuotaRepository planQuotaRepository;
    private SubscriptionQuotaRepository subscriptionQuotaRepository;
    private TokenPurchaseRepository tokenPurchaseRepository;
    private TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private FinancialEventRepository financialEventRepository;
    private EventPublisherPort eventPublisherPort;
    private SubscriptionPlanResolver subscriptionPlanResolver;
    private InvoiceSettlementService service;

    private final UUID invoiceId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID sourceId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("5000000");

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        subscriptionRequestRepository = mock(SubscriptionRequestRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        planQuotaRepository = mock(PlanQuotaRepository.class);
        subscriptionQuotaRepository = mock(SubscriptionQuotaRepository.class);
        tokenPurchaseRepository = mock(TokenPurchaseRepository.class);
        tokenPurchaseItemRepository = mock(TokenPurchaseItemRepository.class);
        financialEventRepository = mock(FinancialEventRepository.class);
        subscriptionPlanResolver = mock(SubscriptionPlanResolver.class);
        eventPublisherPort = mock(EventPublisherPort.class);

        service = new InvoiceSettlementService(
            invoiceRepository,
            subscriptionRequestRepository,
            schoolSubscriptionRepository,
            subscriptionPlanRepository,
            planQuotaRepository,
            subscriptionQuotaRepository,
            tokenPurchaseRepository,
            tokenPurchaseItemRepository,
            financialEventRepository, 
            subscriptionPlanResolver, 
            eventPublisherPort
        );

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));
    }

    private Invoice invoice(InvoiceSourceType sourceType, InvoiceStatus status) {
        return invoice(sourceType, status, PaymentMethod.PAYOS);
    }

    private Invoice invoice(InvoiceSourceType sourceType, InvoiceStatus status, PaymentMethod paymentProvider) {
        return new Invoice(
            invoiceId, "INV-2026-ABCD1234", schoolId, subscriptionId, sourceType, sourceId,
            LocalDate.of(2026, 8, 5), amount, status, paymentProvider, "1754380800000", "link-id",
            "https://pay.test/checkout", null, null
        );
    }

    private void lockedInvoiceIs(Invoice locked) {
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(locked));
    }

    private void givenPendingSubscriptionRequest() {
        var request = new SubscriptionRequest(
            sourceId, schoolId, RequestType.REGISTRATION, null, planId,
            amount, RequestStatus.PENDING, Instant.now(), null, null
        );
        var plan = new SubscriptionPlan(
            planId, "Gói Trường", null, amount, 365,
            60, 500, false, PlanStatus.ACTIVE, 1, Instant.now(), null, null
        );

        when(subscriptionRequestRepository.findById(sourceId)).thenReturn(Optional.of(request));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());
        when(planQuotaRepository.findAllByPlanId(planId)).thenReturn(List.of(
            new PlanQuota(planId, QuotaType.GRADING, BigDecimal.valueOf(100), new BigDecimal("1000"))
        ));
        when(schoolSubscriptionRepository.save(any(SchoolSubscription.class))).thenAnswer(call -> {
            SchoolSubscription saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(subscriptionId);
            }
            return saved;
        });
    }

    /** Trả về id của SubscriptionQuota sẽ nhận thêm token. */
    private UUID givenPendingTokenPurchase() {
        var quotaId = UUID.randomUUID();
        when(tokenPurchaseRepository.findById(sourceId)).thenReturn(Optional.of(
            new TokenPurchase(sourceId, subscriptionId, amount, PurchaseStatus.PENDING, Instant.now())
        ));
        when(tokenPurchaseItemRepository.findAllByPurchaseId(any())).thenReturn(List.of(
            new TokenPurchaseItem(sourceId, QuotaType.GRADING, BigDecimal.valueOf(50), new BigDecimal("1000"), amount)
        ));
        when(subscriptionQuotaRepository.findAllBySubscriptionId(subscriptionId)).thenReturn(List.of(
            new SubscriptionQuota(quotaId, subscriptionId, QuotaType.GRADING, BigDecimal.valueOf(100), BigDecimal.valueOf(10))
        ));
        when(schoolSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(
            new SchoolSubscription(subscriptionId, schoolId, planId, LocalDate.now(), LocalDate.now().plusDays(365),
                SubscriptionStatus.ACTIVE, amount, null, Instant.now(), 0L)
        ));
        return quotaId;
    }

    @Test
    void marksInvoicePaidAndActivatesSubscriptionOnSuccess() {
        var pending = invoice(InvoiceSourceType.SUBSCRIPTION_REQUEST, InvoiceStatus.PENDING);
        lockedInvoiceIs(pending);
        givenPendingSubscriptionRequest();

        service.settle(pending, true);

        assertThat(pending.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(pending.getPaidAt()).isNotNull();

        var subscriptionCaptor = ArgumentCaptor.forClass(SchoolSubscription.class);
        verify(schoolSubscriptionRepository).save(subscriptionCaptor.capture());
        assertThat(subscriptionCaptor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscriptionCaptor.getValue().getPlanId()).isEqualTo(planId);

        var requestCaptor = ArgumentCaptor.forClass(SubscriptionRequest.class);
        verify(subscriptionRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(RequestStatus.APPROVED);

        var quotaCaptor = ArgumentCaptor.forClass(SubscriptionQuota.class);
        verify(subscriptionQuotaRepository).save(quotaCaptor.capture());
        assertThat(quotaCaptor.getValue().getQuotaType()).isEqualTo(QuotaType.GRADING);
        assertThat(quotaCaptor.getValue().getTotalAllocated()).isEqualByComparingTo(BigDecimal.valueOf(100));

        var eventCaptor = ArgumentCaptor.forClass(FinancialEvent.class);
        verify(financialEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(FinancialEventType.SUB_PURCHASED);
        assertThat(eventCaptor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.PAYOS);

        verify(eventPublisherPort).publish(any(InvoicePaidEvent.class));
    }

    /**
     * financial_event là nguồn để đối soát ngược với dashboard của từng cổng, nên nó phải ghi đúng
     * cổng đã thu tiền cho hóa đơn này. Trước đây chỗ này hardcode PAYOS, nghĩa là mọi giao dịch
     * SePay sẽ nằm lẫn trong nhóm PayOS và không tra ra được.
     */
    @Test
    void recordsTheGatewayThatActuallyCollectedTheMoney() {
        var pending = invoice(InvoiceSourceType.SUBSCRIPTION_REQUEST, InvoiceStatus.PENDING, PaymentMethod.SEPAY);
        lockedInvoiceIs(pending);
        givenPendingSubscriptionRequest();

        service.settle(pending, true);

        var eventCaptor = ArgumentCaptor.forClass(FinancialEvent.class);
        verify(financialEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.SEPAY);
    }

    @Test
    void recordsTheGatewayForTokenPurchaseToo() {
        var pending = invoice(InvoiceSourceType.TOKEN_PURCHASE, InvoiceStatus.PENDING, PaymentMethod.SEPAY);
        lockedInvoiceIs(pending);
        givenPendingTokenPurchase();

        service.settle(pending, true);

        var eventCaptor = ArgumentCaptor.forClass(FinancialEvent.class);
        verify(financialEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.SEPAY);
    }

    /**
     * Hợp đồng của {@code SELECT ... FOR UPDATE}: trạng thái được đọc lại dưới khóa, không tin
     * vào object mà caller truyền vào. Webhook và sync-status có thể cùng đọc thấy PENDING trước
     * khi bên kia commit — nếu tin object của caller thì cả hai đều chốt.
     */
    @Test
    void ignoresStalePendingSnapshotWhenInvoiceAlreadySettled() {
        var stalePendingSnapshot = invoice(InvoiceSourceType.SUBSCRIPTION_REQUEST, InvoiceStatus.PENDING);
        lockedInvoiceIs(invoice(InvoiceSourceType.SUBSCRIPTION_REQUEST, InvoiceStatus.PAID));

        service.settle(stalePendingSnapshot, true);

        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(schoolSubscriptionRepository, never()).save(any(SchoolSubscription.class));
        verify(subscriptionRequestRepository, never()).save(any(SubscriptionRequest.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void settlingTwiceActivatesSubscriptionOnlyOnce() {
        var pending = invoice(InvoiceSourceType.SUBSCRIPTION_REQUEST, InvoiceStatus.PENDING);
        lockedInvoiceIs(pending);
        givenPendingSubscriptionRequest();

        service.settle(pending, true);
        service.settle(pending, true); // lần 2 đọc lại thấy PAID nên phải bỏ qua

        verify(schoolSubscriptionRepository).save(any(SchoolSubscription.class));
        verify(financialEventRepository).save(any(FinancialEvent.class));
        verify(eventPublisherPort).publish(any(InvoicePaidEvent.class));
    }

    @Test
    void marksFailedWithoutGrantingAnythingOnFailure() {
        var pending = invoice(InvoiceSourceType.SUBSCRIPTION_REQUEST, InvoiceStatus.PENDING);
        lockedInvoiceIs(pending);

        service.settle(pending, false);

        assertThat(pending.getStatus()).isEqualTo(InvoiceStatus.FAILED);
        assertThat(pending.getPaidAt()).isNull();
        verify(invoiceRepository).save(pending);
        verify(schoolSubscriptionRepository, never()).save(any(SchoolSubscription.class));
        verify(subscriptionQuotaRepository, never()).save(any(SubscriptionQuota.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void recordsExplicitFailureStatusWhenReasonIsKnown() {
        var pending = invoice(InvoiceSourceType.SUBSCRIPTION_REQUEST, InvoiceStatus.PENDING);
        lockedInvoiceIs(pending);

        service.settle(pending, false, InvoiceStatus.CANCELLED);

        assertThat(pending.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(pending.getPaidAt()).isNotNull();
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void addsPurchasedTokensToSubscriptionQuotaOnSuccess() {
        var pending = invoice(InvoiceSourceType.TOKEN_PURCHASE, InvoiceStatus.PENDING);
        lockedInvoiceIs(pending);

        var quotaId = givenPendingTokenPurchase();

        service.settle(pending, true);

        assertThat(pending.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(subscriptionQuotaRepository).addAllocation(eq(quotaId), eq(BigDecimal.valueOf(50)));

        var purchaseCaptor = ArgumentCaptor.forClass(TokenPurchase.class);
        verify(tokenPurchaseRepository).save(purchaseCaptor.capture());
        assertThat(purchaseCaptor.getValue().getStatus()).isEqualTo(PurchaseStatus.PAID);

        var eventCaptor = ArgumentCaptor.forClass(FinancialEvent.class);
        verify(financialEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(FinancialEventType.TOKEN_PURCHASED);
    }
}
