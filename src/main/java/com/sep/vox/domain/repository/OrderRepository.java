package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;

/**
 * The school's "purchase intent" -- replaces the old subscription_request + token_purchase, and also carries the
 * payment lifecycle that the old invoice wrongly held.
 */
public interface OrderRepository {
    Optional<Order> findById(UUID id);

    /**
     * Locks the order until the end of the transaction before settling the payment result. The gateway webhook and
     * PendingOrderReconciler may settle the same order: without the lock both read PENDING
     * before the other commits and grant the plan/add money twice.
     */
    Optional<Order> findByIdForUpdate(UUID id);

    Order save(Order order);

    List<Order> findBySchoolId(UUID schoolId);
    PageResult<Order> findBySchoolId(UUID schoolId, int page, int size);

    /** List of orders for the System Admin -- every filter can be skipped with null. */
    PageResult<Order> findForAdmin(UUID schoolId, OrderStatus status, OrderType type, String keyword, int page, int size);
    List<Order> findByStatus(OrderStatus status);

    /**
     * The open subscription order of a school, matching exactly the condition of uq_orders_one_open_subscription_order
     * (PENDING + type IN (SUBSCRIPTION_REQUEST, SUBSCRIPTION_UPGRADE)). Used to block EARLY in the use case
     * with a proper message, instead of letting the unique index throw DataIntegrityViolationException at the user.
     */
    Optional<Order> findOpenSubscriptionOrderBySchoolId(UUID schoolId);

    boolean existsBySchoolIdAndTypeInAndStatus(UUID schoolId, List<OrderType> types, OrderStatus status);

    /** Revenue collected in the period. {@code from} is inclusive, {@code to} is NOT inclusive. */
    BigDecimal sumTotalAmountByStatusInRange(OrderStatus status, Instant from, Instant to);

    /**
     * Orders by status in a period, with the same range convention as
     * {@link #sumTotalAmountByStatusInRange}. Used by statistics screens that need to SPLIT the amount into groups (by
     * month, by order type) rather than just a total.
     */
    List<Order> findByStatusInRange(OrderStatus status, Instant from, Instant to);

    /**
     * Same as {@link #findByStatusInRange} but ONLY for one school -- for the school-level admin screen.
     *
     * <p>Exists separately because the unfiltered version above reads the orders of the whole system: calling it by mistake from
     * a school screen would turn its spending total and spending chart into aggregates of every school.
     */
    List<Order> findBySchoolIdAndStatusInRange(UUID schoolId, OrderStatus status, Instant from, Instant to);
}
