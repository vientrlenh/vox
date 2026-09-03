package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import com.sep.vox.domain.repository.InvoiceRepository;
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
    private final SchoolSubscriptionQuotaUserAllocationRepository quotaUserAllocationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private final SchoolDebtNotificationService schoolDebtNotificationService;
    private final SchoolUserRepository schoolUserRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

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
        // Khóa đơn TRƯỚC khi đọc trạng thái: hai luồng cùng đọc "đơn còn PENDING" rồi cùng đi cộng
        // tiền là đúng kịch bản mà uq_payment_records_one_paid_per_order không chặn được, vì mỗi
        // luồng thao tác trên một dòng payment_records khác nhau của cùng một đơn.
        var order = orderRepository.findByIdForUpdate(payment.getOrderId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng của lần thanh toán"));

        if (order.getStatus() == OrderStatus.SUCCESS) {
            LOGGER.info("Đơn {} đã SUCCESS -- bỏ qua lần chốt lặp cho payment {}",
                order.getId(), payment.getId());
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            // Tiền về cho một đơn ĐÃ ĐÓNG. Đây KHÔNG phải callback lặp vô hại: trường đã trả nhưng
            // sẽ không nhận được gì, và hệ thống chưa có luồng hoàn tiền nào. Phải kêu to để có
            // người xử lý tay, thay vì lặng lẽ return như nhánh SUCCESS ở trên.
            //
            // Đường vào ca này: trường hủy đơn trong lúc còn phiên SePay sống (cổng không cho hủy
            // sớm nên CancelOrderUseCase chặn, nhưng nếu chặn đó bị nới thì đây là hậu quả), hoặc
            // đơn hết hạn đúng lúc trường đang trả.
            LOGGER.error(
                "TIỀN VỀ CHO ĐƠN ĐÃ ĐÓNG -- cần hoàn tiền thủ công: orderId={} orderStatus={} paymentId={} amountVnd={}",
                order.getId(), order.getStatus(), payment.getId(), payment.getAmountVnd());
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
        var subscriptionId = fulfil(order, paidAt);

        // Chỉ báo khi hóa đơn THẬT SỰ vừa được phát: Optional rỗng nghĩa là đơn này đã có hóa đơn
        // từ một lần chốt trước, và một callback lặp không được sinh mail/thông báo lần hai.
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
        var balance = schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(order.getSchoolId(), now);

        var credited = order.getSubtotalAmountVnd();
        var balanceBefore = balance.getBalanceVnd();
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
        var previousSubscriptionId = schoolSubscriptionRepository
            .findMostRecentBySchoolId(order.getSchoolId())
            .map(previous -> previous.getId())
            .orElse(null);

        var planId = orderItemRepository.findByOrderId(order.getId()).stream()
            .filter(item -> item.getType() == OrderItemType.SUBSCRIPTION)
            .map(item -> item.getItemId())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Đơn đăng ký " + order.getId() + " không có dòng SUBSCRIPTION nào để kích hoạt"));

        var plan = subscriptionPlanRepository.findById(planId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ của đơn đăng ký"));

        var startsAt = order.getType() == OrderType.SUBSCRIPTION_UPGRADE
            ? cutOverToUpgrade(order.getSchoolId(), now)
            : nextPeriodStart(order.getSchoolId(), now);

        // Giá đã trả lấy từ ĐƠN chứ không đọc lại plan.getPriceVnd(): gói có thể đổi giá trong lúc
        // đơn nằm chờ, mà thứ phải lưu lại là con số trường thật sự trả. Dùng subtotal (giá niêm yết)
        // chứ không phải total: khoản bù nâng cấp là chuyện của LẦN MUA này, không phải giá trị của
        // kỳ -- ghi total thì lần nâng cấp sau lại bù dựa trên một con số đã bị bù một lần rồi.
        var subscription = schoolSubscriptionRepository.save(SchoolSubscription.activate(
            order.getSchoolId(), plan, order.getSubtotalAmountVnd(), startsAt, now));

        seedQuotaRecords(subscription.getId(), planId, previousSubscriptionId);
        carryForwardUserAllocations(previousSubscriptionId, subscription.getId());

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
        var unfinished = schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now);
        for (var previous : unfinished) {
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
     */
    private void seedQuotaRecords(UUID subscriptionId, UUID planId, UUID previousSubscriptionId) {
        var carriedByType = unspentFundingByQuotaType(previousSubscriptionId);

        List<SchoolSubscriptionQuotaRecord> records = subscriptionPlanQuotaRepository
            .findBySubscriptionPlanId(planId).stream()
            .map(planQuota -> SchoolSubscriptionQuotaRecord.seeded(
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

        var seededTypes = records.stream().map(record -> record.getQuotaType()).toList();
        carriedByType.forEach((quotaType, amountVnd) -> {
            if (!seededTypes.contains(quotaType)) {
                LOGGER.warn(
                    "Kỳ {} không có ví {} nên {}đ tiền tự nạp chưa tiêu của kỳ {} không mang sang được"
                        + " -- gói mới không còn loại hạn mức này.",
                    subscriptionId, quotaType, amountVnd, previousSubscriptionId);
            }
        });
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
        var carried = new EnumMap<QuotaType, BigDecimal>(QuotaType.class);
        for (var record : quotaRecordRepository.findBySchoolSubscriptionId(previousSubscriptionId)) {
            var unspent = record.unspentFundedVnd();
            if (unspent.signum() > 0) {
                carried.put(record.getQuotaType(), unspent);
            }
        }
        return carried;
    }

    /**
     * Chép trần chi CÁ NHÂN của kỳ cũ sang kỳ mới.
     *
     * <p>Bản ghi phân bổ gắn với {@code school_subscription_id}, mà mỗi kỳ là một dòng subscription
     * mới, nên nếu không chép thì mọi trần cá nhân BIẾN MẤT đúng lúc kỳ mới bắt đầu -- và hai ví hỏng
     * theo hai chiều ngược nhau, cùng im lặng như nhau:
     *
     * <ul>
     *   <li>PRACTICE: {@code findPracticeSpendableFundsVnd} lấy LEAST với COALESCE(...,0), nên không
     *       có dòng nghĩa là 0 -- CẢ TRƯỜNG mất quyền luyện nói cho tới khi có người vào chia lại,
     *       ngay sau khi trường vừa trả tiền gia hạn.</li>
     *   <li>EXAM: {@code ClassTestTokenQuotaGuardService.remainingUserAllocation} trả null khi không
     *       có dòng, và cửa chặn bỏ qua null -- mọi giáo viên thành không còn trần chi nào.</li>
     * </ul>
     *
     * <p>Chép TRẦN, KHÔNG chép phần đã tiêu: {@code upsertAllocation} dựng dòng mới với
     * {@code used = 0}, đúng bằng cách {@link #seedQuotaRecords} cho ví cấp trường bắt đầu lại từ 0.
     * Kỳ mới là một ngân sách mới, ở cả hai cấp.
     *
     * <p><b>Ảnh chụp lấy tại thời điểm TRẢ TIỀN, không phải lúc kỳ mới chạy.</b> Với gia hạn sớm, hai
     * mốc đó cách nhau và mọi thay đổi phân bổ trong quãng giữa sẽ không theo sang. Chấp nhận có ý:
     * bám đúng mốc bắt đầu thì phải có thêm một job quét kỳ tới hạn, trong khi cái giá của việc lệch
     * chỉ là vài con số cũ mà quản trị trường sửa lại được bất cứ lúc nào -- còn cái giá của việc
     * không chép gì cả là cả trường đứng hình.
     */
    private void carryForwardUserAllocations(UUID previousSubscriptionId, UUID newSubscriptionId) {
        if (previousSubscriptionId == null) {
            return;
        }

        for (var quotaType : QuotaType.values()) {
            var carried = quotaUserAllocationRepository
                .findBySchoolSubscriptionIdAndQuotaType(previousSubscriptionId, quotaType);
            for (var allocation : carried) {
                quotaUserAllocationRepository.upsertAllocation(
                    newSubscriptionId, quotaType, allocation.getUserId(), allocation.getAllocatedAmountVnd());
            }
            if (!carried.isEmpty()) {
                LOGGER.info("Chuyển {} trần chi cá nhân {} từ kỳ {} sang kỳ {}",
                    carried.size(), quotaType, previousSubscriptionId, newSubscriptionId);
            }
        }
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
        var invoice = invoiceRepository.save(Invoice.issueFor(order.getId(), payment.getId(), now));
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
        var schoolAdminIds = schoolUserRepository
            .findBySchoolIdWithRole(order.getSchoolId(), RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream()
            .map(schoolUser -> schoolUser.getUserId())
            .toList();

        // Chốt danh sách người nhận NGAY LÚC phát, không để consumer truy vấn lại -- giống hệt các
        // event nợ hạn mức. Nhờ vậy mỗi lần chạy lại (retry, replay từ DLT) đều ra đúng tập người
        // nhận cũ và uk_notifications_user_event chặn được trùng.
        var payload = jsonSerializationPort.toJson(new InvoicePaidPayloadV1(
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
