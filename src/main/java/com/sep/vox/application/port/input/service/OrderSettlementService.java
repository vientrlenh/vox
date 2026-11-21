package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.user.Role;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.event.InvoicePaidPayloadV1;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.invoice.Invoice;
import com.sep.vox.domain.model.invoice.InvoiceSourceType;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderItemType;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.OrderItemRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Settles the payment result of ONE order: marks the attempt as paid, closes the order, delivers the goods, issues the
 * invoice.
 *
 * <p>Replaces the old InvoiceSettlementService. The biggest difference is NOT the table name but that the payment
 * ATTEMPT is now separated from the ORDER: previously an invoice was at once the document, the gateway session and the
 * intent to buy, so one failed payment killed all three. Now a failed attempt only closes that one payment_records row,
 * the order stays PENDING and the school can pay again -- see {@link #failAttempt}.
 *
 * <p>Separate from the use case because TWO entry points both need it: the gateway callback
 * (ProcessPaymentCallbackUseCase) and the job that sweeps pending orders, to be written later. Both paths may run
 * CONCURRENTLY on the same order, so every entry here must go through {@code findByIdForUpdate}.
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
    private final SchoolSubscriptionQuotaUserAllocationRepository quotaUserAllocationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private final SchoolDebtNotificationService schoolDebtNotificationService;
    private final SchoolUserRepository schoolUserRepository;
    private final RoleRepository roleRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    /**
     * Cùng con số với {@code DistributeQuotaToUsersService.MAX_ELIGIBLE_USERS_PAGE_SIZE}: hai bên phải
     * nhìn thấy cùng một tập người, nếu không thì người ở rìa giới hạn được chia trần ở màn hình rồi
     * mất nó ở lần gia hạn kế tiếp.
     */
    private static final int MAX_CARRY_FORWARD_USERS = 10_000;

    public OrderSettlementService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRecordRepository paymentRecordRepository,
            InvoiceRepository invoiceRepository,
            SchoolBalanceRepository schoolBalanceRepository,
            SchoolBalanceEntryRepository schoolBalanceEntryRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository quotaRecordRepository,
            SchoolSubscriptionQuotaUserAllocationRepository quotaUserAllocationRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository,
            SchoolDebtNotificationService schoolDebtNotificationService,
            SchoolUserRepository schoolUserRepository,
            RoleRepository roleRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.invoiceRepository = invoiceRepository;
        this.schoolBalanceRepository = schoolBalanceRepository;
        this.schoolBalanceEntryRepository = schoolBalanceEntryRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.quotaRecordRepository = quotaRecordRepository;
        this.quotaUserAllocationRepository = quotaUserAllocationRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
        this.schoolDebtNotificationService = schoolDebtNotificationService;
        this.schoolUserRepository = schoolUserRepository;
        this.roleRepository = roleRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
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
        // Lock the order BEFORE reading its status: two threads both reading "the order is still PENDING" and both going on to
        // credit the money is exactly the scenario that uq_payment_records_one_paid_per_order cannot block, because each thread
        // works on a different payment_records row of the same order.
        Order order = orderRepository.findByIdForUpdate(payment.getOrderId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng của lần thanh toán"));

        if (order.getStatus() == OrderStatus.SUCCESS) {
            LOGGER.info("Đơn {} đã SUCCESS -- bỏ qua lần chốt lặp cho payment {}",
                order.getId(), payment.getId());
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            // Money has arrived for an order that is ALREADY CLOSED. This is NOT a harmless repeated callback: the school has paid but
            // will receive nothing, and the system has no refund flow yet. It must be loud so that someone handles it by hand, instead
            // of quietly returning like the SUCCESS branch above.
            //
            // How this case is reached: the school cancels the order while a SePay session is still alive (the gateway does not allow
            // early cancellation, so CancelOrderUseCase blocks it, but if that block is loosened this is the consequence), or the order
            // expires exactly while the school is paying.
            LOGGER.error(
                "TIỀN VỀ CHO ĐƠN ĐÃ ĐÓNG -- cần hoàn tiền thủ công: orderId={} orderStatus={} paymentId={} amountVnd={}",
                order.getId(), order.getStatus(), payment.getId(), payment.getAmountVnd());
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(paidAt);
        paymentRecordRepository.save(payment);

        order.setStatus(OrderStatus.SUCCESS);
        orderRepository.save(order);

        // Deliver the goods BEFORE issuing the invoice: the invoice is the document for something already delivered. Both are in
        // the same transaction so this order does not change the outcome on failure, but it states the intent correctly for later
        // readers.
        UUID subscriptionId = fulfil(order, paidAt);

        // Only notify when the invoice was REALLY just issued: an empty Optional means this order already has an invoice from an
        // earlier settlement, and a repeated callback must not produce a second mail/notification.
        issueInvoice(order, payment, paidAt)
            .ifPresent(invoice -> publishInvoicePaid(order, invoice, paidAt, subscriptionId));
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
     * Closes an order that is past {@code expires_at} without having collected money.
     *
     * <p>This is the ONLY place allowed to move an order to EXPIRED, and it REFUSES to do so while the order still has a
     * pending attempt: a PENDING row means the gateway still has a session that could bring in money, and closing the order
     * at that moment would put us in the situation where the school has paid but there is no order left to deliver against.
     * The caller must ask the gateway and settle that attempt first (see PendingOrderReconciler).
     *
     * @return true if the order was just moved to EXPIRED
     */
    @Transactional
    public boolean expireIfOverdue(UUID orderId, Instant now) {
        Order order = orderRepository.findByIdForUpdate(orderId)
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
        orderRepository.save(order);
        LOGGER.info("Đóng đơn {} do quá hạn thanh toán ({})", orderId, order.getExpiresAt());
        return true;
    }

    /**
     * @return kỳ đăng ký gắn với lần chốt này, dùng để dựng hóa đơn. Null là hợp lệ: trường chưa có
     *         gói nào vẫn nạp tiền được -- xem {@link #creditBalance}.
     */
    private UUID fulfil(Order order, Instant now) {
        return switch (order.getType()) {
            case TOPUP -> creditBalance(order, now);
            case SUBSCRIPTION_REQUEST, SUBSCRIPTION_UPGRADE -> activateSubscription(order, now);
        };
    }

    /**
     * Cộng vào ví trường số tiền HÀNG, không phải số tiền đã thu: phí dịch vụ là tiền công của
     * mình, cộng cả total vào ví là trả lại phí cho trường dưới dạng số dư tiêu được.
     */
    private UUID creditBalance(Order order, Instant now) {
        // Khóa ví: một trường có thể có nhiều đơn nạp cùng lúc (uq_orders_one_open_subscription_order
        // cố ý KHÔNG chặn TOPUP), nên hai callback về gần nhau sẽ cùng đọc một số dư cũ rồi cùng ghi
        // đè -- mất hẳn một lần nạp mà không có lỗi nào nổi lên.
        SchoolBalance balance = schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(order.getSchoolId(), now);

        BigDecimal credited = order.getSubtotalAmountVnd();
        BigDecimal balanceBefore = balance.getBalanceVnd();
        BigDecimal balanceAfter = balance.apply(credited, now);
        schoolBalanceRepository.save(balance);

        // subscriptionId chỉ để truy vết "lúc nạp trường đang dùng gói nào" -- số dư không thuộc về
        // gói nào cả, và trường chưa có gói vẫn nạp được nên null là hợp lệ.
        UUID activeSubscriptionId = schoolSubscriptionRepository.findActiveBySchoolId(order.getSchoolId())
            .map(s -> s.getId())
            .orElse(null);

        schoolBalanceEntryRepository.save(SchoolBalanceEntry.forTopUp(
            order.getSchoolId(), activeSubscriptionId, order.getId(), credited, balanceAfter, now));

        LOGGER.info("Nạp {} VND vào ví trường {} từ đơn {} -- số dư mới {}",
            credited, order.getSchoolId(), order.getId(), balanceAfter);

        reportDebtClearedIfCrossedBack(order.getSchoolId(), activeSubscriptionId, balanceBefore, balanceAfter, now);

        return activeSubscriptionId;
    }

    /**
     * Lần nạp này vừa kéo số dư từ ÂM về không âm: trường hết nợ và khoá tự mở ngay.
     *
     * <p>Đây là chỗ gọi DUY NHẤT của publishSchoolDebtCleared, và trước V4 nó không tồn tại --
     * hàm phát sự kiện đã viết xong, mẫu email đã có, mà không dòng mã nào gọi tới. Hệ quả: sổ nợ
     * ghi lúc trường rơi vào nợ và lúc vượt trần, rồi im lặng mãi mãi. Với hiệu trưởng, quyển sổ
     * dùng để giải thích "vì sao trường bị khoá" không bao giờ nói trường đã thoát ra.
     *
     * <p>Nạp thêm là đường DUY NHẤT cộng tiền vào ví (REFUND/ADJUSTMENT có trong enum nhưng chưa có
     * factory nào dựng), nên đặt ở đây là phủ hết. Khi hai loại kia có đường ghi, chúng phải gọi lại
     * chính hàm này.
     *
     * <p>So sánh TRƯỚC/SAU của cùng một dòng đang giữ khoá, giống crossedIntoDebt ở chiều ngược lại:
     * chỉ báo đúng một lần lúc CHUYỂN, còn nạp tiếp khi đã hết nợ thì không sinh sự kiện nào.
     */
    private void reportDebtClearedIfCrossedBack(
            UUID schoolId, UUID subscriptionId, BigDecimal balanceBefore, BigDecimal balanceAfter, Instant now) {
        if (balanceBefore.signum() >= 0 || balanceAfter.signum() < 0) {
            return;
        }

        // school_debt_events.subscription_id là NOT NULL, mà nợ lại thuộc về TRƯỜNG chứ không thuộc
        // kỳ đăng ký nào -- một trường trả hết nợ đúng lúc gói vừa hết hạn thì không có id nào để
        // ghi. Bỏ qua dòng sự kiện thay vì để INSERT nổ và cuốn theo cả lần nạp tiền đã thành công.
        if (subscriptionId == null) {
            LOGGER.warn(
                "Trường {} vừa hết nợ (số dư {} -> {}) nhưng không có gói nào đang hoạt động để gắn sự kiện -- bỏ qua dòng CLEARED.",
                schoolId, balanceBefore, balanceAfter);
            return;
        }

        LOGGER.info("Trường {} đã hết nợ: số dư {} -> {}", schoolId, balanceBefore, balanceAfter);
        schoolDebtNotificationService.publishSchoolDebtCleared(subscriptionId, schoolId, now);
    }

    /**
     * Kích hoạt kỳ thuê bao mới và cấp hạn mức kèm gói.
     *
     * <p>Gói lấy từ order_items chứ không phải từ một cột trên orders: đơn đăng ký mang đúng một
     * dòng SUBSCRIPTION và chính dòng đó đã đóng băng đơn giá lúc đặt.
     *
     * <p>Kỳ mới NỐI TIẾP kỳ cũ nếu kỳ cũ còn hạn (gia hạn sớm), chứ không đè lên nó và cũng không
     * đóng nó lại. Hai dòng cùng ACTIVE là trạng thái BÌNH THƯỜNG ở đây -- một dòng đang chạy, một
     * dòng đã trả tiền và xếp hàng phía sau; findActiveBySchoolId lọc theo ngày nên luôn chỉ ra
     * đúng một kỳ đang hiệu lực. Xem SchoolSubscription.activate.
     */
    private UUID activateSubscription(Order order, Instant now) {
        // Chốt kỳ nguồn TRƯỚC mọi thao tác ghi: cutOverToUpgrade sẽ kéo endDate của các kỳ chưa kết
        // thúc về hiện tại, và save() bên dưới thêm một kỳ mới -- hỏi "kỳ gần đây nhất" sau bất kỳ
        // việc nào trong hai việc đó là hỏi về một thế giới đã đổi.
        UUID previousSubscriptionId = schoolSubscriptionRepository
            .findMostRecentBySchoolId(order.getSchoolId())
            .map(previous -> previous.getId())
            .orElse(null);

        UUID planId = orderItemRepository.findByOrderId(order.getId()).stream()
            .filter(item -> item.getType() == OrderItemType.SUBSCRIPTION)
            .map(item -> item.getItemId())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Đơn đăng ký " + order.getId() + " không có dòng SUBSCRIPTION nào để kích hoạt"));

        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ của đơn đăng ký"));

        Instant startsAt = order.getType() == OrderType.SUBSCRIPTION_UPGRADE
            ? cutOverToUpgrade(order.getSchoolId(), now)
            : nextPeriodStart(order.getSchoolId(), now);

        // Giá đã trả lấy từ ĐƠN chứ không đọc lại plan.getPriceVnd(): gói có thể đổi giá trong lúc
        // đơn nằm chờ, mà thứ phải lưu lại là con số trường thật sự trả. Dùng subtotal (giá niêm yết)
        // chứ không phải total: khoản bù nâng cấp là chuyện của LẦN MUA này, không phải giá trị của
        // kỳ -- ghi total thì lần nâng cấp sau lại bù dựa trên một con số đã bị bù một lần rồi.
        SchoolSubscription subscription = schoolSubscriptionRepository.save(SchoolSubscription.activate(
            order.getSchoolId(), plan, order.getSubtotalAmountVnd(), startsAt, now));

        // Gia hạn SỚM: kỳ cũ còn chạy tới startsAt, nên phần tiền tự nạp chưa tiêu của nó CHƯA chốt
        // được -- xem seedQuotaRecords và CarryQuotaFundingAtPeriodStartService.
        Map<QuotaType, BigDecimal> seededPoolTotals = seedQuotaRecords(
            subscription.getId(), planId, previousSubscriptionId, startsAt.isAfter(now));
        carryForwardUserAllocations(
            previousSubscriptionId, subscription.getId(), order.getSchoolId(), seededPoolTotals);

        if (startsAt.isAfter(now)) {
            LOGGER.info("Gia hạn sớm gói {} cho trường {} từ đơn {} -- kỳ mới chạy {} tới {}",
                planId, order.getSchoolId(), order.getId(), startsAt, subscription.getEndDate());
        } else {
            LOGGER.info("Kích hoạt gói {} cho trường {} từ đơn {} -- hạn tới {}",
                planId, order.getSchoolId(), order.getId(), subscription.getEndDate());
        }

        return subscription.getId();
    }

    /**
     * Mốc bắt đầu cho đăng ký mới / gia hạn: nối vào sau kỳ xa nhất còn hạn, không có kỳ nào thì chạy
     * ngay.
     *
     * <p>Lấy theo endDate LỚN NHẤT chứ không phải kỳ đang chạy: trường gia hạn hai lần liên tiếp thì
     * lần sau phải xếp sau lần trước, nếu bám vào kỳ đang chạy thì hai lần gia hạn sẽ trùng nhau và
     * mất một chu kỳ.
     */
    private Instant nextPeriodStart(UUID schoolId, Instant now) {
        return schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now).stream()
            .findFirst()
            .map(s -> s.getEndDate())
            .orElse(now);
    }

    /**
     * Nâng cấp: gói mới có hiệu lực NGAY, nên mọi kỳ chưa kết thúc của trường bị đóng lại tại đây.
     *
     * <p>Đóng bằng cách kéo endDate về {@code now} chứ không chỉ đổi status: hai truy vấn phân biệt
     * "kỳ nào đang chạy" đều soi khoảng ngày (findInForceBySchoolId) hoặc endDate
     * (findUnfinishedBySchoolId), nên một dòng EXPIRED mà endDate vẫn ở tương lai sẽ tiếp tục
     * được tính là "kỳ chưa kết thúc" ở chỗ thứ hai -- và lần gia hạn kế tiếp sẽ xếp hàng sau một kỳ
     * đã chết. Kéo endDate về hiện tại cũng là ghi đúng sự thật: kỳ đó kết thúc thật vào lúc này.
     *
     * <p>Đóng TẤT CẢ chứ không chỉ kỳ đang chạy: trường có thể vừa gia hạn (kỳ tương lai đang xếp
     * hàng) rồi đổi ý nâng cấp. Bỏ sót kỳ xếp hàng thì sau khi gói mới hết hạn, trường tụt về đúng
     * gói cũ mà họ vừa trả tiền để rời khỏi.
     *
     * <p>Hạn mức của các kỳ bị đóng KHÔNG được chuyển sang kỳ mới: phần chưa dùng đã được quy ra
     * tiền và trừ vào chính đơn nâng cấp này (xem SubscriptionUpgradePolicyService), cộng thêm
     * lần nữa là bù hai lần cho cùng một thứ.
     */
    private Instant cutOverToUpgrade(UUID schoolId, Instant now) {
        List<SchoolSubscription> unfinished = schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now);
        for (SchoolSubscription previous : unfinished) {
            previous.setStatus(SchoolSubscriptionStatus.EXPIRED);
            previous.setEndDate(now);
            schoolSubscriptionRepository.save(previous);
        }
        if (!unfinished.isEmpty()) {
            LOGGER.info("Nâng cấp trường {}: đóng {} kỳ chưa kết thúc tại {}", schoolId, unfinished.size(), now);
        }
        return now;
    }

    /**
     * Chép định mức của gói thành bộ đếm của kỳ này.
     *
     * <p>Phải CHÉP chứ không đọc thẳng từ plan mỗi lần dùng: định mức gói có thể đổi giữa chừng, mà
     * kỳ đang chạy phải giữ nguyên số đã bán. Đây cũng là chỗ used_amount_vnd bắt đầu lại từ 0 cho
     * kỳ mới -- còn số dư ví tự nạp thì không đụng tới, nó nằm ở school_balances và sống xuyên kỳ.
     *
     * <p><b>Tiền trường đã nạp VÀO ví hạn mức thì phải mang sang.</b> Nó không nằm ở school_balances
     * nữa (đã rời ví khi nạp, kèm bút toán QUOTA_FUNDING) mà nằm ngay trong bản ghi bị dựng lại ở đây
     * -- nên không mang sang là XOÁ TIỀN THẬT của trường vào đúng ngày họ gia hạn. Khác hẳn ca mất
     * trần chi cá nhân: trần chi thì quản trị viên chia lại được, tiền thì không.
     *
     * <p>Chỉ mang phần CHƯA TIÊU, theo quy ước "tiền gói tiêu trước, tiền tự nạp tiêu sau" -- xem
     * {@link SchoolSubscriptionQuotaRecord#unspentFundedVnd()}.
     *
     * <p><b>Kỳ TƯƠNG LAI thì chỉ ĐẶT HẸN, không mang sang.</b> Gia hạn sớm là hai mốc khác nhau: kỳ cũ
     * còn chạy tới {@code startsAt}, còn tiêu được và còn nạp thêm được, nên phần chưa tiêu của nó
     * chốt tại đây là chốt một con số sẽ sai -- tiêu nốt thì tiền được tiêu hai lần, nạp thêm thì
     * khoản mới bốc hơi ở ranh giới. Ví được seed đúng định mức gói cộng một cái hẹn, và
     * {@link CarryQuotaFundingAtPeriodStartService} chốt con số thật vào lúc kỳ mới bắt đầu. Xem V13.
     *
     * @param deferFundingCarry kỳ mới chưa tới ngày chạy (gia hạn sớm)
     * @return ví của kỳ MỚI theo từng loại, kèm trần của nó. {@link #carryForwardUserAllocations} cần
     *         cả hai: tập khoá nói loại ví nào thật sự tồn tại ở kỳ mới, còn giá trị là mức kẹp cho
     *         tổng trần cá nhân chép sang.
     */
    private Map<QuotaType, BigDecimal> seedQuotaRecords(UUID subscriptionId, UUID planId,
            UUID previousSubscriptionId, boolean deferFundingCarry) {
        // KHÔNG đọc ví kỳ cũ ở nhánh hoãn: đọc là chốt, và chốt sớm chính là lỗi đang sửa.
        Map<QuotaType, BigDecimal> carriedByType = deferFundingCarry
            ? Map.<QuotaType, BigDecimal>of()
            : unspentFundingByQuotaType(previousSubscriptionId);

        List<SchoolSubscriptionQuotaRecord> records = subscriptionPlanQuotaRepository
            .findBySubscriptionPlanId(planId).stream()
            .map(planQuota -> deferFundingCarry && previousSubscriptionId != null
                ? SchoolSubscriptionQuotaRecord.seededPendingCarry(
                    subscriptionId,
                    planQuota.getQuotaType(),
                    planQuota.getIncludedAmountVnd(),
                    previousSubscriptionId)
                : SchoolSubscriptionQuotaRecord.seeded(
                    subscriptionId,
                    planQuota.getQuotaType(),
                    planQuota.getIncludedAmountVnd(),
                    carriedByType.getOrDefault(planQuota.getQuotaType(), BigDecimal.ZERO)))
            .toList();

        records.forEach(quotaRecordRepository::save);

        records.stream()
            .filter(record -> record.getFundedFromBalanceVnd().signum() > 0)
            .forEach(record -> LOGGER.info("Chuyển {}đ tiền tự nạp chưa tiêu của ví {} sang kỳ {}",
                record.getFundedFromBalanceVnd(), record.getQuotaType(), subscriptionId));

        Map<QuotaType, BigDecimal> seededPoolTotals = new EnumMap<QuotaType, BigDecimal>(QuotaType.class);
        records.forEach(record ->
            seededPoolTotals.put(record.getQuotaType(), record.getTotalAllocatedAmountVnd()));

        carriedByType.forEach((quotaType, amountVnd) -> {
            if (!seededPoolTotals.containsKey(quotaType)) {
                LOGGER.warn(
                    "Kỳ {} không có ví {} nên {}đ tiền tự nạp chưa tiêu của kỳ {} không mang sang được"
                        + " -- gói mới không còn loại hạn mức này.",
                    subscriptionId, quotaType, amountVnd, previousSubscriptionId);
            }
        });

        return seededPoolTotals;
    }

    /**
     * Phần tiền tự nạp còn chưa tiêu của kỳ cũ, theo từng loại ví.
     *
     * <p>Gói mới có thể KHÔNG còn loại ví mà kỳ cũ có (nhà trường đổi sang gói không kèm PRACTICE
     * chẳng hạn). Khi đó tiền của loại đó không có chỗ nào để mang sang và nằm lại ở kỳ cũ -- một ca
     * hiếm và chưa có đường xử lý, nên ghi log WARN để lộ ra thay vì lặng lẽ bốc hơi.
     */
    private Map<QuotaType, BigDecimal> unspentFundingByQuotaType(UUID previousSubscriptionId) {
        if (previousSubscriptionId == null) {
            return Map.of();
        }
        Map<QuotaType, BigDecimal> carried = new EnumMap<QuotaType, BigDecimal>(QuotaType.class);
        for (SchoolSubscriptionQuotaRecord record : quotaRecordRepository.findBySchoolSubscriptionId(previousSubscriptionId)) {
            BigDecimal unspent = record.unspentFundedVnd();
            if (unspent.signum() > 0) {
                carried.put(record.getQuotaType(), unspent);
            }
        }
        return carried;
    }

    /**
     * Copies the PERSONAL spending ceilings of the old period to the new period.
     *
     * <p>The allocation record is attached to {@code school_subscription_id}, and every period is a new subscription
     * row, so without copying, every personal ceiling DISAPPEARS exactly when the new period starts -- and the two wallets break
     * in two opposite directions, equally silently:
     *
     * <ul>
     *   <li>PRACTICE: {@code findPracticeSpendableFundsVnd} takes LEAST with COALESCE(...,0), so a missing
     *       row means 0 -- THE WHOLE SCHOOL loses the right to practice speaking until someone goes in to redistribute,
     *       right after the school has just paid for the renewal.</li>
     *   <li>ASSESSMENT: {@code ClassTestTokenQuotaGuardService.remainingUserAllocation} returns null when there is no
     *       row, and the guard skips null -- every teacher ends up with no spending ceiling at all.</li>
     * </ul>
     *
     * <p>Copies the CEILING, NOT what has been spent: {@code upsertAllocation} builds the new row with
     * {@code used = 0}, exactly the way {@link #seedQuotaRecords} restarts the school-level wallet from 0.
     * A new period is a new budget, at both levels.
     *
     * <p><b>The snapshot is taken at PAYMENT time, not when the new period starts running.</b> With an early renewal the two
     * moments are far apart and any allocation change in between will not carry over. This is accepted on purpose:
     * following the start moment exactly would need another job that sweeps periods coming due, while the cost of the mismatch
     * is only a few stale numbers that the school administrator can fix at any time -- whereas the cost of
     * copying nothing is the whole school freezing.
     *
     * <p><b>The copy is FILTERED and CLAMPED</b>, not copied verbatim. Three conditions, each closing a bug that the
     * first verbatim copy left open:
     *
     * <ul>
     *   <li>Only wallet types that the NEW period really has -- iterating {@code QuotaType.values()} and upserting
     *       unconditionally would give a school that switched to a plan without PRACTICE a batch of ceiling rows pointing at
     *       a wallet that does not exist, whereas {@link #seedQuotaRecords} already handles the symmetric case on the
     *       MONEY side with a WARN line.</li>
     *   <li>Only people who are STILL eligible -- allocation rows are not deleted when a teacher leaves or a
     *       student graduates, so copying everything would rebuild the whole staffing history every period plus the newly departed.
     *       With no way to delete, the table only bloats and {@code orphanedAmountVnd}
     *       on the screen only grows. Same filter as {@code sumAllocatedForEligibleUsers}: if the two
     *       sides count different sets we are back to exactly bug Q-5.</li>
     *   <li>The copied total does not exceed the new period's wallet -- after a downgrade (or the end of a promotion) the total of the old ceilings
     *       may be larger than the whole new wallet, and the school would open the period already over its own ceiling:
     *       every manual edit afterwards is rejected with "exceeds the distributable part" even though nobody lowered the
     *       ceiling. Scale down PROPORTIONALLY to keep the ratios between individuals.</li>
     * </ul>
     *
     * <p>The clamp here is the wallet ceiling ({@code total_allocated}), not the distributable part
     * ({@code SchoolQuotaPolicy.distributableAmountOf}, which is stricter because it also multiplies by the school's ratio).
     * That policy sits in a repository this service does not hold; clamping to the wallet is enough so that the new period does not open
     * in a broken state, and the school administrator can fix the rest on the screen.
     */
    private void carryForwardUserAllocations(UUID previousSubscriptionId, UUID newSubscriptionId,
            UUID schoolId, Map<QuotaType, BigDecimal> seededPoolTotals) {
        if (previousSubscriptionId == null) {
            return;
        }

        for (Map.Entry<QuotaType, BigDecimal> seededPool : seededPoolTotals.entrySet()) {
            QuotaType quotaType = seededPool.getKey();
            BigDecimal poolTotalVnd = orZero(seededPool.getValue());

            List<SchoolSubscriptionQuotaUserAllocation> carried = quotaUserAllocationRepository
                .findBySchoolSubscriptionIdAndQuotaType(previousSubscriptionId, quotaType);
            if (carried.isEmpty()) {
                continue;
            }

            Optional<Set<UUID>> eligible = eligibleUserIds(schoolId, quotaType);
            List<SchoolSubscriptionQuotaUserAllocation> kept = eligible
                .map(userIds -> carried.stream()
                    .filter(allocation -> userIds.contains(allocation.getUserId()))
                    .toList())
                .orElse(carried);
            if (kept.size() < carried.size()) {
                LOGGER.info("Bỏ {} dòng trần {} của người không còn đủ điều kiện -- không mang sang kỳ {}",
                    carried.size() - kept.size(), quotaType, newSubscriptionId);
            }

            BigDecimal scale = carryScaleFor(kept, poolTotalVnd, quotaType, previousSubscriptionId, newSubscriptionId);
            for (SchoolSubscriptionQuotaUserAllocation allocation : kept) {
                quotaUserAllocationRepository.upsertAllocation(newSubscriptionId, quotaType,
                    allocation.getUserId(), scaled(orZero(allocation.getAllocatedAmountVnd()), scale));
            }

            if (!kept.isEmpty()) {
                LOGGER.info("Chuyển {} trần chi cá nhân {} từ kỳ {} sang kỳ {}",
                    kept.size(), quotaType, previousSubscriptionId, newSubscriptionId);
            }
        }
    }

    /**
     * Tập người CÒN đủ điều kiện nhận trần của ví này -- đang ACTIVE, còn thuộc trường, còn đúng vai
     * trò. Cùng ba điều kiện mà {@code sumAllocatedForEligibleUsers} dùng để tính "đã chia", và cùng
     * lời gọi mà {@code DistributeQuotaToUsersService.fetchEligibleUserIds} dùng để dựng danh sách.
     *
     * <p><b>Optional rỗng nghĩa là KHÔNG XÁC ĐỊNH ĐƯỢC, và chỗ gọi phải chép TẤT CẢ.</b> Hai lối
     * hỏng ở đây đều dẫn tới một tập thiếu người chứ không phải một tập rỗng rõ ràng, mà lọc theo một
     * tập thiếu là LẤY MẤT trần của người thật -- đúng thảm hoạ Q-2, chỉ khác là lần này do chính
     * đoạn sửa Q-2 gây ra. Giữ nhầm một dòng đáng bỏ chỉ làm bảng phình thêm một kỳ; hai cái giá đó
     * không cùng hạng.
     */
    private Optional<Set<UUID>> eligibleUserIds(UUID schoolId, QuotaType quotaType) {
        String roleCode = quotaType.allocationRoleCode();
        Optional<Role> role = roleRepository.findByCode(roleCode);
        if (role.isEmpty()) {
            LOGGER.error("Không tìm thấy vai trò {} -- chép TOÀN BỘ trần {} sang kỳ mới thay vì lọc,"
                + " để không lấy mất trần của người còn đang dạy/học.", roleCode, quotaType);
            return Optional.empty();
        }

        PageResult<SchoolUser> page = schoolUserRepository.findBySchoolId(schoolId, null, role.get().getId(),
            UserStatus.ACTIVE.name(), null, false, 1, MAX_CARRY_FORWARD_USERS);
        if (page.totalElements() > page.content().size()) {
            LOGGER.error("Trường {} có {} người đủ điều kiện nhận trần {} nhưng chỉ đọc được {} --"
                + " chép TOÀN BỘ thay vì lọc, vì lọc theo một tập thiếu sẽ xoá trần của phần dôi ra.",
                schoolId, page.totalElements(), quotaType, page.content().size());
            return Optional.empty();
        }

        return Optional.of(page.content().stream()
            .map(schoolUser -> schoolUser.getUserId())
            .collect(Collectors.toSet()));
    }

    /**
     * Tỉ lệ thu nhỏ để tổng trần chép sang vừa ví của kỳ mới. {@code null} = không phải thu nhỏ.
     *
     * <p>Làm tròn XUỐNG nên tổng sau khi nhân luôn nhỏ hơn hoặc bằng trần ví -- thà thiếu vài đồng
     * còn hơn để kỳ mới mở ra đã vượt trần, đúng thứ phép kẹp này sinh ra để chặn.
     */
    private BigDecimal carryScaleFor(List<SchoolSubscriptionQuotaUserAllocation> kept,
            BigDecimal poolTotalVnd, QuotaType quotaType, UUID previousSubscriptionId, UUID newSubscriptionId) {
        BigDecimal totalCarriedVnd = kept.stream()
            .map(allocation -> orZero(allocation.getAllocatedAmountVnd()))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        if (totalCarriedVnd.signum() <= 0 || totalCarriedVnd.compareTo(poolTotalVnd) <= 0) {
            return null;
        }

        LOGGER.warn("Trần cá nhân {} của kỳ {} cộng lại {}đ, lớn hơn ví {}đ của kỳ {} -- thu nhỏ theo"
            + " tỉ lệ để kỳ mới không mở ra trong trạng thái đã vượt trần.",
            quotaType, previousSubscriptionId, totalCarriedVnd, poolTotalVnd, newSubscriptionId);
        return poolTotalVnd.divide(totalCarriedVnd, 10, RoundingMode.DOWN);
    }

    private static BigDecimal scaled(BigDecimal amountVnd, BigDecimal scale) {
        return scale == null ? amountVnd : amountVnd.multiply(scale).setScale(0, RoundingMode.DOWN);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Hóa đơn chỉ phát SAU khi tiền đã về -- đây là thay đổi nghiệp vụ chính so với model cũ, nơi
     * invoice được tạo lúc phát link và mang luôn trạng thái thanh toán.
     *
     * <p>Kiểm tra tồn tại trước khi phát: guard trạng thái đơn ở trên đã chặn hầu hết đường lặp,
     * nhưng hóa đơn là chứng từ đối ngoại nên đáng trả thêm một truy vấn để chắc chắn không phát
     * hai số cho cùng một đơn.
     *
     * @return hóa đơn vừa phát, hoặc rỗng khi đơn đã có hóa đơn từ lần chốt trước. Phía gọi dùng
     *         đúng dấu hiệu này để quyết định có báo cho trường hay không.
     */
    private Optional<Invoice> issueInvoice(Order order, PaymentRecord payment, Instant now) {
        if (invoiceRepository.existsByOrderId(order.getId())) {
            return Optional.empty();
        }
        Invoice invoice = invoiceRepository.save(Invoice.issueFor(order.getId(), payment.getId(), now));
        LOGGER.info("Phát hành hóa đơn {} cho đơn {}", invoice.getInvoiceNumber(), order.getId());

        return Optional.of(invoice);
    }

    /**
     * Báo cho mọi SCHOOL_ADMIN của trường rằng hóa đơn đã thu tiền: một mail chứng từ
     * (InvoiceEmailConsumer) và một thông báo trong app (NotificationPushedEventConsumer).
     *
     * <p>FIX: cả hai consumer, mẫu mail, ánh xạ category/target và định tuyến topic đều đã tồn tại
     * từ trước, nhưng KHÔNG dòng mã nào ghi outbox {@code InvoicePaid} -- lần tách
     * {@code InvoiceSettlementService} thành lớp này đã đánh rơi lời gọi
     * {@code publishInvoicePaid} (dấu vết còn lại là javadoc của SchoolDebtNotificationService,
     * vẫn trỏ tới một method không còn tồn tại). Hệ quả: trường trả tiền xong không nhận được gì
     * cả, và vì không có lỗi nào nổi lên nên chỉ lộ ra khi đọc chéo publisher với consumer.
     *
     * <p>Nằm trong CÙNG transaction với việc đóng đơn và phát hóa đơn, đúng như mọi publisher khác
     * trong luồng này: chứng từ và thông báo về nó cùng sống hoặc cùng chết. Đặt sau
     * {@code existsByOrderId} nên một callback lặp không phát sự kiện lần hai.
     */
    private void publishInvoicePaid(Order order, Invoice invoice, Instant now, UUID subscriptionId) {
        List<UUID> schoolAdminIds = schoolUserRepository
            .findBySchoolIdWithRole(order.getSchoolId(), RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream()
            .map(schoolUser -> schoolUser.getUserId())
            .toList();

        // Chốt danh sách người nhận NGAY LÚC phát, không để consumer truy vấn lại -- giống hệt các
        // event nợ hạn mức. Nhờ vậy mỗi lần chạy lại (retry, replay từ DLT) đều ra đúng tập người
        // nhận cũ và uk_notifications_user_event chặn được trùng.
        String payload = jsonSerializationPort.toJson(new InvoicePaidPayloadV1(
            schoolAdminIds,
            order.getSchoolId(),
            subscriptionId,
            // sourceId = id ĐƠN. Hóa đơn luôn được phát cho một đơn (Invoice.issueFor), nên đó là
            // nguồn duy nhất luôn tồn tại; đây cũng là khoá để thông báo mở đúng trang chi tiết đơn.
            order.getId(),
            invoice.getInvoiceNumber(),
            order.getTotalAmountVnd(),
            now,
            sourceTypeOf(order.getType())
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.INVOICE, invoice.getId(),
            EventTypeConstant.INVOICE_PAID, payload, now
        ));
    }

    /**
     * Loại đơn -> loại nguồn hóa đơn. Hai enum tách nhau vì {@code InvoiceSourceType} mô tả thứ
     * đang được mua trên chứng từ, còn {@code OrderType} mô tả cách đơn được tạo.
     *
     * <p>Đăng ký mới và nâng cấp cùng ra {@code SUBSCRIPTION}: trên chứng từ cả hai đều là tiền
     * mua một kỳ gói, khác nhau ở đường tạo đơn chứ không ở thứ được bán. Phân biệt hai đường đó
     * là việc của {@code OrderType}, đã nằm sẵn trên đơn.
     */
    private InvoiceSourceType sourceTypeOf(OrderType orderType) {
        return switch (orderType) {
            case TOPUP -> InvoiceSourceType.TOPUP;
            case SUBSCRIPTION_REQUEST -> InvoiceSourceType.SUBSCRIPTION;
            case SUBSCRIPTION_UPGRADE -> InvoiceSourceType.SUBSCRIPTION;
        };
    }
}
