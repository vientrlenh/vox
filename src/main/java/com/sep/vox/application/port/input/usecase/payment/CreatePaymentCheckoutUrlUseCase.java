package com.sep.vox.application.port.input.usecase.payment;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreatePaymentCheckoutUrlCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.payment.PaymentCheckoutResponse;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
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

    public CreatePaymentCheckoutUrlUseCase(
            OrderRepository orderRepository,
            PaymentRecordRepository paymentRecordRepository,
            PaymentProcessResolver paymentProcessResolver,
            UserContextPort userContextPort) {
        this.orderRepository = orderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentProcessResolver = paymentProcessResolver;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PaymentCheckoutResponse execute(CreatePaymentCheckoutUrlCommand input) {
        var provider = providerOf(input.provider());

        // Khóa đơn ngay: trường mở hai tab rồi bấm thanh toán gần như cùng lúc là chuyện bình thường.
        // Không khóa thì cả hai luồng đều thấy "chưa có lần thử nào treo" và cùng phát link -- một
        // trong hai sẽ vỡ ở uq_payment_records_one_pending_per_order, sau khi đã kịp gọi sang cổng và
        // tạo ra một đơn thanh toán thật bên đó mà bên mình không có dòng nào đại diện.
        var order = orderRepository.findByIdForUpdate(input.orderId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));

        if (!order.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng không còn ở trạng thái chờ thanh toán.");
        }

        var now = Instant.now();
        // Đơn quá hạn thì KHÔNG phát link mới: hạn này đã gửi sang cổng ở lần thử trước, và cấp thêm
        // thời gian ở đây nghĩa là sửa một cam kết đã chốt. Muốn mua tiếp thì đặt đơn mới.
        if (!now.isBefore(order.getExpiresAt())) {
            throw new IllegalStateException("Đơn hàng đã hết hạn thanh toán, hãy đặt đơn mới.");
        }

        var reusable = resolvePendingAttempt(order);
        if (reusable != null) {
            return reusable;
        }

        return issueNewCheckout(order, provider, now);
    }

    /**
     * Xử lý lần thử ĐANG TREO trước khi nghĩ tới việc phát cái mới, vì
     * uq_payment_records_one_pending_per_order chỉ cho phép một lần thử treo trên mỗi đơn.
     *
     * <p>Trả về response nếu lần thử cũ còn dùng lại được, trả null nếu đã chốt xong và được phép phát
     * lần thử mới.
     */
    private PaymentCheckoutResponse resolvePendingAttempt(Order order) {
        var pending = paymentRecordRepository.findPendingByOrderId(order.getId()).orElse(null);
        if (pending == null) {
            return null;
        }

        // Phải ĐI HỎI CỔNG chứ không tự suy từ dữ liệu của mình: dòng còn PENDING chỉ nói rằng chưa ai
        // báo về cho ta, không nói rằng trường chưa trả. Bỏ qua bước này rồi phát link mới là mở đúng
        // cánh cửa thu tiền hai lần mà mọi ràng buộc trong V2 đang cố đóng lại.
        var remoteStatus = paymentProcessResolver.resolve(pending.getProvider())
            .getPaymentLinkStatus(pending.getProviderOrderRef())
            .status();

        switch (remoteStatus) {
            case PAID -> throw new IllegalStateException(
                "Đơn hàng này đã được thanh toán, hệ thống đang ghi nhận. Vui lòng đợi trong giây lát.");

            case CANCELLED, EXPIRED, FAILED -> {
                // Cổng đã xác nhận lần thử này KHÔNG ra tiền -- chốt nó lại để nhường chỗ cho lần mới.
                // Chỉ được chốt SAU khi có xác nhận đó: đánh hỏng một lần thử còn sống nghĩa là lúc
                // trường trả vào link cũ sẽ không còn dòng nào đang chờ khoản tiền đó.
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

    private PaymentCheckoutResponse issueNewCheckout(Order order, PaymentProvider provider, Instant now) {
        // Không đưa số tiền <= 0 sang cổng. Không cổng nào nhận, nên nếu lọt qua thì trường nhận một
        // lỗi của nhà cung cấp không đọc được, còn đơn thì nằm PENDING tới lúc hết hạn. Đường sinh ra
        // đơn 0đ đã được bịt ở SubscriptionUpgradePolicyService.calculateUnusedCredit; chốt chặn này ở
        // đây để mọi đường TƯƠNG LAI dẫn tới cùng chỗ đều dừng lại với câu nói người đọc hiểu được,
        // chứ không phải để sửa lại lần nữa cùng một lỗi.
        if (order.getTotalAmountVnd() == null || order.getTotalAmountVnd().signum() <= 0) {
            throw new IllegalStateException(
                "Đơn hàng không có số tiền cần thanh toán, không thể tạo phiên thanh toán. Vui lòng liên hệ hỗ trợ.");
        }
        var paymentProcessPort = paymentProcessResolver.resolve(provider);

        // Mã đơn do adapter sinh và LUÔN MỚI, kể cả khi đây là lần trả lại của cùng một đơn: PayOS từ
        // chối orderCode trùng, SePay đòi order_invoice_number duy nhất.
        var providerOrderRef = paymentProcessPort.newOrderRef();

        // Ghi dòng PENDING TRƯỚC khi gọi sang cổng. Bắt buộc theo thứ tự này: provider_order_ref là
        // thứ duy nhất cổng gửi kèm khi báo về, nên nếu gọi trước mà ghi sau thì có một khoảng thời
        // gian callback về tới nơi mà không tra ngược được đơn nào.
        var payment = paymentRecordRepository.save(PaymentRecord.forEBankingCheckout(
            order.getId(), order.getTotalAmountVnd(), provider, providerOrderRef, now));

        var result = paymentProcessPort.createPaymentLink(new CreatePaymentLinkCommand(
            providerOrderRef,
            order.getTotalAmountVnd(),
            "VOX-" + providerOrderRef,
            // Hạn của link lấy từ ĐƠN, không phải hằng số của adapter: link không được sống lâu hơn
            // đơn (trả tiền cho đơn đã chết) và đơn không được sống lâu hơn link (đơn khóa chỗ mà
            // không còn cách nào trả).
            order.getExpiresAt()
        ));

        // Chỉ lưu URL. Bộ field FORM_POST mang chữ ký HMAC -- lưu xuống DB là ai đọc được DB cũng dựng
        // lại được một checkout hợp lệ, mà chúng lại tính lại được nên lưu cũng không thêm gì.
        payment.setCheckoutUrl(result.actionUrl());
        paymentRecordRepository.save(payment);

        return PaymentCheckoutResponse.from(
            order.getId(), payment.getId(), providerOrderRef, provider, result);
    }

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
