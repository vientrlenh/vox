package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.OrderSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;

/**
 * Lưới an toàn cho các đơn còn PENDING: đi hỏi thẳng cổng thanh toán thay vì ngồi đợi callback.
 *
 * <p>Cần thiết vì callback KHÔNG được đảm bảo: trường đóng tab giữa chừng, webhook trượt lúc mình
 * đang deploy, hoặc cổng đơn giản là gọi hụt. Không có job này thì tiền đã vào tài khoản mà đơn vẫn
 * treo, trường không nhận được hàng và cũng không đặt lại được (uq_orders_one_open_subscription_order
 * chặn đơn đăng ký thứ hai).
 *
 * <p>Thay cho PendingInvoiceReconciler cũ. Khác biệt về bản chất: bản cũ quét HÓA ĐƠN vì hóa đơn
 * mang luôn trạng thái thanh toán; giờ hóa đơn chỉ phát sau khi tiền về, nên thứ phải quét là ĐƠN,
 * còn thứ đem đi hỏi cổng là LẦN THỬ gắn với đơn đó.
 *
 * <p>Hai việc trong CÙNG một lượt và đúng thứ tự này -- đối soát trước, hết hạn sau -- vì chúng phụ
 * thuộc nhau: một đơn vừa quá hạn vừa còn lần thử treo thì rất có thể trường vừa trả ở phút chót.
 * Đóng đơn trước khi hỏi cổng là tự tạo ra ca "đã thu tiền nhưng không còn đơn để giao".
 *
 * <p>Cả hai cổng đều tự gửi lại khi không nhận được phản hồi thành công, nhưng KHÔNG cổng nào công
 * bố lịch retry cho sản phẩm đang dùng (SePay chỉ công bố lịch 8 lần/33 phút cho webhook biến động
 * số dư -- một sản phẩm KHÁC với Payment Gateway ở đây). Nghĩa là không được coi retry của cổng là
 * lưới an toàn: job này mới là đường phục hồi duy nhất mình kiểm soát được.
 */
@Component
public class PendingOrderReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingOrderReconciler.class);

    // 400ms giữa hai lần hỏi (~2.5 req/s). Cả PayOS lẫn SePay đều KHÔNG công bố hạn mức gọi API cụ
    // thể, nên con số này là lựa chọn thủ thế chứ không phải bám theo ngưỡng nào: job chạy nền,
    // chậm thêm vài giây một lượt không ai thấy, còn bị cổng chặn vì dội thì mất hẳn đường đối soát.
    private static final long THROTTLE_MILLIS = 400L;

    // Chặn trên số lần gọi cổng mỗi lượt để một đợt tồn đọng bất thường không biến job thành cuộc
    // gọi API kéo dài hàng chục phút. Phần còn lại để lượt sau (5 phút nữa).
    private static final int MAX_REMOTE_CALLS_PER_RUN = 200;

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentProcessResolver paymentProcessResolver;
    private final OrderSettlementService orderSettlementService;

    public PendingOrderReconciler(
            OrderRepository orderRepository,
            PaymentRecordRepository paymentRecordRepository,
            PaymentProcessResolver paymentProcessResolver,
            OrderSettlementService orderSettlementService) {
        this.orderRepository = orderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentProcessResolver = paymentProcessResolver;
        this.orderSettlementService = orderSettlementService;
    }

    /**
     * KHÔNG {@code @Transactional} ở đây. Một lượt quét có thể kéo dài nhiều phút vì phải gọi ra
     * mạng ngoài, mà gói tất cả vào một transaction thì giữ kết nối DB suốt quãng đó và một đơn lỗi
     * sẽ cuốn theo mọi đơn đã chốt thành công trước nó. Mỗi đơn tự chốt trong transaction riêng của
     * OrderSettlementService.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcile() {
        var now = Instant.now();
        var pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            return;
        }

        var remoteCalls = 0;
        var settled = 0;
        var expired = 0;

        for (var order : pendingOrders) {
            var pendingAttempt = paymentRecordRepository.findPendingByOrderId(order.getId()).orElse(null);

            if (pendingAttempt != null) {
                if (remoteCalls >= MAX_REMOTE_CALLS_PER_RUN) {
                    LOGGER.warn("Dừng đối soát ở {} lần gọi cổng trong lượt này, phần còn lại để lượt sau",
                        MAX_REMOTE_CALLS_PER_RUN);
                    break;
                }
                // Giãn nhịp TRƯỚC mỗi lời gọi (trừ lần đầu) thay vì sau: vòng lặp thoát sớm thì cũng
                // không phải trả giá bằng một lần ngủ vô ích.
                if (remoteCalls > 0 && !throttle()) {
                    break;
                }
                remoteCalls++;
                if (reconcileAttempt(order, pendingAttempt)) {
                    settled++;
                    // Đơn vừa được chốt PAID thì không còn là đơn quá hạn nữa -- sang đơn kế tiếp.
                    continue;
                }
            }

            // Tới đây đơn chắc chắn không còn lần thử nào đang treo: hoặc chưa từng có, hoặc vừa bị
            // chốt hỏng ở trên. expireIfOverdue vẫn tự kiểm tra lại dưới row lock vì giữa hai bước
            // này trường hoàn toàn có thể vừa bấm thanh toán lại.
            if (expireOverdue(order, now)) {
                expired++;
            }
        }

        if (settled > 0 || expired > 0) {
            LOGGER.info("[RECONCILER] Quét {} đơn PENDING: chốt {} đơn đã thu được tiền, đóng {} đơn quá hạn",
                pendingOrders.size(), settled, expired);
        }
    }

    /**
     * Hỏi cổng về một lần thử đang treo và chốt nó.
     *
     * @return true nếu tiền đã về và đơn vừa được giao hàng
     */
    private boolean reconcileAttempt(Order order, PaymentRecord attempt) {
        try {
            var remoteStatus = paymentProcessResolver.resolve(attempt.getProvider())
                .getPaymentLinkStatus(attempt.getProviderOrderRef())
                .status();

            if (remoteStatus == PaymentLinkRemoteStatus.PAID) {
                LOGGER.info("[RECONCILER] Chốt lần thanh toán {} (orderRef={}) thành PAID qua polling {}",
                    attempt.getId(), attempt.getProviderOrderRef(), attempt.getProvider());
                orderSettlementService.settlePaid(attempt, Instant.now());
                return true;
            }

            // Chỉ đóng LẦN THỬ chứ không đóng đơn -- cùng lý do như ở callback: trường phải bấm trả
            // lại được mà không cần đặt đơn mới. Xem OrderSettlementService.failAttempt.
            if (isFinalFailure(remoteStatus)) {
                LOGGER.info("[RECONCILER] Chốt lần thanh toán {} (orderRef={}) thành FAILED do cổng {} báo {}",
                    attempt.getId(), attempt.getProviderOrderRef(), attempt.getProvider(), remoteStatus);
                orderSettlementService.failAttempt(attempt, PaymentStatus.FAILED);
            }
            return false;
        } catch (Exception e) {
            // Nuốt lỗi của TỪNG đơn: một cổng đang hỏng không được phép chặn việc đối soát các đơn
            // còn lại trong cùng lượt quét.
            LOGGER.warn("Lỗi khi đối soát đơn {} provider={} orderRef={}",
                order.getId(), attempt.getProvider(), attempt.getProviderOrderRef(), e);
            return false;
        }
    }

    private boolean expireOverdue(Order order, Instant now) {
        try {
            return orderSettlementService.expireIfOverdue(order.getId(), now);
        } catch (Exception e) {
            LOGGER.warn("Lỗi khi đóng đơn quá hạn {}", order.getId(), e);
            return false;
        }
    }

    /** null hoặc PENDING/PROCESSING/UNDERPAID = chưa phải trạng thái cuối, chưa chốt gì. */
    private boolean isFinalFailure(PaymentLinkRemoteStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case CANCELLED, EXPIRED, FAILED -> true;
            default -> false;
        };
    }

    // Trả false khi bị ngắt (vd ứng dụng đang tắt) để vòng lặp dừng hẳn thay vì chạy tiếp không
    // throttle và dội API của cổng.
    private boolean throttle() {
        try {
            Thread.sleep(THROTTLE_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Job đối soát bị ngắt giữa chừng, dừng lượt quét này");
            return false;
        }
    }
}
