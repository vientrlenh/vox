package com.sep.vox.application.port.input.usecase.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.order.CreateSubscriptionOrderCommand;
import com.sep.vox.application.port.input.service.SubscriptionUpgradePolicyService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ServiceFeePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.OrderItemRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Places an order to buy ONE BILLING PERIOD of a plan for a school. Replaces the old SubmitRequestUseCase.
 *
 * <p>This use case does NOT grant the plan. It only records the intent to buy in the PENDING state; the subscription is
 * only really created when the money arrives (see OrderSettlementService). This is the biggest difference from the old
 * flow, where ApproveRequestUseCase created an ACTIVE subscription directly without any gateway confirming the money had
 * been collected.
 */
@Service
@RequiredArgsConstructor
public class CreateSubscriptionOrderUseCase implements IUseCase<CreateSubscriptionOrderCommand, UUID> {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionUpgradePolicyService upgradePolicyService;
    private final ServiceFeePort serviceFeePort;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(CreateSubscriptionOrderCommand input) {
        // Permission is already blocked at @PreAuthorize("hasRole('SCHOOL_ADMIN')"); all that is left here is to get WHICH
        // school -- and it comes from the token, so there is no path to place an order on behalf of another school.
        UUID schoolId = userContextPort.getCurrentSchoolId();

        requireNoSuspendedSubscription(schoolId);
        SubscriptionPlan plan = requirePlanOnSale(input.subscriptionPlanId());
        requireNoOpenSubscriptionOrder(schoolId);

        Instant now = Instant.now();
        requireNoQueuedPeriod(schoolId, now);

        Order savedOrder = orderRepository.save(buildOrder(schoolId, plan, now));

        // A subscription order always has EXACTLY ONE line: each order buys one period of one plan. This line is the ONLY place
        // that records which plan was bought (orders has no plan_id column), so settlement reads it to know what to grant.
        orderItemRepository.save(OrderItem.forSubscriptionPlan(savedOrder.getId(), plan.getId(), plan.getPriceVnd()));

        return savedOrder.getId();
    }

    // A school that the System Admin has force-suspended (fraud, ...) is not allowed to place a new order until it is
    // explicitly unsuspended: settlement would create a new ACTIVE subscription without touching the SUSPENDED record,
    // i.e. walk around UnsuspendSubscriptionUseCase.
    //
    // Looking at the WHOLE history is deliberate -- it must not be dodged by waiting for the suspended period to pass.
    // The sanction still has an end: SubscriptionExpiryJob moves a SUSPENDED period past its endDate to EXPIRED (see
    // SpringDataSchoolSubscriptionRepository.expireOverdue), so this gate locks at most until the end of the paid
    // period, not forever.
    private void requireNoSuspendedSubscription(UUID schoolId) {
        boolean hasSuspended = schoolSubscriptionRepository.findBySchoolId(schoolId).stream()
            .anyMatch(subscription -> subscription.getStatus() == SchoolSubscriptionStatus.SUSPENDED);
        if (hasSuspended) {
            throw new IllegalArgumentException("Gói đăng ký của trường đang bị đình chỉ, không thể đặt đơn mới.");
        }
    }

    private SubscriptionPlan requirePlanOnSale(UUID subscriptionPlanId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(subscriptionPlanId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói"));
        // The old flow only checked the plan EXISTS -- a real gap: an order pointing at a DRAFT plan (price not
        // finalised, admin can still edit) or an ARCHIVED one (no longer sold) could still be created, and by
        // settlement the school had already paid for a plan that should not be on sale.
        if (plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Gói này hiện không được mở bán.");
        }
        return plan;
    }

    // Block BEFORE the database does, to give a readable message. If skipped, uq_orders_one_open_subscription_order still
    // blocks but as a DataIntegrityViolationException -- the school gets a 500 error instead of learning that it has an
    // unfinished order to pay or cancel.
    private void requireNoOpenSubscriptionOrder(UUID schoolId) {
        if (orderRepository.findOpenSubscriptionOrderBySchoolId(schoolId).isPresent()) {
            throw new IllegalArgumentException(
                "Trường đang có một đơn đăng ký chờ thanh toán, hãy hoàn tất hoặc hủy đơn đó trước khi đặt đơn mới.");
        }
    }

    // At most ONE period may be queued. Early renewal is not blocked -- a school can still buy ahead on the very first day,
    // which is what it needs to do when budget has to be spent within the financial year -- only stacking several periods is
    // blocked.
    //
    // DELIBERATELY not a threshold like "must use 80% before renewal is allowed": that threshold aims at the wrong target. It
    // does not prevent stacking (it can still be stacked, just later) yet it blocks exactly the schools that have money and
    // want to pay early. Blocking by the NUMBER OF WAITING PERIODS is what really needs blocking: it keeps the price-lock
    // risk to exactly one period.
    private void requireNoQueuedPeriod(UUID schoolId, Instant now) {
        if (hasQueuedPeriod(schoolId, now)) {
            throw new IllegalArgumentException(
                "Trường đã có một kỳ đã thanh toán đang chờ tới ngày chạy, hãy đợi kỳ đó bắt đầu rồi mới mua tiếp.");
        }
    }

    /**
     * An UPGRADE when the school is running a DIFFERENT plan, otherwise SUBSCRIPTION_REQUEST -- covering both first-time
     * registration and renewal of the plan currently in use.
     *
     * <p>Renewal DELIBERATELY has no OrderType of its own: its settlement is identical to first-time registration (create a
     * new period + a new quota set), differing only in the start point, and that point can be derived from the data without
     * needing a label on the order. An upgrade really is DIFFERENT: the running period is cut short and there is a credit,
     * so it deserves a type of its own.
     */
    private Order buildOrder(UUID schoolId, SubscriptionPlan plan, Instant now) {
        SchoolSubscription inForce = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).orElse(null);

        if (inForce != null && isUpgradeTo(plan, inForce)) {
            return upgradeOrder(schoolId, plan, now, inForce);
        }
        return chainedOrder(schoolId, plan, now);
    }

    /**
     * NÂNG CẤP chỉ khi gói mới ĐẮT HƠN gói đang chạy. Gói rẻ hơn hoặc bằng giá đi đường nối tiếp --
     * và đó không phải chuyện đặt tên.
     *
     * <p>Khoản bù bị chặn trên bởi giá gói mới (xem SubscriptionUpgradePolicyService), nên "nâng cấp"
     * xuống một gói rẻ hơn sẽ âm thầm đốt phần chênh: đang dùng gói 12 triệu, đổi sang gói 3 triệu ở
     * ngày thứ hai thì chỉ được bù tối đa 3 triệu, mất trắng gần 9 triệu mà không có chỗ nào báo.
     * Đi đường nối tiếp thì trường dùng hết kỳ đắt tiền đã trả rồi mới sang gói rẻ -- không mất gì.
     *
     * <p>So GIÁ NIÊM YẾT của hai gói, không so pricePaidSnapshot: câu hỏi ở đây là "gói mới có phải
     * hàng cao cấp hơn không", chứ không phải "lần trước trường trả bao nhiêu" -- một trường từng
     * được giảm giá không vì thế mà biến mọi lần mua sau thành nâng cấp.
     */
    private boolean isUpgradeTo(SubscriptionPlan plan, SchoolSubscription inForce) {
        if (inForce.getSubscriptionPlanId().equals(plan.getId())) {
            return false;
        }
        return subscriptionPlanRepository.findById(inForce.getSubscriptionPlanId())
            .map(currentPlan -> plan.getPriceVnd().compareTo(currentPlan.getPriceVnd()) > 0)
            .orElse(false);
    }

    private Order upgradeOrder(UUID schoolId, SubscriptionPlan plan, Instant now, SchoolSubscription inForce) {
        upgradePolicyService.requireUpgradeEligible(inForce, now);

        // Credit EVERY period that has not finished, because settlement will close them all -- including a paid period waiting in
        // the queue.
        //
        // Computed HERE, when the order is placed, not at settlement: the school must see the amount to pay before pressing pay,
        // and the gateway collects exactly the order's total. The consequence is that the longer the order waits, the more
        // generous the credit is compared with the moment of the plan change (off by at most one day, per Order.PENDING_TTL) --
        // accepted as a deviation in the school's favour, because recomputing at settlement would make the money already collected
        // no longer match the order, and that is the thing that cannot be fixed.
        BigDecimal unusedCredit = upgradePolicyService.calculateUnusedCredit(
            schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now), plan.getPriceVnd(), now);

        // Service fee added on top (of the amount after the credit)
        BigDecimal serviceFeeVnd = plan.getPriceVnd().subtract(unusedCredit)
            .multiply(serviceFeePort.serviceFeeRatio())
            .setScale(0, RoundingMode.HALF_UP);

        return Order.forSubscriptionUpgrade(schoolId,
            "Nâng cấp lên " + plan.getName(), plan.getPriceVnd(), unusedCredit, serviceFeeVnd, now);
    }

    /**
     * Có kỳ nào đã trả tiền mà chưa tới ngày chạy không.
     *
     * <p>"Chưa kết thúc" trừ "đang chạy" -- không cần thêm truy vấn nào: kỳ đang chạy nhiều nhất là
     * một, nên còn dư dòng nào trong danh sách chưa-kết-thúc thì đó chính là kỳ đang xếp hàng.
     */
    private boolean hasQueuedPeriod(UUID schoolId, Instant now) {
        int unfinished = schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now).size();
        int inForce = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).isPresent() ? 1 : 0;
        return unfinished - inForce > 0;
    }

    /** A new registration, a renewal, or a switch to a cheaper plan -- all of them chain after the running period. */
    private Order chainedOrder(UUID schoolId, SubscriptionPlan plan, Instant now) {
        BigDecimal serviceFeeVnd = plan.getPriceVnd().multiply(serviceFeePort.serviceFeeRatio()).setScale(0, RoundingMode.HALF_UP);
        return Order.forSubscription(schoolId, OrderType.SUBSCRIPTION_REQUEST,
            "Đăng ký " + plan.getName(), plan.getPriceVnd(), serviceFeeVnd, now);
    }
}
