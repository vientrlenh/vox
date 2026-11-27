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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.event.InvoicePaidPayloadV1;
import com.sep.vox.application.exception.ResourceNotFoundException;
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
@RequiredArgsConstructor
@Slf4j
public class OrderSettlementService {

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
     * Same number as {@code DistributeQuotaToUsersService.MAX_ELIGIBLE_USERS_PAGE_SIZE}: both sides must
     * see the same set of people, otherwise a person at the edge of the limit is given a cap on the screen and
     * then loses it at the next renewal.
     */
    private static final int MAX_CARRY_FORWARD_USERS = 10_000;

    /**
     * Money HAS ARRIVED: closes the attempt, closes the order, delivers the goods, issues the invoice.
     *
     * <p>IDEMPOTENT by order status. It has to be: both gateways resend when they do not receive a successful
     * response, and the pending-order sweep job (PendingOrderReconciler) can run exactly when the webhook arrives.
     * Without this latch a top-up order would credit the balance several times.
     */
    @Transactional
    public void settlePaid(PaymentRecord payment, Instant paidAt) {
        Order order = lockOrderOf(payment);
        if (!isOpenForSettlement(order, payment)) {
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

    private Order lockOrderOf(PaymentRecord payment) {
        // Lock the order BEFORE reading its status: two threads both reading "the order is still PENDING" and both going on to
        // credit the money is exactly the scenario that uq_payment_records_one_paid_per_order cannot block, because each thread
        // works on a different payment_records row of the same order.
        return orderRepository.findByIdForUpdate(payment.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng của lần thanh toán"));
    }

    private boolean isOpenForSettlement(Order order, PaymentRecord payment) {
        if (order.getStatus() == OrderStatus.SUCCESS) {
            log.info("Đơn {} đã SUCCESS -- bỏ qua lần chốt lặp cho payment {}",
                order.getId(), payment.getId());
            return false;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            // Money has arrived for an order that is ALREADY CLOSED. This is NOT a harmless repeated callback: the school has paid but
            // will receive nothing, and the system has no refund flow yet. It must be loud so that someone handles it by hand, instead
            // of quietly returning like the SUCCESS branch above.
            //
            // How this case is reached: the school cancels the order while a SePay session is still alive (the gateway does not allow
            // early cancellation, so CancelOrderUseCase blocks it, but if that block is loosened this is the consequence), or the order
            // expires exactly while the school is paying.
            log.error(
                "TIỀN VỀ CHO ĐƠN ĐÃ ĐÓNG -- cần hoàn tiền thủ công: orderId={} orderStatus={} paymentId={} amountVnd={}",
                order.getId(), order.getStatus(), payment.getId(), payment.getAmountVnd());
            return false;
        }
        return true;
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng cần hết hạn"));

        if (order.getStatus() != OrderStatus.PENDING || now.isBefore(order.getExpiresAt())) {
            return false;
        }
        if (paymentRecordRepository.findPendingByOrderId(orderId).isPresent()) {
            log.warn("Đơn {} đã quá hạn nhưng còn lần thử đang treo -- chưa đóng, đợi đối soát xong",
                orderId);
            return false;
        }

        order.setStatus(OrderStatus.EXPIRED);
        orderRepository.save(order);
        log.info("Đóng đơn {} do quá hạn thanh toán ({})", orderId, order.getExpiresAt());
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
     * Credits the school wallet with the ITEM amount, not the amount collected: the service fee is our own pay, so
     * crediting the whole total to the wallet would hand the fee back to the school as a spendable balance.
     */
    private UUID creditBalance(Order order, Instant now) {
        // Lock the wallet: a school can have several top-up orders at once (uq_orders_one_open_subscription_order
        // deliberately does NOT block TOPUP), so two callbacks arriving close together would read the same old balance
        // and both overwrite it -- one top-up is lost entirely with no error surfacing.
        SchoolBalance balance = schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(order.getSchoolId(), now);

        BigDecimal credited = order.getSubtotalAmountVnd();
        BigDecimal balanceBefore = balance.getBalanceVnd();
        BigDecimal balanceAfter = balance.apply(credited, now);
        schoolBalanceRepository.save(balance);

        UUID activeSubscriptionId = activeSubscriptionIdOf(order.getSchoolId());

        schoolBalanceEntryRepository.save(SchoolBalanceEntry.forTopUp(
            order.getSchoolId(), activeSubscriptionId, order.getId(), credited, balanceAfter, now));

        log.info("Nạp {} VND vào ví trường {} từ đơn {} -- số dư mới {}",
            credited, order.getSchoolId(), order.getId(), balanceAfter);

        reportDebtClearedIfCrossedBack(order.getSchoolId(), activeSubscriptionId, balanceBefore, balanceAfter, now);

        return activeSubscriptionId;
    }

    // subscriptionId is only for tracing "which plan the school was on when it topped up" -- the balance belongs to no
    // plan, and a school without a plan can still top up, so null is valid.
    private UUID activeSubscriptionIdOf(UUID schoolId) {
        return schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .map(s -> s.getId())
            .orElse(null);
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
            log.warn(
                "Trường {} vừa hết nợ (số dư {} -> {}) nhưng không có gói nào đang hoạt động để gắn sự kiện -- bỏ qua dòng CLEARED.",
                schoolId, balanceBefore, balanceAfter);
            return;
        }

        log.info("Trường {} đã hết nợ: số dư {} -> {}", schoolId, balanceBefore, balanceAfter);
        schoolDebtNotificationService.publishSchoolDebtCleared(subscriptionId, schoolId, now);
    }

    /**
     * Activates a new subscription period and grants the quota that comes with the plan.
     *
     * <p>The plan is read from order_items rather than from a column on orders: a subscription order carries exactly
     * one SUBSCRIPTION line and that line already froze the unit price at order time.
     *
     * <p>The new period CONTINUES the old one if the old one is still valid (early renewal), instead of overwriting it
     * or closing it. Two ACTIVE rows is a NORMAL state here -- one running, one paid and queued behind it;
     * findActiveBySchoolId filters by date so it always yields exactly one period in force. See SchoolSubscription.activate.
     */
    private UUID activateSubscription(Order order, Instant now) {
        // Pin down the source period BEFORE any write: cutOverToUpgrade will pull the endDate of unfinished periods back
        // to now, and the save() below adds a new period -- asking for "the most recent period" after either of those
        // is asking about a world that has already changed.
        UUID previousSubscriptionId = previousSubscriptionIdOf(order.getSchoolId());
        UUID planId = subscriptionPlanIdOf(order);
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói dịch vụ của đơn đăng ký"));

        Instant startsAt = order.getType() == OrderType.SUBSCRIPTION_UPGRADE
            ? cutOverToUpgrade(order.getSchoolId(), now)
            : nextPeriodStart(order.getSchoolId(), now);
        SchoolSubscription subscription = saveActivatedSubscription(order, plan, startsAt, now);

        // EARLY renewal: the old period keeps running until startsAt, so its unspent self-topped-up money cannot be
        // settled yet -- see seedQuotaRecords and CarryQuotaFundingAtPeriodStartService.
        Map<QuotaType, BigDecimal> seededPoolTotals = seedQuotaRecords(
            subscription.getId(), planId, previousSubscriptionId, startsAt.isAfter(now));
        carryForwardUserAllocations(
            previousSubscriptionId, subscription.getId(), order.getSchoolId(), seededPoolTotals);
        logActivation(order, planId, subscription, startsAt, now);

        return subscription.getId();
    }

    private UUID previousSubscriptionIdOf(UUID schoolId) {
        return schoolSubscriptionRepository
            .findMostRecentBySchoolId(schoolId)
            .map(previous -> previous.getId())
            .orElse(null);
    }

    private UUID subscriptionPlanIdOf(Order order) {
        return orderItemRepository.findByOrderId(order.getId()).stream()
            .filter(item -> item.getType() == OrderItemType.SUBSCRIPTION)
            .map(item -> item.getItemId())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Đơn đăng ký " + order.getId() + " không có dòng SUBSCRIPTION nào để kích hoạt"));
    }

    // The price paid is taken from the ORDER rather than re-reading plan.getPriceVnd(): a plan can change price while the
    // order waits, and what must be stored is the number the school really paid. Uses subtotal (list price) rather than
    // total: the upgrade credit is about THIS purchase, not the value of the period -- recording total would make the
    // next upgrade credit based on a number that was already credited once.
    private SchoolSubscription saveActivatedSubscription(Order order, SubscriptionPlan plan, Instant startsAt, Instant now) {
        return schoolSubscriptionRepository.save(SchoolSubscription.activate(
            order.getSchoolId(), plan, order.getSubtotalAmountVnd(), startsAt, now));
    }

    private void logActivation(Order order, UUID planId, SchoolSubscription subscription, Instant startsAt, Instant now) {
        if (startsAt.isAfter(now)) {
            log.info("Gia hạn sớm gói {} cho trường {} từ đơn {} -- kỳ mới chạy {} tới {}",
                planId, order.getSchoolId(), order.getId(), startsAt, subscription.getEndDate());
        } else {
            log.info("Kích hoạt gói {} cho trường {} từ đơn {} -- hạn tới {}",
                planId, order.getSchoolId(), order.getId(), subscription.getEndDate());
        }
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
            log.info("Nâng cấp trường {}: đóng {} kỳ chưa kết thúc tại {}", schoolId, unfinished.size(), now);
        }
        return now;
    }

    /**
     * Copies the plan's quota norms into the counters of this period.
     *
     * <p>Must COPY rather than read straight from the plan on every use: plan norms can change midway, while a running
     * period must keep the numbers it sold. This is also where used_amount_vnd restarts from 0 for the new period --
     * the self-topped-up wallet balance is not touched, it lives in school_balances and survives across periods.
     *
     * <p><b>Money the school topped up INTO the quota wallet must be carried over.</b> It is no longer in
     * school_balances (it left the wallet when topped up, with a QUOTA_FUNDING entry) but sits inside the record that
     * is rebuilt here -- so not carrying it over DELETES the school's real money on the very day it renews. Quite
     * different from losing a personal spending ceiling: an administrator can redistribute a ceiling, but not money.
     *
     * <p>Only the UNSPENT part is carried, by the convention "plan money is spent first, self-topped-up money
     * after" -- see {@link SchoolSubscriptionQuotaRecord#unspentFundedVnd()}.
     *
     * <p><b>A FUTURE period is only SCHEDULED, not carried.</b> An early renewal has two different moments: the old
     * period keeps running until {@code startsAt}, can still be spent and still be topped up, so settling its unspent
     * part here would settle a number that will be wrong -- spend the rest and the money is spent twice, top up and
     * the new amount evaporates at the boundary. The wallet is seeded with exactly the plan norm plus an appointment,
     * and {@link CarryQuotaFundingAtPeriodStartService} settles the real number when the new period starts. See V13.
     *
     * @param deferFundingCarry the new period has not reached its start date yet (early renewal)
     * @return the wallets of the NEW period by type, with their ceilings. {@link #carryForwardUserAllocations} needs
     *         both: the key set says which wallet types really exist in the new period, and the value is the clamp
     *         for the total of the personal ceilings copied over.
     */
    private Map<QuotaType, BigDecimal> seedQuotaRecords(UUID subscriptionId, UUID planId,
            UUID previousSubscriptionId, boolean deferFundingCarry) {
        // Do NOT read the old period's wallet in the deferred branch: reading is settling, and settling early is
        // exactly the bug being fixed.
        Map<QuotaType, BigDecimal> carriedByType = deferFundingCarry
            ? Map.<QuotaType, BigDecimal>of()
            : unspentFundingByQuotaType(previousSubscriptionId);

        List<SchoolSubscriptionQuotaRecord> records = buildQuotaRecords(
            subscriptionId, planId, previousSubscriptionId, deferFundingCarry, carriedByType);

        records.forEach(quotaRecordRepository::save);

        records.stream()
            .filter(record -> record.getFundedFromBalanceVnd().signum() > 0)
            .forEach(record -> log.info("Chuyển {}đ tiền tự nạp chưa tiêu của ví {} sang kỳ {}",
                record.getFundedFromBalanceVnd(), record.getQuotaType(), subscriptionId));

        Map<QuotaType, BigDecimal> seededPoolTotals = new EnumMap<QuotaType, BigDecimal>(QuotaType.class);
        records.forEach(record ->
            seededPoolTotals.put(record.getQuotaType(), record.getTotalAllocatedAmountVnd()));
        warnAboutFundingWithoutPool(carriedByType, seededPoolTotals, subscriptionId, previousSubscriptionId);

        return seededPoolTotals;
    }

    private List<SchoolSubscriptionQuotaRecord> buildQuotaRecords(UUID subscriptionId, UUID planId,
            UUID previousSubscriptionId, boolean deferFundingCarry, Map<QuotaType, BigDecimal> carriedByType) {
        return subscriptionPlanQuotaRepository
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
    }

    private void warnAboutFundingWithoutPool(Map<QuotaType, BigDecimal> carriedByType,
            Map<QuotaType, BigDecimal> seededPoolTotals, UUID subscriptionId, UUID previousSubscriptionId) {
        carriedByType.forEach((quotaType, amountVnd) -> {
            if (!seededPoolTotals.containsKey(quotaType)) {
                log.warn(
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
            carryForwardPoolAllocations(previousSubscriptionId, newSubscriptionId, schoolId,
                seededPool.getKey(), orZero(seededPool.getValue()));
        }
    }

    private void carryForwardPoolAllocations(UUID previousSubscriptionId, UUID newSubscriptionId, UUID schoolId,
            QuotaType quotaType, BigDecimal poolTotalVnd) {
        List<SchoolSubscriptionQuotaUserAllocation> carried = quotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaType(previousSubscriptionId, quotaType);
        if (carried.isEmpty()) {
            return;
        }

        List<SchoolSubscriptionQuotaUserAllocation> kept = keepStillEligible(carried, schoolId, quotaType, newSubscriptionId);

        BigDecimal scale = carryScaleFor(kept, poolTotalVnd, quotaType, previousSubscriptionId, newSubscriptionId);
        for (SchoolSubscriptionQuotaUserAllocation allocation : kept) {
            quotaUserAllocationRepository.upsertAllocation(newSubscriptionId, quotaType,
                allocation.getUserId(), scaled(orZero(allocation.getAllocatedAmountVnd()), scale));
        }

        if (!kept.isEmpty()) {
            log.info("Chuyển {} trần chi cá nhân {} từ kỳ {} sang kỳ {}",
                kept.size(), quotaType, previousSubscriptionId, newSubscriptionId);
        }
    }

    private List<SchoolSubscriptionQuotaUserAllocation> keepStillEligible(
            List<SchoolSubscriptionQuotaUserAllocation> carried, UUID schoolId, QuotaType quotaType,
            UUID newSubscriptionId) {
        Optional<Set<UUID>> eligible = eligibleUserIds(schoolId, quotaType);
        List<SchoolSubscriptionQuotaUserAllocation> kept = eligible
            .map(userIds -> carried.stream()
                .filter(allocation -> userIds.contains(allocation.getUserId()))
                .toList())
            .orElse(carried);
        if (kept.size() < carried.size()) {
            log.info("Bỏ {} dòng trần {} của người không còn đủ điều kiện -- không mang sang kỳ {}",
                carried.size() - kept.size(), quotaType, newSubscriptionId);
        }
        return kept;
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
            log.error("Không tìm thấy vai trò {} -- chép TOÀN BỘ trần {} sang kỳ mới thay vì lọc,"
                + " để không lấy mất trần của người còn đang dạy/học.", roleCode, quotaType);
            return Optional.empty();
        }

        PageResult<SchoolUser> page = schoolUserRepository.findBySchoolId(schoolId, null, role.get().getId(),
            UserStatus.ACTIVE.name(), null, false, 1, MAX_CARRY_FORWARD_USERS);
        if (page.totalElements() > page.content().size()) {
            log.error("Trường {} có {} người đủ điều kiện nhận trần {} nhưng chỉ đọc được {} --"
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

        log.warn("Trần cá nhân {} của kỳ {} cộng lại {}đ, lớn hơn ví {}đ của kỳ {} -- thu nhỏ theo"
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
        log.info("Phát hành hóa đơn {} cho đơn {}", invoice.getInvoiceNumber(), order.getId());

        return Optional.of(invoice);
    }

    /**
     * Tells every SCHOOL_ADMIN of the school that an invoice has been paid: a receipt email (InvoiceEmailConsumer)
     * and an in-app notification (NotificationPushedEventConsumer).
     *
     * <p>FIX: both consumers, the mail template, the category/target mapping and the topic routing already existed,
     * but NO code wrote the {@code InvoicePaid} outbox event -- splitting {@code InvoiceSettlementService} into this
     * class dropped the {@code publishInvoicePaid} call (the leftover trace is the javadoc of
     * SchoolDebtNotificationService, still pointing at a method that no longer exists). Result: a school that finished
     * paying received nothing, and since no error surfaced it was only exposed by cross-reading publisher against consumer.
     *
     * <p>Lives in the SAME transaction as closing the order and issuing the invoice, like every other publisher in this
     * flow: the document and the notification about it live or die together. Placed after {@code existsByOrderId}, so a
     * repeated callback does not publish the event a second time.
     */
    private void publishInvoicePaid(Order order, Invoice invoice, Instant now, UUID subscriptionId) {
        List<UUID> schoolAdminIds = schoolAdminIdsOf(order.getSchoolId());

        // Fix the recipient list RIGHT WHEN publishing, rather than letting the consumer query again -- exactly like the
        // quota debt events. That way every re-run (retry, replay from the DLT) produces the same set of recipients as
        // before and uk_notifications_user_event can block duplicates.
        String payload = jsonSerializationPort.toJson(new InvoicePaidPayloadV1(
            schoolAdminIds,
            order.getSchoolId(),
            subscriptionId,
            // sourceId = the ORDER id. An invoice is always issued for an order (Invoice.issueFor), so it is the only
            // source that always exists; it is also the key that lets the notification open the right order detail page.
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

    private List<UUID> schoolAdminIdsOf(UUID schoolId) {
        return schoolUserRepository
            .findBySchoolIdWithRole(schoolId, RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream()
            .map(schoolUser -> schoolUser.getUserId())
            .toList();
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
