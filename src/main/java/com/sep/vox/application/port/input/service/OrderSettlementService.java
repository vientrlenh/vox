package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.invoice.Invoice;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderItemType;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.OrderItemRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

import java.math.BigDecimal;

/**
 * Chốt kết quả thanh toán của MỘT đơn: đánh dấu lần thử đã trả, đóng đơn, giao hàng, phát hóa đơn.
 *
 * <p>Thay cho InvoiceSettlementService cũ. Khác biệt lớn nhất KHÔNG phải ở tên bảng mà ở chỗ lần
 * THỬ thanh toán đã tách khỏi ĐƠN: trước đây một invoice vừa là chứng từ vừa là phiên cổng vừa là ý
 * định mua, nên một lần trả hụt là chết cả ba. Giờ một lần thử hỏng chỉ đóng đúng dòng
 * payment_records đó, đơn vẫn PENDING và trường bấm trả lại được -- xem {@link #failAttempt}.
 *
 * <p>Tách khỏi use case vì có HAI đường vào cùng cần nó: callback của cổng
 * (ProcessPaymentCallbackUseCase) và job quét đơn treo sẽ viết sau. Cả hai đường đó có thể chạy
 * ĐỒNG THỜI trên cùng một đơn, nên mọi lối vào đây đều phải đi qua {@code findByIdForUpdate}.
 */
@Service
public class OrderSettlementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSettlementService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;
    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    public OrderSettlementService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRecordRepository paymentRecordRepository,
            InvoiceRepository invoiceRepository,
            SchoolBalanceRepository schoolBalanceRepository,
            SchoolBalanceEntryRepository schoolBalanceEntryRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository quotaRecordRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.invoiceRepository = invoiceRepository;
        this.schoolBalanceRepository = schoolBalanceRepository;
        this.schoolBalanceEntryRepository = schoolBalanceEntryRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.quotaRecordRepository = quotaRecordRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
    }

    /**
     * Tiền ĐÃ VỀ: đóng lần thử, đóng đơn, giao hàng, phát hóa đơn.
     *
     * <p>IDEMPOTENT theo trạng thái đơn. Bắt buộc phải vậy: cả hai cổng đều gửi lại khi không nhận
     * được phản hồi thành công, và job quét đơn treo (PendingOrderReconciler) có thể chạy đúng lúc
     * webhook về. Không có chốt này thì một đơn nạp tiền được cộng số dư nhiều lần.
     */
    @Transactional
    public void settlePaid(PaymentRecord payment, Instant paidAt) {
        // Khóa đơn TRƯỚC khi đọc trạng thái: hai luồng cùng đọc "đơn còn PENDING" rồi cùng đi cộng
        // tiền là đúng kịch bản mà uq_payment_records_one_paid_per_order không chặn được, vì mỗi
        // luồng thao tác trên một dòng payment_records khác nhau của cùng một đơn.
        var order = orderRepository.findByIdForUpdate(payment.getOrderId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng của lần thanh toán"));

        if (order.getStatus() != OrderStatus.PENDING) {
            LOGGER.info("Đơn {} đã ở trạng thái {} -- bỏ qua lần chốt lặp cho payment {}",
                order.getId(), order.getStatus(), payment.getId());
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(paidAt);
        paymentRecordRepository.save(payment);

        order.setStatus(OrderStatus.SUCCESS);
        order.setUpdatedAt(paidAt);
        orderRepository.save(order);

        // Giao hàng TRƯỚC khi phát hóa đơn: hóa đơn là chứng từ cho thứ đã giao. Cùng transaction
        // nên thứ tự này không đổi kết quả khi lỗi, nhưng nó nói đúng ý định cho người đọc sau.
        fulfil(order, paidAt);

        issueInvoice(order, payment, paidAt);
    }

    /**
     * Cổng đã xác nhận lần thử này KHÔNG ra tiền (hủy/hết hạn/thất bại).
     *
     * <p>CHỈ đóng dòng payment_records, KHÔNG đụng tới đơn: đơn vẫn PENDING nên trường bấm thanh
     * toán lại được ngay, và ràng buộc uq_payment_records_one_pending_per_order lúc đó đã thông vì
     * dòng cũ không còn PENDING. Đóng luôn đơn ở đây là bắt trường đặt lại đơn mới chỉ vì họ bấm
     * nhầm nút hủy trên trang cổng.
     *
     * <p>Đơn hết hạn thật thì đã có expires_at + job quét lo, không phải việc của callback.
     */
    @Transactional
    public void failAttempt(PaymentRecord payment, PaymentStatus failureStatus) {
        if (payment.isSettled()) {
            return;
        }
        payment.setStatus(failureStatus);
        paymentRecordRepository.save(payment);
    }

    /**
     * Đóng một đơn đã quá {@code expires_at} mà chưa thu được tiền.
     *
     * <p>Đây là chỗ DUY NHẤT được phép đưa đơn sang EXPIRED, và nó TỪ CHỐI làm việc đó khi đơn còn
     * một lần thử đang treo: dòng PENDING nghĩa là bên cổng vẫn còn một phiên có thể ra tiền, và
     * đóng đơn lúc đó là tự đặt mình vào cảnh trường trả xong nhưng không còn đơn nào để giao hàng.
     * Người gọi phải đi hỏi cổng và chốt lần thử đó trước (xem PendingOrderReconciler).
     *
     * @return true nếu đơn vừa được chuyển sang EXPIRED
     */
    @Transactional
    public boolean expireIfOverdue(UUID orderId, Instant now) {
        var order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng cần hết hạn"));

        if (order.getStatus() != OrderStatus.PENDING || now.isBefore(order.getExpiresAt())) {
            return false;
        }
        if (paymentRecordRepository.findPendingByOrderId(orderId).isPresent()) {
            LOGGER.warn("Đơn {} đã quá hạn nhưng còn lần thử đang treo -- chưa đóng, đợi đối soát xong",
                orderId);
            return false;
        }

        order.setStatus(OrderStatus.EXPIRED);
        order.setUpdatedAt(now);
        orderRepository.save(order);
        LOGGER.info("Đóng đơn {} do quá hạn thanh toán ({})", orderId, order.getExpiresAt());
        return true;
    }

    private void fulfil(Order order, Instant now) {
        switch (order.getType()) {
            case TOPUP -> creditBalance(order, now);
            case SUBSCRIPTION_REQUEST, SUBSCRIPTION_UPGRADE -> activateSubscription(order, now);
        }
    }

    /**
     * Cộng vào ví trường số tiền HÀNG, không phải số tiền đã thu: phí dịch vụ là tiền công của
     * mình, cộng cả total vào ví là trả lại phí cho trường dưới dạng số dư tiêu được.
     */
    private void creditBalance(Order order, Instant now) {
        // Khóa ví: một trường có thể có nhiều đơn nạp cùng lúc (uq_orders_one_open_subscription_order
        // cố ý KHÔNG chặn TOPUP), nên hai callback về gần nhau sẽ cùng đọc một số dư cũ rồi cùng ghi
        // đè -- mất hẳn một lần nạp mà không có lỗi nào nổi lên.
        var balance = schoolBalanceRepository.findBySchoolIdForUpdate(order.getSchoolId())
            .orElseGet(() -> schoolBalanceRepository.save(
                SchoolBalance.emptyFor(order.getSchoolId(), now)));

        var credited = order.getSubtotalAmountVnd();
        var balanceAfter = balance.apply(credited, now);
        schoolBalanceRepository.save(balance);

        // subscriptionId chỉ để truy vết "lúc nạp trường đang dùng gói nào" -- số dư không thuộc về
        // gói nào cả, và trường chưa có gói vẫn nạp được nên null là hợp lệ.
        var activeSubscriptionId = schoolSubscriptionRepository.findActiveBySchoolId(order.getSchoolId())
            .map(s -> s.getId())
            .orElse(null);

        schoolBalanceEntryRepository.save(SchoolBalanceEntry.forTopUp(
            order.getSchoolId(), activeSubscriptionId, order.getId(), credited, balanceAfter, now));

        LOGGER.info("Nạp {} VND vào ví trường {} từ đơn {} -- số dư mới {}",
            credited, order.getSchoolId(), order.getId(), balanceAfter);
    }

    /**
     * Kích hoạt kỳ thuê bao mới và cấp hạn mức kèm gói.
     *
     * <p>Gói lấy từ order_items chứ không phải từ một cột trên orders: đơn đăng ký mang đúng một
     * dòng SUBSCRIPTION và chính dòng đó đã đóng băng đơn giá lúc đặt.
     */
    private void activateSubscription(Order order, Instant now) {
        var planId = orderItemRepository.findByOrderId(order.getId()).stream()
            .filter(item -> item.getType() == OrderItemType.SUBSCRIPTION)
            .map(item -> item.getItemId())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Đơn đăng ký " + order.getId() + " không có dòng SUBSCRIPTION nào để kích hoạt"));

        var plan = subscriptionPlanRepository.findById(planId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ của đơn đăng ký"));

        // Giá đã trả lấy từ ĐƠN chứ không đọc lại plan.getPriceVnd(): gói có thể đổi giá trong lúc
        // đơn nằm chờ, mà thứ phải lưu lại là con số trường thật sự trả.
        var subscription = schoolSubscriptionRepository.save(
            SchoolSubscription.activate(order.getSchoolId(), plan, order.getSubtotalAmountVnd(), now));

        seedQuotaRecords(subscription.getId(), planId);

        LOGGER.info("Kích hoạt gói {} cho trường {} từ đơn {} -- hạn tới {}",
            planId, order.getSchoolId(), order.getId(), subscription.getEndDate());
    }

    /**
     * Chép định mức của gói thành bộ đếm của kỳ này.
     *
     * <p>Phải CHÉP chứ không đọc thẳng từ plan mỗi lần dùng: định mức gói có thể đổi giữa chừng, mà
     * kỳ đang chạy phải giữ nguyên số đã bán. Đây cũng là chỗ used_amount_vnd bắt đầu lại từ 0 cho
     * kỳ mới -- còn tiền TỰ NẠP thì không đụng tới, nó nằm ở school_balances và sống xuyên kỳ.
     */
    private void seedQuotaRecords(UUID subscriptionId, UUID planId) {
        List<SchoolSubscriptionQuotaRecord> records = subscriptionPlanQuotaRepository
            .findBySubscriptionPlanId(planId).stream()
            .map(planQuota -> new SchoolSubscriptionQuotaRecord(
                subscriptionId, planQuota.getQuotaType(), planQuota.getIncludedAmountVnd(), BigDecimal.ZERO))
            .toList();

        records.forEach(quotaRecordRepository::save);
    }

    /**
     * Hóa đơn chỉ phát SAU khi tiền đã về -- đây là thay đổi nghiệp vụ chính so với model cũ, nơi
     * invoice được tạo lúc phát link và mang luôn trạng thái thanh toán.
     *
     * <p>Kiểm tra tồn tại trước khi phát: guard trạng thái đơn ở trên đã chặn hầu hết đường lặp,
     * nhưng hóa đơn là chứng từ đối ngoại nên đáng trả thêm một truy vấn để chắc chắn không phát
     * hai số cho cùng một đơn.
     */
    private void issueInvoice(Order order, PaymentRecord payment, Instant now) {
        if (invoiceRepository.existsByOrderId(order.getId())) {
            return;
        }
        var invoice = invoiceRepository.save(Invoice.issueFor(order.getId(), payment.getId(), now));
        LOGGER.info("Phát hành hóa đơn {} cho đơn {}", invoice.getInvoiceNumber(), order.getId());
    }
}
