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
            .orElseThrow(() -> new ResourceNotFoundException("Order of the payment not found"));
    }

    private boolean isOpenForSettlement(Order order, PaymentRecord payment) {
        if (order.getStatus() == OrderStatus.SUCCESS) {
            log.info("Order {} is already SUCCESS -- skipping the repeated settlement for payment {}",
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
                "MONEY RECEIVED FOR A CLOSED ORDER -- manual refund needed: orderId={} orderStatus={} paymentId={} amountVnd={}",
                order.getId(), order.getStatus(), payment.getId(), payment.getAmountVnd());
            return false;
        }
        return true;
    }

    /**
     * The gateway has confirmed this attempt did NOT bring in money (cancelled/expired/failed).
     *
     * <p>ONLY closes the payment_records row, does NOT touch the order: the order stays PENDING so the school can click pay
     * again right away, and the constraint uq_payment_records_one_pending_per_order is already clear then because
     * the old row is no longer PENDING. Closing the order here would force the school to create a new order just because they
     * clicked the cancel button by mistake on the gateway page.
     *
     * <p>A genuinely expired order is already handled by expires_at + the sweeper job, not the callback's concern.
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
            .orElseThrow(() -> new ResourceNotFoundException("Order to expire not found"));

        if (order.getStatus() != OrderStatus.PENDING || now.isBefore(order.getExpiresAt())) {
            return false;
        }
        if (paymentRecordRepository.findPendingByOrderId(orderId).isPresent()) {
            log.warn("Order {} is past its deadline but still has a pending attempt -- not closing it, waiting for reconciliation to finish",
                orderId);
            return false;
        }

        order.setStatus(OrderStatus.EXPIRED);
        orderRepository.save(order);
        log.info("Closing order {} because the payment deadline passed ({})", orderId, order.getExpiresAt());
        return true;
    }

    /**
     * @return the subscription period tied to this settlement, used to build the invoice. Null is valid: a school with no
     *         plan can still top up -- see {@link #creditBalance}.
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

        log.info("Credited {} VND to the balance of school {} from order {} -- new balance {}",
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
     * This top-up has just moved the balance from NEGATIVE back to non-negative: the school is out of debt and the lock opens by itself.
     *
     * <p>This is the ONLY call site of publishSchoolDebtCleared, and before V4 it did not exist --
     * the event-publishing function was written, the email template existed, but no line of code called it. Consequence: the
     * debt ledger recorded when the school fell into debt and when it exceeded the cap, then stayed silent forever. For the principal, the ledger
     * used to explain "why the school is locked" never said the school had got out.
     *
     * <p>Topping up is the ONLY path that adds money to the balance (REFUND/ADJUSTMENT exist in the enum but no
     * factory builds them yet), so putting it here covers everything. When those two get a write path, they must call
     * this same function again.
     *
     * <p>Compares BEFORE/AFTER on the same row while holding the lock, like crossedIntoDebt in the opposite direction:
     * it reports exactly once, on the CROSSING, and topping up again after the debt is gone creates no event.
     */
    private void reportDebtClearedIfCrossedBack(
            UUID schoolId, UUID subscriptionId, BigDecimal balanceBefore, BigDecimal balanceAfter, Instant now) {
        if (balanceBefore.signum() >= 0 || balanceAfter.signum() < 0) {
            return;
        }

        // school_debt_events.subscription_id is NOT NULL, yet the debt belongs to the SCHOOL, not to any
        // subscription period -- a school that pays off its debt just as its plan expires has no id to
        // write. Skip the event row instead of letting the INSERT blow up and take the successful top-up down with it.
        if (subscriptionId == null) {
            log.warn(
                "School {} just cleared its debt (balance {} -> {}) but has no active subscription to attach the event to -- skipping the CLEARED row.",
                schoolId, balanceBefore, balanceAfter);
            return;
        }

        log.info("School {} is out of debt: balance {} -> {}", schoolId, balanceBefore, balanceAfter);
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
            .orElseThrow(() -> new ResourceNotFoundException("Subscription plan of the order not found"));

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
                "Order " + order.getId() + " has no SUBSCRIPTION line to activate"));
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
            log.info("Early renewal of plan {} for school {} from order {} -- the new period runs {} to {}",
                planId, order.getSchoolId(), order.getId(), startsAt, subscription.getEndDate());
        } else {
            log.info("Activating plan {} for school {} from order {} -- valid until {}",
                planId, order.getSchoolId(), order.getId(), subscription.getEndDate());
        }
    }

    /**
     * The start point for a new subscription / renewal: chain after the furthest period that is still valid, or run
     * immediately if there is none.
     *
     * <p>Uses the LARGEST endDate rather than the running period: when a school renews twice in a row the
     * second must queue after the first; anchoring on the running period would make the two renewals overlap and
     * lose one cycle.
     */
    private Instant nextPeriodStart(UUID schoolId, Instant now) {
        return schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now).stream()
            .findFirst()
            .map(s -> s.getEndDate())
            .orElse(now);
    }

    /**
     * Upgrade: the new plan takes effect IMMEDIATELY, so every unfinished period of the school is closed here.
     *
     * <p>Closed by pulling endDate back to {@code now} rather than only changing the status: the two queries that tell
     * "which period is running" look at the date range (findInForceBySchoolId) or at endDate
     * (findUnfinishedBySchoolId), so an EXPIRED row whose endDate is still in the future would keep
     * counting as an "unfinished period" in the second place -- and the next renewal would queue behind a
     * dead period. Pulling endDate back to now is also recording the truth: that period really ended at this moment.
     *
     * <p>Close ALL of them, not just the running one: a school may have just renewed (a future period queued)
     * and then changed its mind to upgrade. Missing the queued period would, once the new plan expires, drop the school back to exactly
     * the old plan it just paid to leave.
     *
     * <p>The quotas of the closed periods are NOT carried to the new period: the unused part has already been converted to
     * money and deducted from this very upgrade order (see SubscriptionUpgradePolicyService), and adding it
     * again would compensate twice for the same thing.
     */
    private Instant cutOverToUpgrade(UUID schoolId, Instant now) {
        List<SchoolSubscription> unfinished = schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now);
        for (SchoolSubscription previous : unfinished) {
            previous.setStatus(SchoolSubscriptionStatus.EXPIRED);
            previous.setEndDate(now);
            schoolSubscriptionRepository.save(previous);
        }
        if (!unfinished.isEmpty()) {
            log.info("Upgrade of school {}: closing {} unfinished periods at {}", schoolId, unfinished.size(), now);
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
            .forEach(record -> log.info("Moving {} VND of unspent self-funded money of quota {} to period {}",
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
                    "Period {} has no quota {} so {} VND of unspent self-funded money of period {} cannot be carried over"
                        + " -- the new plan no longer has this quota type.",
                    subscriptionId, quotaType, amountVnd, previousSubscriptionId);
            }
        });
    }

    /**
     * The unspent self-funded part of the old period, per quota type.
     *
     * <p>The new plan may NO LONGER have a quota type that the old period had (the school switches to a plan without PRACTICE,
     * for instance). Then the money of that type has nowhere to be carried and stays in the old period -- a rare case with
     * no handling yet, so a WARN log is written to expose it instead of letting it evaporate silently.
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
            log.info("Moving {} personal spending caps {} from period {} to period {}",
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
            log.info("Dropping {} cap rows {} of people who are no longer eligible -- not carrying them over to period {}",
                carried.size() - kept.size(), quotaType, newSubscriptionId);
        }
        return kept;
    }

    /**
     * The set of people who are STILL eligible to receive the cap of this quota type -- ACTIVE, still in the school, still in the right
     * role. The same three conditions that {@code sumAllocatedForEligibleUsers} uses to compute "already distributed", and the same
     * call that {@code DistributeQuotaToUsersService.fetchEligibleUserIds} uses to build the list.
     *
     * <p><b>An empty Optional means UNDETERMINABLE, and the caller must copy EVERYTHING.</b> Both
     * failure paths here lead to a set missing people rather than a clearly empty set, and filtering by a
     * set missing people TAKES AWAY the cap of real people -- exactly the Q-2 disaster, except this time caused by
     * the very patch that fixed Q-2. Keeping one row that should have been dropped only makes the table one period bigger; the two
     * costs are not in the same class.
     */
    private Optional<Set<UUID>> eligibleUserIds(UUID schoolId, QuotaType quotaType) {
        String roleCode = quotaType.allocationRoleCode();
        Optional<Role> role = roleRepository.findByCode(roleCode);
        if (role.isEmpty()) {
            log.error("Role {} not found -- copying ALL caps {} to the new period instead of filtering,"
                + " so that the cap of someone still teaching/studying is not taken away.", roleCode, quotaType);
            return Optional.empty();
        }

        PageResult<SchoolUser> page = schoolUserRepository.findBySchoolId(schoolId, null, role.get().getId(),
            UserStatus.ACTIVE.name(), null, false, 1, MAX_CARRY_FORWARD_USERS);
        if (page.totalElements() > page.content().size()) {
            log.error("School {} has {} people eligible for cap {} but only {} could be read --"
                + " copying ALL instead of filtering, because filtering by an incomplete set would erase the cap of the remainder.",
                schoolId, page.totalElements(), quotaType, page.content().size());
            return Optional.empty();
        }

        return Optional.of(page.content().stream()
            .map(schoolUser -> schoolUser.getUserId())
            .collect(Collectors.toSet()));
    }

    /**
     * The scale-down ratio so that the total of the copied caps fits the new period's quota. {@code null} = no scaling needed.
     *
     * <p>Rounds DOWN so the total after multiplying is always less than or equal to the quota cap -- better a few dong short
     * than a new period that opens already over the cap, which is exactly what this clamp exists to prevent.
     */
    private BigDecimal carryScaleFor(List<SchoolSubscriptionQuotaUserAllocation> kept,
            BigDecimal poolTotalVnd, QuotaType quotaType, UUID previousSubscriptionId, UUID newSubscriptionId) {
        BigDecimal totalCarriedVnd = kept.stream()
            .map(allocation -> orZero(allocation.getAllocatedAmountVnd()))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        if (totalCarriedVnd.signum() <= 0 || totalCarriedVnd.compareTo(poolTotalVnd) <= 0) {
            return null;
        }

        log.warn("Personal caps {} of period {} add up to {} VND, more than the {} VND quota of period {} -- scaling down by"
            + " ratio so the new period does not open already over its cap.",
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
     * The invoice is issued only AFTER the money has arrived -- this is the main business change versus the old model, where the
     * invoice was created when the link was issued and carried the payment status itself.
     *
     * <p>The existence check comes before issuing: the order status guard above already blocks most repeat paths,
     * but the invoice is an external-facing document so it is worth one more query to be sure two numbers are never issued
     * for the same order.
     *
     * @return the invoice just issued, or empty when the order already has an invoice from an earlier settlement. The caller uses
     *         exactly this signal to decide whether to notify the school.
     */
    private Optional<Invoice> issueInvoice(Order order, PaymentRecord payment, Instant now) {
        if (invoiceRepository.existsByOrderId(order.getId())) {
            return Optional.empty();
        }
        Invoice invoice = invoiceRepository.save(Invoice.issueFor(order.getId(), payment.getId(), now));
        log.info("Issuing invoice {} for order {}", invoice.getInvoiceNumber(), order.getId());

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
     * Order type -> invoice source type. The two enums are separate because {@code InvoiceSourceType} describes what
     * is being bought on the document, while {@code OrderType} describes how the order was created.
     *
     * <p>New registration and upgrade both map to {@code SUBSCRIPTION}: on the document both are money
     * for buying one plan period, differing in the order-creation path and not in what is sold. Telling those two paths apart
     * is the job of {@code OrderType}, which is already on the order.
     */
    private InvoiceSourceType sourceTypeOf(OrderType orderType) {
        return switch (orderType) {
            case TOPUP -> InvoiceSourceType.TOPUP;
            case SUBSCRIPTION_REQUEST -> InvoiceSourceType.SUBSCRIPTION;
            case SUBSCRIPTION_UPGRADE -> InvoiceSourceType.SUBSCRIPTION;
        };
    }
}
