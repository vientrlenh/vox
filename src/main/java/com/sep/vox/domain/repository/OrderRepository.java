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
 * "Ý định mua" của trường -- thay cho subscription_request + token_purchase cũ, và mang luôn phần
 * vòng đời thanh toán mà invoice cũ ôm nhầm.
 */
public interface OrderRepository {
    Optional<Order> findById(UUID id);

    /**
     * Khóa đơn tới hết transaction trước khi chốt kết quả thanh toán. Webhook cổng và
     * PendingOrderReconciler có thể cùng chốt một đơn: không khóa thì cả hai đọc thấy PENDING
     * trước khi bên kia commit và cấp gói/cộng tiền hai lần.
     */
    Optional<Order> findByIdForUpdate(UUID id);

    Order save(Order order);

    List<Order> findBySchoolId(UUID schoolId);
    PageResult<Order> findBySchoolId(UUID schoolId, int page, int size);

    /** Danh sách đơn cho System Admin -- mọi bộ lọc đều bỏ qua được bằng null. */
    PageResult<Order> findForAdmin(UUID schoolId, OrderStatus status, OrderType type, String keyword, int page, int size);
    List<Order> findByStatus(OrderStatus status);

    /**
     * Đơn gói đang mở của trường, soi đúng điều kiện của uq_orders_one_open_subscription_order
     * (PENDING + type IN (SUBSCRIPTION_REQUEST, SUBSCRIPTION_UPGRADE)). Dùng để chặn TRƯỚC ở use case
     * với thông báo tử tế, thay vì để unique index ném DataIntegrityViolationException lên người dùng.
     */
    Optional<Order> findOpenSubscriptionOrderBySchoolId(UUID schoolId);

    boolean existsBySchoolIdAndTypeInAndStatus(UUID schoolId, List<OrderType> types, OrderStatus status);

    /** Doanh thu đã thu trong kỳ. {@code from} bao gồm, {@code to} KHÔNG bao gồm. */
    BigDecimal sumTotalAmountByStatusInRange(OrderStatus status, Instant from, Instant to);

    /**
     * Đơn theo trạng thái trong một kỳ, cùng quy ước khoảng với
     * {@link #sumTotalAmountByStatusInRange}. Dùng cho các màn thống kê cần TÁCH NHÓM số tiền (theo
     * tháng, theo loại đơn) chứ không chỉ cần một tổng.
     */
    List<Order> findByStatusInRange(OrderStatus status, Instant from, Instant to);
}
