package com.sep.vox.application.port.input.usecase.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreatePaymentCheckoutUrlCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.payment.PaymentCheckoutResponse;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;

/**
 * Phát URL thanh toán cho một đơn. Thay cho CẢ BA use case cũ
 * (CreatePaymentLinkForSubscriptionRequest / ForRenewal / ForTokenPurchase): ba luồng đó khác nhau ở
 * chỗ "mua cái gì", mà chuyện đó giờ đã nằm gọn trong Order rồi -- tới bước thu tiền thì chúng giống
 * hệt nhau, chỉ là một số tiền và một đơn để gắn vào.
 *
 * <p>Hiện chỉ nhận thanh toán qua cổng thứ ba (E_BANKING) -- xem PaymentRecord.forEBankingCheckout.
 */
@Service
public class CreatePaymentCheckoutUrlUseCase
        implements IUseCase<CreatePaymentCheckoutUrlCommand, PaymentCheckoutResponse> {

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentProcessResolver paymentProcessResolver;
    private final UserContextPort userContextPort;
    private final TransactionTemplate transactionTemplate;

    public CreatePaymentCheckoutUrlUseCase(
            OrderRepository orderRepository,
            PaymentRecordRepository paymentRecordRepository,
            PaymentProcessResolver paymentProcessResolver,
            UserContextPort userContextPort,
            PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentProcessResolver = paymentProcessResolver;
        this.userContextPort = userContextPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * KHÔNG có {@code @Transactional} ở đây, và đó là điểm chính của cả class.
     *
     * <p>Bản trước bọc cả hàm trong MỘT transaction, nên hai chuyện cùng sai:
     *
     * <ul>
     *   <li>Việc "ghi dòng payment_records TRƯỚC khi gọi cổng" chỉ trước trong phạm vi transaction.
     *   Cổng dựng phiên xong nhưng phản hồi rơi (timeout) là rollback, xóa luôn dòng mang
     *   provider_order_ref -- trong khi trang thanh toán bên cổng vẫn sống. Trường trả vào đó thì
     *   callback tra ngược không ra lần thử nào (UNKNOWN_PAYMENT), job đối soát cũng không thấy gì để
     *   hỏi. Tiền về, không giao gì, không còn dấu vết nào ở phía mình.</li>
     *   <li>Khóa FOR UPDATE trên dòng đơn bị giữ suốt HAI lượt gọi mạng. Cổng chậm là dòng đơn kẹt
     *   theo, và {@code OrderSettlementService.settlePaid} -- cũng mở đầu bằng findByIdForUpdate --
     *   phải xếp hàng sau nó. Cổng lỗi thì chính đường ghi nhận tiền về bị chặn.</li>
     * </ul>
     *
     * <p>Nên chia thành các transaction NGẮN, mỗi cái ĐÓNG LẠI trước khi ra mạng. Dòng PENDING vì thế
     * được commit thật trước lúc gọi tạo link: gọi hỏng thì nó nằm lại, và lần bấm sau đi vào đúng
     * nhánh {@code resolvePendingAttempt} để hỏi cổng xem mã đó đã ra tiền chưa.
     *
     * <p>KHÔNG sửa được bằng cách chỉ bọc riêng phần ghi dòng PENDING trong REQUIRES_NEW mà vẫn giữ
     * transaction ngoài: INSERT vào payment_records có khóa ngoại trỏ orders nên Postgres lấy
     * {@code FOR KEY SHARE} trên đúng dòng đơn mà transaction ngoài đang giữ {@code FOR UPDATE}. Hai
     * chế độ đó xung khắc, nên transaction trong chờ transaction ngoài, mà ngoài thì đang chờ trong
     * -- tự khóa chính mình tới khi hết lock_timeout.
     */
    @Override
    public PaymentCheckoutResponse execute(CreatePaymentCheckoutUrlCommand input) {
        var provider = providerOf(input.provider());

        var pending = inTransaction(() -> loadPendingAttempt(input.orderId()));
        if (pending != null) {
            // Gọi cổng NGOÀI mọi transaction: đây là lượt mạng thứ nhất.
            var remoteStatus = paymentProcessResolver.resolve(pending.getProvider())
                .getPaymentLinkStatus(pending.getProviderOrderRef())
                .status();

            var reusable = inTransaction(() -> resolvePendingAttempt(input.orderId(), pending, remoteStatus));
            if (reusable != null) {
                return reusable;
            }
        }

        // Dòng PENDING được COMMIT ở đây, trước lượt mạng thứ hai.
        var prepared = inTransaction(() -> openAttempt(input.orderId(), provider));

        var result = paymentProcessResolver.resolve(provider).createPaymentLink(new CreatePaymentLinkCommand(
            prepared.providerOrderRef(),
            prepared.amountVnd(),
            "VOX-" + prepared.providerOrderRef(),
            // Hạn của link lấy từ ĐƠN, không phải hằng số của adapter: link không được sống lâu hơn
            // đơn (trả tiền cho đơn đã chết) và đơn không được sống lâu hơn link (đơn khóa chỗ mà
            // không còn cách nào trả).
            prepared.expiresAt()
        ));

        inTransaction(() -> attachCheckoutUrl(prepared.paymentId(), result.actionUrl()));

        return PaymentCheckoutResponse.from(
            input.orderId(), prepared.paymentId(), prepared.providerOrderRef(), provider, result);
    }

    /**
     * Khóa đơn, kiểm tra quyền và trạng thái, trả về lần thử đang treo (hoặc null).
     *
     * <p>Khóa nhả ngay khi transaction này đóng, tức TRƯỚC lượt gọi cổng. Nó vẫn cần thiết: trường mở
     * hai tab rồi bấm gần như cùng lúc là chuyện bình thường, và khóa khiến hai luồng đó nhìn thấy
     * cùng một sự thật về "đã có lần thử nào treo chưa" thay vì cùng đọc rồi cùng phát link.
     */
    private PaymentRecord loadPendingAttempt(UUID orderId) {
        var order = requirePayableOrder(orderId);
        return paymentRecordRepository.findPendingByOrderId(order.getId()).orElse(null);
    }

    /**
     * Xử lý lần thử ĐANG TREO trước khi nghĩ tới việc phát cái mới, vì
     * uq_payment_records_one_pending_per_order chỉ cho phép một lần thử treo trên mỗi đơn.
     *
     * <p>Phải ĐI HỎI CỔNG chứ không tự suy từ dữ liệu của mình -- việc đó đã làm ở
     * {@code execute}, ngoài transaction. Dòng còn PENDING chỉ nói rằng chưa ai báo về cho ta, không
     * nói rằng trường chưa trả; bỏ qua bước hỏi rồi phát link mới là mở đúng cánh cửa thu tiền hai
     * lần mà mọi ràng buộc trong V2 đang cố đóng lại.
     *
     * <p>Trả về response nếu lần thử cũ còn dùng lại được, trả null nếu đã chốt xong và được phép phát
     * lần thử mới.
     */
    private PaymentCheckoutResponse resolvePendingAttempt(
            UUID orderId, PaymentRecord pending, PaymentLinkRemoteStatus remoteStatus) {
        var order = requirePayableOrder(orderId);

        switch (remoteStatus) {
            case PAID -> throw new IllegalStateException(
                "Đơn hàng này đã được thanh toán, hệ thống đang ghi nhận. Vui lòng đợi trong giây lát.");

            case CANCELLED, EXPIRED, FAILED, NOT_FOUND -> {
                // Ân hạn cho NOT_FOUND: lần thử treo có thể là của MỘT TAB KHÁC vừa commit dòng
                // PENDING và đang đợi createPaymentLink trả về. Chốt hỏng nó lúc ấy là đánh hỏng một
                // lần thử sắp sống -- xem PaymentRecord.canRetireOnGatewayNotFound.
                if (remoteStatus == PaymentLinkRemoteStatus.NOT_FOUND
                        && !pending.canRetireOnGatewayNotFound(Instant.now())) {
                    throw new IllegalStateException(
                        "Đơn hàng vừa mở một phiên thanh toán mà cổng chưa xác nhận. "
                            + "Vui lòng thử lại sau vài phút.");
                }
                // Cổng đã xác nhận lần thử này KHÔNG ra tiền -- chốt nó lại để nhường chỗ cho lần mới.
                // Chỉ được chốt SAU khi có xác nhận đó: đánh hỏng một lần thử còn sống nghĩa là lúc
                // trường trả vào link cũ sẽ không còn dòng nào đang chờ khoản tiền đó.
                //
                // NOT_FOUND nằm chung nhóm này vì nó là hệ quả trực tiếp của việc commit dòng PENDING
                // trước khi gọi cổng: lời gọi tạo link hỏng TRƯỚC lúc cổng kịp dựng phiên sẽ để lại
                // một dòng mang mã mà bên cổng chưa từng thấy. Không có nhánh này thì lần thử đó treo
                // tới lúc đơn hết hạn và trường không xin được link mới. An toàn vì cổng khẳng định
                // không có phiên nào mang mã đó -- khác hẳn "hỏi không được", thứ mà adapter ném ra
                // ngoài chứ không quy về đây.
                pending.setStatus(PaymentStatus.FAILED);
                paymentRecordRepository.save(pending);
                return null;
            }

            // Cổng nói vẫn đang chờ trả: trả lại ĐÚNG link cũ thay vì phát link mới. Phát mới thì link
            // cũ vẫn trả được nhưng không còn dòng PENDING nào ứng với nó.
            default -> {
                if (pending.getCheckoutUrl() == null) {
                    // Cổng dạng FORM_POST: bộ field mang chữ ký nên cố ý không lưu, không dựng lại
                    // được ở đây. Để trường đợi hết hạn lần thử cũ còn hơn phát thêm một link song song.
                    throw new IllegalStateException(
                        "Đơn hàng đang có một phiên thanh toán chờ xử lý. Vui lòng hoàn tất phiên đó hoặc thử lại sau.");
                }
                return PaymentCheckoutResponse.redirectTo(
                    order.getId(), pending.getId(), pending.getProviderOrderRef(),
                    pending.getProvider(), pending.getCheckoutUrl());
            }
        }
    }

    /**
     * Chốt một lần thử mới và COMMIT nó, để lượt gọi cổng phía sau luôn có một dòng đại diện.
     *
     * <p>Khóa lại đơn và kiểm tra lại từ đầu chứ không tin vào kết quả của
     * {@code loadPendingAttempt}: giữa hai transaction, đơn có thể đã bị hủy, đã hết hạn, hoặc một
     * tab khác đã kịp mở lần thử của nó.
     */
    private PreparedAttempt openAttempt(UUID orderId, PaymentProvider provider) {
        var order = requirePayableOrder(orderId);

        // Không đưa số tiền <= 0 sang cổng. Không cổng nào nhận, nên nếu lọt qua thì trường nhận một
        // lỗi của nhà cung cấp không đọc được, còn đơn thì nằm PENDING tới lúc hết hạn. Đường sinh ra
        // đơn 0đ đã được bịt ở SubscriptionUpgradePolicyService.calculateUnusedCredit; chốt chặn này ở
        // đây để mọi đường TƯƠNG LAI dẫn tới cùng chỗ đều dừng lại với câu nói người đọc hiểu được,
        // chứ không phải để sửa lại lần nữa cùng một lỗi.
        if (order.getTotalAmountVnd() == null || order.getTotalAmountVnd().signum() <= 0) {
            throw new IllegalStateException(
                "Đơn hàng không có số tiền cần thanh toán, không thể tạo phiên thanh toán. Vui lòng liên hệ hỗ trợ.");
        }

        // Mã đơn do adapter sinh và LUÔN MỚI, kể cả khi đây là lần trả lại của cùng một đơn: PayOS từ
        // chối orderCode trùng, SePay đòi order_invoice_number duy nhất. Cả hai adapter sinh mã cục bộ
        // (đồng hồ / UUID) chứ không gọi mạng, nên gọi trong transaction là an toàn.
        var providerOrderRef = paymentProcessResolver.resolve(provider).newOrderRef();

        var payment = paymentRecordRepository.save(PaymentRecord.forEBankingCheckout(
            order.getId(), order.getTotalAmountVnd(), provider, providerOrderRef, Instant.now()));

        return new PreparedAttempt(
            payment.getId(), providerOrderRef, order.getTotalAmountVnd(), order.getExpiresAt());
    }

    /**
     * Chỉ lưu URL. Bộ field FORM_POST mang chữ ký HMAC -- lưu xuống DB là ai đọc được DB cũng dựng
     * lại được một checkout hợp lệ, mà chúng lại tính lại được nên lưu cũng không thêm gì.
     *
     * <p>Không khóa đơn ở đây: chỉ đụng đúng dòng payment_records vừa tạo, và việc nó có được phép
     * tồn tại hay không đã chốt xong ở {@code openAttempt}.
     */
    private Void attachCheckoutUrl(UUID paymentId, String checkoutUrl) {
        var payment = paymentRecordRepository.findById(paymentId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lần thanh toán vừa tạo"));
        payment.setCheckoutUrl(checkoutUrl);
        paymentRecordRepository.save(payment);
        return null;
    }

    /**
     * Khóa dòng đơn rồi kiểm tra ba điều kiện bắt buộc để được phát link. Gọi ở ĐẦU mỗi transaction
     * có ghi, vì mỗi transaction là một lần nhìn dữ liệu mới.
     */
    private Order requirePayableOrder(UUID orderId) {
        var order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));

        if (!order.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng không còn ở trạng thái chờ thanh toán.");
        }
        // Đơn quá hạn thì KHÔNG phát link mới: hạn này đã gửi sang cổng ở lần thử trước, và cấp thêm
        // thời gian ở đây nghĩa là sửa một cam kết đã chốt. Muốn mua tiếp thì đặt đơn mới.
        if (!Instant.now().isBefore(order.getExpiresAt())) {
            throw new IllegalStateException("Đơn hàng đã hết hạn thanh toán, hãy đặt đơn mới.");
        }
        return order;
    }

    /**
     * REQUIRES_NEW chứ không phải REQUIRED: nếu một ngày nào đó có người gọi use case này từ bên trong
     * một transaction sẵn có, REQUIRED sẽ nhập vào transaction đó và cả class quay lại đúng lỗi cũ --
     * khóa đơn bị giữ qua lượt gọi mạng, dòng PENDING bị rollback cùng. Cờ này khiến mỗi giai đoạn tự
     * đóng lại bất kể ngữ cảnh gọi.
     *
     * <p>An toàn vì người gọi hiện tại là PaymentController (REST, không transaction) nên không có
     * dòng đơn nào đang bị khóa ở ngoài để mà chờ nhau.
     */
    private <T> T inTransaction(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    /** Dữ liệu cần mang qua ranh giới transaction để gọi cổng -- đều là POJO đã tách khỏi persistence context. */
    private record PreparedAttempt(
        UUID paymentId, String providerOrderRef, BigDecimal amountVnd, Instant expiresAt) {}

    private static PaymentProvider providerOf(String provider) {
        var normalized = StringNormalization.normalizeCode(provider);
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Cổng thanh toán không được để trống");
        }
        try {
            return PaymentProvider.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cổng thanh toán không hợp lệ: " + provider);
        }
    }
}
