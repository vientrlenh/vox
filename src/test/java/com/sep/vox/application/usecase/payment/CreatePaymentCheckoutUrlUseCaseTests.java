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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.sep.vox.application.port.input.command.CreatePaymentCheckoutUrlCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.payment.CreatePaymentCheckoutUrlUseCase;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.port.output.UserContextPort;
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

import tools.jackson.databind.json.JsonMapper;

/**
 * Bất biến DUY NHẤT mà lớp test này tồn tại vì nó: dòng payment_records phải được COMMIT trước khi
 * gọi sang cổng tạo link.
 *
 * <p>Bản trước bọc cả use case trong một {@code @Transactional}, nên thứ tự "ghi rồi mới gọi" chỉ
 * đúng trong bộ nhớ: cổng dựng phiên xong mà phản hồi rơi là rollback xóa luôn dòng mang
 * provider_order_ref, trường trả tiền vào trang checkout vẫn sống và callback không tra ra đơn nào.
 * Tiền về, không giao gì, không còn dấu vết.
 *
 * <p>PHẠM VI của phép kiểm dưới đây, nói cho đúng: nó soi thẳng vào lần {@code commit} của
 * transaction manager, nên nó bắt được mọi cách sắp xếp lại các giai đoạn khiến lời gọi cổng lọt vào
 * TRONG một transaction đang mở. Nó KHÔNG bắt được việc ai đó gắn lại {@code @Transactional} lên
 * {@code execute}: unit test gọi thẳng đối tượng nên không có proxy nào của Spring, annotation đó
 * hoàn toàn trơ ở đây. Chốt chặn cho ca ấy vẫn là người đọc code -- xem javadoc của chính use case.
 */
class CreatePaymentCheckoutUrlUseCaseTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private OrderRepository orderRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private PaymentProcessPort paymentProcessPort;
    private PlatformTransactionManager transactionManager;
    private final Map<UUID, PaymentRecord> savedById = new HashMap<>();
    private CreatePaymentCheckoutUrlUseCase useCase;

    @BeforeEach
    void setUp() {
        savedById.clear();
        orderRepository = mock(OrderRepository.class);
        paymentRecordRepository = mock(PaymentRecordRepository.class);
        paymentProcessPort = mock(PaymentProcessPort.class);
        transactionManager = mock(PlatformTransactionManager.class);
        var userContextPort = mock(UserContextPort.class);

        when(paymentProcessPort.provider()).thenReturn(PaymentProvider.PAYOS);
        // Mặc định của mock là false, mà use case chặn ngay ở đó -- không khai thì mọi test dưới đây
        // đều dừng ở "cổng chưa cấu hình" trước khi chạm tới thứ nó định kiểm.
        when(paymentProcessPort.isConfigured()).thenReturn(true);
        when(paymentProcessPort.newOrderRef()).thenReturn("123456");
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(userContextPort.getCurrentSchoolId()).thenReturn(SCHOOL_ID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payableOrder()));
        // Giả lập đúng hành vi của adapter thật: cột id có DEFAULT uuidv7() và entity khai
        // @Generated(INSERT) + insertable=false, nên id chỉ có SAU khi lưu, và save() trả về bản đã
        // mang id đó. attachCheckoutUrl đọc lại theo chính id này nên thiếu nó là test tự bịa ra một
        // lỗi không tồn tại ngoài đời.
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
            userContextPort, JsonMapper.builder().build(), transactionManager);
    }

    @Test
    void should_commit_the_pending_attempt_before_calling_the_gateway() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentProcessPort.createPaymentLink(any()))
            .thenReturn(PaymentCheckoutResult.redirect("https://payos.test/checkout", "link-1"));

        useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS"));

        var order = inOrder(paymentRecordRepository, transactionManager, paymentProcessPort);
        order.verify(paymentRecordRepository).save(any());
        order.verify(transactionManager).commit(any());
        order.verify(paymentProcessPort).createPaymentLink(any());
    }

    /**
     * Mặt trái của "commit trước rồi mới gọi cổng": nếu cổng thiếu cấu hình thì lời gọi chắc chắn
     * chết, và dòng PENDING vừa commit ở lại vĩnh viễn -- hủy đơn phải hỏi cổng nên hỏng theo, phát
     * link mới cũng vậy, còn expireIfOverdue từ chối đóng đơn khi còn lần thử treo. Đơn kẹt PENDING
     * mãi mãi và trường không đặt được đơn đăng ký nào khác.
     *
     * <p>Nhánh NOT_FOUND không cứu được ca này vì nó cần cổng TRẢ LỜI được. Nên phải chặn TRƯỚC khi
     * ghi, và test này khoá đúng điều đó: không có dòng nào được lưu.
     */
    @Test
    void should_refuse_before_writing_anything_when_the_gateway_is_not_configured() {
        when(paymentProcessPort.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa được cấu hình");

        verify(paymentRecordRepository, never()).save(any());
        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /**
     * Cổng trả về QR thì mã và thông tin chuyển khoản phải được LƯU, không chỉ đi qua response một
     * lần rồi mất. Quét QR nghĩa là rời trang sang app ngân hàng -- quay lại là đường đi bình thường,
     * mà hỏi trạng thái chỉ trả về TRẠNG THÁI chứ cổng không phát lại mã.
     */
    @Test
    void should_persist_the_qr_payload_so_it_can_be_rebuilt_later() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentProcessPort.createPaymentLink(any())).thenReturn(PaymentCheckoutResult.qr(
            "00020101021138...VIETQR",
            new BankTransferDetails("970422", "0912888866", "CONG TY VOX", new BigDecimal("500000"), "VOX 8f3c1a04"),
            "https://payos.test/checkout",
            "link-1"));

        var response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS"));

        assertThat(response.action()).isEqualTo("QR");
        assertThat(response.qrCode()).isEqualTo("00020101021138...VIETQR");
        assertThat(response.transfer().transferContent()).isEqualTo("VOX 8f3c1a04");

        var stored = savedById.get(response.paymentId());
        assertThat(stored.getCheckoutUrl()).isEqualTo("https://payos.test/checkout");
        assertThat(stored.getProviderPayloadJson()).contains("VIETQR").contains("VOX 8f3c1a04");
    }

    /** Và dựng lại được từ chính payload đó khi trường quay lại đơn còn treo. */
    @Test
    void should_rebuild_the_qr_from_the_stored_payload_on_a_live_attempt() {
        var live = existingAttempt();
        live.setCheckoutUrl("https://payos.test/checkout");
        live.setProviderPayloadJson(
            "{\"qrCode\":\"00020101021138...VIETQR\",\"transfer\":{\"bankBin\":\"970422\","
                + "\"accountNumber\":\"0912888866\",\"accountName\":\"CONG TY VOX\","
                + "\"amountVnd\":500000,\"transferContent\":\"VOX 8f3c1a04\"}}");
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(live));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.PENDING));

        var response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS"));

        assertThat(response.action()).isEqualTo("QR");
        assertThat(response.qrCode()).isEqualTo("00020101021138...VIETQR");
        assertThat(response.transfer().accountNumber()).isEqualTo("0912888866");
        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /**
     * Hệ quả trực tiếp của việc commit trước: lời gọi tạo link hỏng TRƯỚC khi cổng kịp dựng phiên sẽ
     * để lại một dòng PENDING mang mã mà bên cổng chưa từng thấy. Không có nhánh NOT_FOUND thì lần thử
     * đó treo tới lúc đơn hết hạn và trường không xin được link mới.
     */
    @Test
    void should_retire_an_attempt_the_gateway_has_never_heard_of() {
        var orphan = existingAttempt();
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(orphan));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.NOT_FOUND));
        when(paymentProcessPort.createPaymentLink(any()))
            .thenReturn(PaymentCheckoutResult.redirect("https://payos.test/checkout", "link-2"));

        var response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS"));

        assertThat(orphan.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.providerOrderRef()).isEqualTo("123456");
        verify(paymentProcessPort).createPaymentLink(any());
    }

    /**
     * Mặt kia của NOT_FOUND, và là ca nguy hiểm hơn: lần thử vừa mở vài giây trước thì NOT_FOUND
     * KHÔNG có nghĩa là nó chết. Rất có thể một tab khác vừa commit dòng PENDING và đang đợi
     * createPaymentLink trả về -- cổng chưa dựng phiên nên trả NOT_FOUND hoàn toàn đúng.
     *
     * <p>Chốt hỏng nó lúc này là tạo ra ca tệ nhất: tab kia nhận link thật, trường trả tiền vào đó,
     * mà dòng ứng với khoản tiền ấy đã FAILED nên callback không giao gì (PAID_AFTER_WRITE_OFF).
     * Thà bắt đợi vài phút.
     */
    @Test
    void should_not_believe_not_found_for_an_attempt_that_was_just_opened() {
        var justOpened = existingAttempt(Instant.now().minus(3, ChronoUnit.SECONDS));
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(justOpened));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.NOT_FOUND));

        assertThatThrownBy(() -> useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("thử lại sau vài phút");

        assertThat(justOpened.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /**
     * Cổng nói đã thu tiền cho lần thử đang treo: KHÔNG được phát thêm link nào. Phát nữa là hai phiên
     * cùng sống trên một đơn, tức mời trường trả lần thứ hai.
     */
    @Test
    void should_refuse_to_issue_a_second_link_when_the_gateway_says_the_attempt_is_paid() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(existingAttempt()));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.PAID));

        assertThatThrownBy(() -> useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã được thanh toán");

        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /** Phiên cũ vẫn sống thì trả lại ĐÚNG link đó, không sinh mã mới. */
    @Test
    void should_reuse_a_live_attempt_instead_of_opening_a_new_one() {
        var live = existingAttempt();
        live.setCheckoutUrl("https://payos.test/still-alive");
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.of(live));
        when(paymentProcessPort.getPaymentLinkStatus("orphan-ref"))
            .thenReturn(new PaymentLinkStatusResult(PaymentLinkRemoteStatus.PENDING));

        var response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS"));

        assertThat(response.checkoutUrl()).isEqualTo("https://payos.test/still-alive");
        verify(paymentProcessPort, never()).createPaymentLink(any());
    }

    /**
     * Nhánh REDIRECT phải LƯU được link, nếu không thì test trên chỉ đúng trên giấy: entity từng khai
     * checkout_url là updatable = false, nên câu UPDATE của attachCheckoutUrl lặng lẽ bỏ qua cột đó
     * -- không lỗi, không cảnh báo, cột ở NULL vĩnh viễn. Hệ quả là mọi lần quay lại trả tiếp đều rơi
     * vào "đang có một phiên thanh toán chờ xử lý" thay vì nhận lại link của chính mình.
     */
    @Test
    void should_persist_the_checkout_url_for_a_redirect_gateway() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentProcessPort.createPaymentLink(any()))
            .thenReturn(PaymentCheckoutResult.redirect("https://payos.test/checkout", "link-1"));

        var response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS"));

        assertThat(savedById.get(response.paymentId()).getCheckoutUrl())
            .isEqualTo("https://payos.test/checkout");
    }

    /**
     * Ngược lại, cổng dạng FORM_POST KHÔNG được lưu: actionUrl của nó là một endpoint cố định dùng
     * chung mọi đơn, còn thứ nhận dạng phiên nằm ở bộ field mang chữ ký (cố ý không lưu). Lưu mỗi URL
     * rồi lần sau redirect thẳng tới đó là đưa trường vào một trang init trống.
     *
     * <p>Nhờ vậy checkout_url IS NULL giữ đúng một nghĩa -- "lần thử này không dựng lại được" -- và
     * đó chính là điều kiện resolvePendingAttempt đang kiểm.
     */
    @Test
    void should_not_persist_a_form_post_action_url() {
        when(paymentRecordRepository.findPendingByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentProcessPort.createPaymentLink(any()))
            .thenReturn(PaymentCheckoutResult.formPost(
                "https://sepay.test/v1/checkout/init", Map.of("merchant", "vox", "signature", "abc")));

        var response = useCase.execute(new CreatePaymentCheckoutUrlCommand(ORDER_ID, "PAYOS"));

        assertThat(savedById.get(response.paymentId()).getCheckoutUrl()).isNull();
    }

    private static Order payableOrder() {
        var order = new Order();
        order.setId(ORDER_ID);
        order.setSchoolId(SCHOOL_ID);
        order.setType(OrderType.TOPUP);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmountVnd(new BigDecimal("500000"));
        order.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
        return order;
    }

    private static PaymentRecord existingAttempt() {
        // Mặc định là lần thử ĐÃ QUA ân hạn: phần lớn phép kiểm dưới đây nói về hành vi ở trạng thái
        // ổn định, không phải về cửa sổ vài phút ngay sau lúc mở lần thử.
        return existingAttempt(Instant.now().minus(10, ChronoUnit.MINUTES));
    }

    private static PaymentRecord existingAttempt(Instant createdAt) {
        var attempt = PaymentRecord.forEBankingCheckout(
            ORDER_ID, new BigDecimal("500000"), PaymentProvider.PAYOS, "orphan-ref", createdAt);
        attempt.setId(UUID.randomUUID());
        return attempt;
    }
}
