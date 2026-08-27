package com.sep.vox.application.port.input.usecase.order;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CancelOrderCommand;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;

/**
 * Trường tự bỏ một đơn chưa trả tiền.
 *
 * <p>Có mặt vì {@code uq_orders_one_open_subscription_order} chỉ cho phép MỘT đơn đăng ký còn mở
 * mỗi trường: không có đường hủy thì một trường đặt nhầm gói bị chặn đặt đơn mới cho tới khi đơn cũ
 * tự hết hạn (tối đa 24 tiếng theo Order.PENDING_TTL). Đây là bản THỦ CÔNG của việc mà
 * PendingOrderReconciler làm tự động khi đơn quá hạn.
 *
 * <p>Phần khó KHÔNG nằm ở việc đổi trạng thái đơn mà ở phiên thanh toán còn sống bên cổng: đóng đơn
 * mà để link sống tiếp nghĩa là trường vẫn trả vào đó được, và lúc tiền về thì
 * OrderSettlementService thấy đơn không còn PENDING nên không giao hàng -- thu tiền xong không cấp
 * gì, mà hệ thống hiện chưa có luồng hoàn tiền nào. Nên quy tắc ở đây là: chỉ đóng đơn khi CHẮC
 * CHẮN không còn đường nào ra tiền nữa.
 */
@Service
public class CancelOrderUseCase implements IUseCase<CancelOrderCommand, UUID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelOrderUseCase.class);

    private static final String CANCELLATION_REASON = "Trường chủ động hủy đơn";

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentProcessResolver paymentProcessResolver;
    private final UserContextPort userContextPort;

    public CancelOrderUseCase(
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
    public UUID execute(CancelOrderCommand input) {
        // Khóa đơn: hủy và callback "đã trả" hoàn toàn có thể về cùng lúc. Không khóa thì cả hai
        // cùng thấy đơn PENDING, một bên đóng CANCELLED, một bên đóng SUCCESS -- thứ tự ghi quyết
        // định kết quả, và một trong hai kết quả là sai.
        var order = orderRepository.findByIdForUpdate(input.orderId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));

        if (!order.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng không còn ở trạng thái chờ thanh toán, không thể hủy.");
        }

        closeLivePaymentAttempt(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        LOGGER.info("Trường {} hủy đơn {}", order.getSchoolId(), order.getId());
        return order.getId();
    }

    /**
     * Đảm bảo đơn không còn phiên nào có thể ra tiền, hoặc ném lỗi. Trả về bình thường = an toàn để
     * đóng đơn.
     */
    private void closeLivePaymentAttempt(Order order) {
        var attempt = paymentRecordRepository.findPendingByOrderId(order.getId()).orElse(null);
        if (attempt == null) {
            return;
        }

        var paymentPort = paymentProcessResolver.resolve(attempt.getProvider());

        // Hỏi cổng TRƯỚC khi làm gì khác: dòng còn PENDING chỉ nói rằng chưa ai báo về cho ta, không
        // nói rằng trường chưa trả.
        var remoteStatus = paymentPort.getPaymentLinkStatus(attempt.getProviderOrderRef()).status();

        if (remoteStatus == PaymentLinkRemoteStatus.PAID) {
            throw new IllegalStateException(
                "Đơn hàng này đã được thanh toán, hệ thống đang ghi nhận. Không thể hủy.");
        }

        // Cổng đã xác nhận phiên chết sẵn (trường bấm hủy trên trang cổng, hoặc link tự hết hạn):
        // không cần gọi hủy nữa, chỉ chốt lại dòng của mình cho khớp.
        if (isAlreadyDead(remoteStatus)) {
            markAttemptFailed(order, attempt.getId());
            return;
        }

        // Phiên vẫn sống -> phải đóng được nó ở phía cổng mới dám đóng đơn.
        if (!paymentPort.cancelPaymentLink(attempt.getProviderOrderRef(), CANCELLATION_REASON)) {
            // SePay rơi vào đây MỌI LẦN vì cổng không có API hủy phiên chưa trả -- xem
            // PaymentProcessPort.cancelPaymentLink. Chấp nhận bắt trường đợi hết hạn (tối đa 24
            // tiếng, và hạn đó đã hiện trên đơn) thay vì mở ra ca thu tiền cho một đơn đã đóng.
            throw new IllegalStateException(
                "Đơn hàng đang có một phiên thanh toán chờ xử lý mà cổng không cho hủy sớm. "
                    + "Vui lòng hoàn tất phiên đó hoặc đợi đơn hết hạn.");
        }

        markAttemptFailed(order, attempt.getId());
    }

    /**
     * Đóng dòng thanh toán của đơn đang bị hủy.
     *
     * <p>ĐIỀU GIỮ CHO CHỖ NÀY AN TOÀN là khóa đơn ở {@code execute}, KHÔNG phải lần đọc lại dưới đây.
     * {@code OrderSettlementService.settlePaid} -- đường DUY NHẤT đẩy một lần thử sang PAID -- mở đầu
     * bằng {@code findByIdForUpdate} trên chính đơn này, nên suốt lúc ta còn giữ khóa thì nó nằm chờ,
     * không thể chốt xen vào giữa hai lần gọi mạng ở trên.
     *
     * <p>Lần đọc lại ở đây KHÔNG tự nó chống được gì: {@code findPendingByOrderId} phía trên đã nạp
     * đúng dòng này vào persistence context, nên {@code findById} lấy lại bản trong bộ nhớ chứ không
     * chạm DB -- hai chốt bên dưới vì thế luôn nhìn thấy trạng thái của lần đọc đầu. Giữ lại vì chúng
     * rẻ và vì chúng nói đúng ý định, nhưng ĐỪNG dựa vào chúng: ai thêm một đường ghi payment_records
     * mà không đi qua khóa đơn thì phải khóa lại ở đây, không phải trông vào lần đọc này.
     *
     * <p>({@code failAttempt} có ghi payment_records mà không giữ khóa đơn, nhưng nó chỉ chuyển sang
     * trạng thái thất bại -- trùng đúng thứ ta sắp ghi, nên vô hại.)
     */
    private void markAttemptFailed(Order order, UUID attemptId) {
        var current = paymentRecordRepository.findById(attemptId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lần thanh toán của đơn"));

        if (current.getStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException(
                "Đơn hàng này vừa được thanh toán, hệ thống đang ghi nhận. Không thể hủy.");
        }
        if (current.isSettled()) {
            return;
        }

        current.setStatus(PaymentStatus.FAILED);
        paymentRecordRepository.save(current);
        LOGGER.info("Đóng lần thanh toán {} của đơn {} do trường hủy đơn", attemptId, order.getId());
    }

    /** Cổng đã chốt là phiên này không ra tiền -- không cần hủy thêm. */
    private boolean isAlreadyDead(PaymentLinkRemoteStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case CANCELLED, EXPIRED, FAILED -> true;
            default -> false;
        };
    }
}
