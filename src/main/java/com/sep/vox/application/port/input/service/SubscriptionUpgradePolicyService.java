package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.subscription.SchoolSubscription;

/**
 * The rules of a MID-PERIOD UPGRADE: when an upgrade is allowed, and how much to credit back.
 *
 * <p>The two questions live together because they share one computation -- the fraction of time elapsed in the
 * running period -- and because splitting them makes it easy to change one and forget the other, and a mismatch here is a mismatch in money.
 *
 * <h2>Why upgrading needs a minimum elapsed time</h2>
 *
 * <p>The credit is proportional to the unused part, while the new period is always a FULL cycle. Let f be the fraction already
 * elapsed at upgrade time: the school pays {@code P × f} and receives a whole period. Without a floor f tends to
 * 0 -- upgrading back and forth between two plans the very next day, paying almost 0 each time while always
 * holding a full period. Unlimited use, near-zero revenue.
 *
 * <p>With a floor f every upgrade must wait {@code f × T}, so the cost per unit of time is
 * {@code (P × f) / (f × T) = P / T} -- exactly the list price. In other words the floor is NOT there to punish anyone:
 * it is what brings continuous plan switching back to the normal charge rate. 20% is chosen because the specific number only
 * decides how noisy it is (5 switches per period), not the revenue.
 */
@Service
public class SubscriptionUpgradePolicyService {

    /** At least 20% of the current period must be used before upgrading is allowed -- see the class Javadoc. */
    private static final BigDecimal MIN_ELAPSED_RATIO = new BigDecimal("0.20");

    /**
     * The minimum amount an upgrade order must still have to pay -- see {@link #calculateUnusedCredit}.
     *
     * <p>The specific number has no business meaning, it only has to sit above the REAL minimum of the payment gateway.
     * Adjust it to the PayOS/SePay limit if that is higher.
     */
    private static final BigDecimal MIN_PAYABLE_VND = new BigDecimal("1000");

    private static final DateTimeFormatter DISPLAY_DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneConstant.BUSINESS_ZONE);

    /**
     * Blocks the upgrade when the current period is still too new.
     *
     * <p>Looks at the RUNNING period, not the furthest one: a queued period has not started so its "how much used"
     * is always 0, and taking it as the reference would never allow an upgrade.
     */
    public void requireUpgradeEligible(SchoolSubscription inForce, Instant at) {
        long total = Duration.between(inForce.getStartDate(), inForce.getEndDate()).toSeconds();
        if (total <= 0) {
            return;
        }
        long elapsed = Duration.between(inForce.getStartDate(), at).toSeconds();
        long minElapsed = MIN_ELAPSED_RATIO.multiply(BigDecimal.valueOf(total)).longValue();
        if (elapsed >= minElapsed) {
            return;
        }

        Instant eligibleAt = inForce.getStartDate().plusSeconds(minElapsed);
        throw new IllegalArgumentException(
            "The current plan has only just started and cannot be upgraded yet. It can be upgraded from "
                + DISPLAY_DATE.format(eligibleAt) + ".");
    }

    /**
     * The total credit for EVERY period that will be closed by the upgrade -- both the running period and the periods that are paid and
     * queued.
     *
     * <p>The queued periods must be included: the upgrade closes everything (see OrderSettlementService.cutOverToUpgrade),
     * so missing any one means closing a period the school paid for without refunding anything. For a period that has not started, the
     * unused part is exactly the whole price paid.
     *
     * <p>Takes {@code pricePaidSnapshot} rather than the plan's current price: the refund must be based on the amount
     * the school REALLY paid, and the plan may have changed price since then.
     *
     * <p>The real cap is {@code cap - MIN_PAYABLE_VND}, not {@code cap}: a 0-dong order cannot be paid any more than a
     * negative one. Hitting exactly the cap is something that REALLY HAPPENS, not an assumption -- a school with a running period
     * PLUS a paid queued period easily has a total unused part exceeding the price of a single plan, and then the order comes out at
     * exactly 0 VND and sits PENDING until it expires: the upgrade has no way forward.
     *
     * <p>Chose to clamp the credit instead of letting a 0-dong order settle itself: settlePaid takes a PaymentRecord,
     * so that other path would have to invent a payment record that does not exist, corrupting the very place
     * used to reconcile with the gateway. And "an over-credit loses the excess" is already the rule here -- that is
     * exactly {@code min(cap)} -- so keeping a small amount payable is the same rule, not a new one.
     *
     * @param cap the nominal cap of the total credit -- the price of the new plan. The constraint
     *            chk_orders_discount_amount_vnd_lower_or_equals_than_subtotal_and_charged_fee does not
     *            allow a discount greater than the goods total, and every gateway rejects a negative order.
     */
    public BigDecimal calculateUnusedCredit(List<SchoolSubscription> unfinished, BigDecimal cap, Instant at) {
        BigDecimal total = unfinished.stream()
            .map(subscription -> unusedValueOf(subscription, at))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        // max(ZERO) for a plan cheaper than the minimum: then no credit is given and the school pays
        // the full price, still better than an order that cannot be paid.
        BigDecimal payableCap = cap.subtract(MIN_PAYABLE_VND).max(BigDecimal.ZERO);
        return total.min(payableCap);
    }

    private BigDecimal unusedValueOf(SchoolSubscription subscription, Instant at) {
        if (subscription.getPricePaidSnapshot() == null) {
            return BigDecimal.ZERO;
        }
        long total = Duration.between(subscription.getStartDate(), subscription.getEndDate()).toSeconds();
        long remaining = Duration.between(at, subscription.getEndDate()).toSeconds();
        if (total <= 0 || remaining <= 0) {
            return BigDecimal.ZERO;
        }
        // A period that has not started: remaining is larger than the period length, but the credit is at most the full price.
        remaining = Math.min(remaining, total);

        return subscription.getPricePaidSnapshot()
            .multiply(BigDecimal.valueOf(remaining))
            .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP);
    }
}
