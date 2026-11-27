package com.sep.vox.application.usecase.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.sep.vox.application.port.input.command.payment.CreatePaymentCheckoutUrlCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.payment.CreatePaymentCheckoutUrlUseCase;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.payment.PaymentCheckoutResponse;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.application.response.output.BankTransferDetails;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;


/**
 * The ONE invariant this test class exists for: the payment_records row must be COMMITTED before the gateway is
 * called to create the link.
 *
 * <p>The earlier version wrapped the whole use case in a single {@code @Transactional}, so the order "write, then
 * call" was only true in memory: the gateway built the session and then the response was lost, the rollback
 * deleted the row carrying provider_order_ref, the school paid on the checkout page that was still alive, and
 * the callback found no order. Money arrives, nothing is delivered, no trace is left.
 *
 * <p>The SCOPE of the check below, stated precisely: it looks straight at the {@code commit} of the transaction
 * manager, so it catches every way of reordering the phases that puts the gateway call INSIDE an open
 * transaction. It does NOT catch someone putting {@code @Transactional} back on {@code execute}: a unit test
 * calls the object directly so there is no Spring proxy, and that annotation is completely inert here. The guard
 * for that case is still the code reader -- see the Javadoc of the use case itself.
 */
class CreatePaymentCheckoutUrlUseCaseTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private OrderRepository orderRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private PaymentProcessPort paymentProcessPort;
    private RequiresNewTransactionPort requiresNewTransactionPort;
    private Runnable transactionCommitted;
    private final Map<UUID, PaymentRecord> savedById = new HashMap<>();
    private CreatePaymentCheckoutUrlUseCase useCase;

    @BeforeEach
    void setUp() {
        savedById.clear();
        orderRepository = mock(OrderRepository.class);
        paymentRecordRepository = mock(PaymentRecordRepository.class);
        paymentProcessPort = mock(PaymentProcessPort.class);
        requiresNewTransactionPort = mock(RequiresNewTransactionPort.class);
        transactionCommitted = mock(Runnable.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        when(paymentProcessPort.provider()).thenReturn(PaymentProvider.PAYOS);
        // The mock's default is false, and the use case blocks right there -- if it is not
        // declared, every test below would stop at "gateway not configured" before reaching what it
        // means to check.
        when(paymentProcessPort.isConfigured()).thenReturn(true);
        when(paymentProcessPort.newOrderRef()).thenReturn("123456");
        // The port runs the work and then marks the commit, like a REQUIRES_NEW transaction that closes before returning.
        when(requiresNewTransactionPort.callInNewTransaction(any())).thenAnswer(call -> {
            Object result = call.<Supplier<?>>getArgument(0).get();
            transactionCommitted.run();
            return result;
        });
        when(userContextPort.getCurrentSchoolId()).thenReturn(SCHOOL_ID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payableOrder()));
        // Simulates the real adapter's behavior: the id column has DEFAULT uuidv7() and the entity
        // declares @Generated(INSERT) + insertable=false, so the id exists only AFTER saving, and
        // save() returns the copy carrying that id. attachCheckoutUrl reads back by this very id,
        // so without it the test would invent an error that does not exist in real life.
        when(paymentRecordRepository.save(any())).thenAnswer(call -> {
            PaymentRecord saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            savedById.put(saved.getId(), saved);
            return saved;
        });
        when(paymentRecordRepository.findById(any()))
            .thenAnswer(call -> Optional.ofNullable(savedById.get(call.getArgument(0))));

        useCase = new CreatePaymentCheckoutUrlUseCase(
            orderRepository, paymentRecordRepository,
            new PaymentProcessResolver(List.of(paymentProcessPort)),
            userContextPort, new FakeJsonSerializationPort(), requiresNewTransactionPort);
    }

    @Test
    void should_commit_the_pending_attempt_before_calling_the_gateway() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentProcessPort.createPaymentLink(any()))
            .thenReturn(PaymentCheckoutResult.redirect("https://payos.test/checkout", "link-1"));

        useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS));

        InOrder order = inOrder(paymentRecordRepository, transactionCommitted, paymentProcessPort);
        order.verify(paymentRecordRepository).save(any());
        order.verify(transactionCommitted).run();
        order.verify(paymentProcessPort).createPaymentLink(any());
    }

    /**
     * The flip side of "commit first, then call the gateway": if the gateway is not configured the call is bound
     * to fail, and the PENDING row that was just committed stays forever -- cancelling the order has to ask the
     * gateway and so fails too, issuing a new link does the same, and expireIfOverdue refuses to close the order
     * while an attempt is pending. The order is stuck PENDING for good and the school cannot place any other
     * subscription order.
     *
     * <p>The NOT_FOUND branch cannot save this case because it needs the gateway to ANSWER. So the check must come
     * BEFORE the write, and this test locks exactly that: no row is saved.
     */
    @Test
    void should_refuse_before_writing_anything_when_the_gateway_is_not_configured() {
        when(paymentProcessPort.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not configured");

        verify(paymentRecordRepository, never()).save(any());
        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /**
     * When the gateway returns a QR, the code and the transfer information must be STORED, not just
     * pass through the response once and be lost. Scanning the QR means leaving the page for the
     * bank app -- coming back is the normal path, and asking for the status returns only the
     * STATUS, since the gateway does not issue the code again.
     */
    @Test
    void should_persist_the_qr_payload_so_it_can_be_rebuilt_later() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentProcessPort.createPaymentLink(any())).thenReturn(PaymentCheckoutResult.qr(
            "00020101021138...VIETQR",
            new BankTransferDetails("970422", "0912888866", "CONG TY VOX", new BigDecimal("500000"), "VOX 8f3c1a04"),
            "https://payos.test/checkout",
            "link-1"));

        PaymentCheckoutResponse response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS));

        assertThat(response.action()).isEqualTo("QR");
        assertThat(response.qrCode()).isEqualTo("00020101021138...VIETQR");
        assertThat(response.transfer().transferContent()).isEqualTo("VOX 8f3c1a04");

        PaymentRecord stored = savedById.get(response.paymentId());
        assertThat(stored.getCheckoutUrl()).isEqualTo("https://payos.test/checkout");
        assertThat(stored.getProviderPayloadJson()).isEqualTo(
            "{\"qrCode\":\"00020101021138...VIETQR\",\"transfer\":{\"bankBin\":\"970422\","
                + "\"accountNumber\":\"0912888866\",\"accountName\":\"CONG TY VOX\","
                + "\"amountVnd\":500000,\"transferContent\":\"VOX 8f3c1a04\"}}");
    }

    /** A stored payload that cannot be read falls back to the checkout URL path instead of failing. */
    @Test
    void should_fall_back_to_the_checkout_url_when_the_stored_qr_payload_is_malformed() {
        PaymentRecord live = existingAttempt();
        live.setCheckoutUrl("https://payos.test/checkout");
        live.setProviderPayloadJson("{not json");
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(live));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.PENDING));

        PaymentCheckoutResponse response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS));

        assertThat(response.action()).isEqualTo("REDIRECT");
        assertThat(response.qrCode()).isNull();
        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /** And it can be rebuilt from that very payload when the school returns to a pending order. */
    @Test
    void should_rebuild_the_qr_from_the_stored_payload_on_a_live_attempt() {
        PaymentRecord live = existingAttempt();
        live.setCheckoutUrl("https://payos.test/checkout");
        live.setProviderPayloadJson(
            "{\"qrCode\":\"00020101021138...VIETQR\",\"transfer\":{\"bankBin\":\"970422\","
                + "\"accountNumber\":\"0912888866\",\"accountName\":\"CONG TY VOX\","
                + "\"amountVnd\":500000,\"transferContent\":\"VOX 8f3c1a04\"}}");
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(live));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.PENDING));

        PaymentCheckoutResponse response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS));

        assertThat(response.action()).isEqualTo("QR");
        assertThat(response.qrCode()).isEqualTo("00020101021138...VIETQR");
        assertThat(response.transfer().accountNumber()).isEqualTo("0912888866");
        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /**
     * A direct consequence of committing first: a create-link call that fails BEFORE the gateway
     * has built the session leaves a PENDING row carrying a code the gateway has never seen.
     * Without the NOT_FOUND branch, that attempt hangs until the order expires and the school
     * cannot get a new link.
     */
    @Test
    void should_retire_an_attempt_the_gateway_has_never_heard_of() {
        PaymentRecord orphan = existingAttempt();
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(orphan));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.NOT_FOUND));
        when(paymentProcessPort.createPaymentLink(any()))
            .thenReturn(PaymentCheckoutResult.redirect("https://payos.test/checkout", "link-2"));

        PaymentCheckoutResponse response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS));

        assertThat(orphan.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.providerOrderRef()).isEqualTo("123456");
        verify(paymentProcessPort).createPaymentLink(any());
    }

    /**
     * The other side of NOT_FOUND, and the more dangerous case: for an attempt opened only a few
     * seconds ago, NOT_FOUND does NOT mean it is dead. Quite possibly another tab has just
     * committed the PENDING row and is waiting for createPaymentLink to return -- the gateway has
     * not built the session yet, so returning NOT_FOUND is entirely correct.
     *
     * <p>Marking it failed now creates the worst case: the other tab receives a real link, the
     * school pays into it, but the row for that payment is already FAILED so the callback delivers
     * nothing (PAID_AFTER_WRITE_OFF). Better to make them wait a few minutes.
     */
    @Test
    void should_not_believe_not_found_for_an_attempt_that_was_just_opened() {
        PaymentRecord justOpened = existingAttempt(Instant.now().minus(3, ChronoUnit.SECONDS));
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(justOpened));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.NOT_FOUND));

        assertThatThrownBy(() -> useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("try again in a few minutes");

        assertThat(justOpened.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /**
     * The gateway says the money was collected for the pending attempt: NO further link may be issued. Issuing
     * another would put two sessions alive on one order, i.e. invite the school to pay a second time.
     */
    @Test
    void should_refuse_to_issue_a_second_link_when_the_gateway_says_the_attempt_is_paid() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(existingAttempt()));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.PAID));

        assertThatThrownBy(() -> useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already been paid");

        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /**
     * If the old session is still alive, return EXACTLY that link, and do not generate a new code.
     */
    @Test
    void should_reuse_a_live_attempt_instead_of_opening_a_new_one() {
        PaymentRecord live = existingAttempt();
        live.setCheckoutUrl("https://payos.test/still-alive");
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(live));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.PENDING));

        PaymentCheckoutResponse response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS));

        assertThat(response.checkoutUrl()).isEqualTo("https://payos.test/still-alive");
        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /**
     * The REDIRECT branch must STORE the link, otherwise the test above is only right on paper: the
     * entity once declared checkout_url as updatable = false, so the UPDATE in attachCheckoutUrl
     * silently skipped that column -- no error, no warning, the column stayed NULL forever. As a
     * result every later return to pay fell into "a payment session is already pending" instead of
     * getting back its own link.
     */
    @Test
    void should_persist_the_checkout_url_for_a_redirect_gateway() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentProcessPort.createPaymentLink(any()))
            .thenReturn(PaymentCheckoutResult.redirect("https://payos.test/checkout", "link-1"));

        PaymentCheckoutResponse response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS));

        assertThat(savedById.get(response.paymentId()).getCheckoutUrl())
            .isEqualTo("https://payos.test/checkout");
    }

    /**
     * Conversely, a FORM_POST gateway is NOT stored: its actionUrl is a fixed endpoint shared by
     * every order, while what identifies the session lives in the signed field set (deliberately
     * not stored). Storing only the URL and redirecting straight to it next time would send the
     * school to a blank init page.
     *
     * <p>That way checkout_url IS NULL keeps exactly one meaning -- "this attempt cannot be
     * rebuilt" -- and that is the condition resolvePendingAttempt is checking.
     */
    @Test
    void should_not_persist_a_form_post_action_url() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentProcessPort.createPaymentLink(any()))
            .thenReturn(PaymentCheckoutResult.formPost(
                "https://sepay.test/v1/checkout/init", Map.of("merchant", "vox", "signature", "abc")));

        PaymentCheckoutResponse response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, PaymentProvider.PAYOS));

        assertThat(savedById.get(response.paymentId()).getCheckoutUrl()).isNull();
    }

    private static Order payableOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setSchoolId(SCHOOL_ID);
        order.setType(OrderType.TOPUP);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmountVnd(new BigDecimal("500000"));
        order.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
        return order;
    }

    private static PaymentRecord existingAttempt() {
        // Default is an attempt that is PAST the grace period: most of the checks below are about steady-state
        // behaviour, not about the few-minute window right after an attempt is opened.
        return existingAttempt(Instant.now().minus(10, ChronoUnit.MINUTES));
    }

    private static PaymentRecord existingAttempt(Instant createdAt) {
        PaymentRecord attempt = PaymentRecord.forEBankingCheckout(
            ORDER_ID, new BigDecimal("500000"), PaymentProvider.PAYOS, "orphan-ref", createdAt);
        attempt.setId(UUID.randomUUID());
        return attempt;
    }
}
