package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.OrderJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    Page<OrderJpaEntity> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    List<OrderJpaEntity> findByStatus(String status);

    /**
     * Danh sách đơn cho System Admin: lọc theo trường/trạng thái/loại, tìm theo tên trường.
     *
     * <p>Mỗi bộ lọc đều bỏ qua được bằng cách truyền null -- gộp vào một câu thay vì tách nhiều method
     * vì tổ hợp bật/tắt của bốn bộ lọc là 16 biến thể.
     *
     * <p>Sắp thêm theo id DESC sau createdAt: hai đơn tạo trong cùng một tick (trường bấm nhanh, hoặc
     * import) mà chỉ sắp theo createdAt thì Postgres trả về thứ tự tùy ý, phân trang sẽ lặp hoặc bỏ
     * sót dòng giữa các trang. id là uuidv7 nên "id DESC" chính là "mới nhất trước".
     */
    @Query("""
        SELECT o FROM OrderJpaEntity o
        WHERE (:schoolId IS NULL OR o.schoolId = :schoolId)
          AND (:status IS NULL OR o.status = :status)
          AND (:type IS NULL OR o.type = :type)
          AND (:keywordPattern IS NULL OR EXISTS (
                SELECT 1 FROM SchoolJpaEntity sc
                WHERE sc.id = o.schoolId AND LOWER(sc.name) LIKE :keywordPattern
              ))
        ORDER BY o.createdAt DESC, o.id DESC
        """)
    Page<OrderJpaEntity> findForAdmin(
        @Param("schoolId") UUID schoolId,
        @Param("status") String status,
        @Param("type") String type,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    boolean existsBySchoolIdAndTypeInAndStatus(UUID schoolId, Collection<String> types, String status);

    // Soi đúng điều kiện của uq_orders_one_open_subscription_order. Trả Optional được vì unique
    // index đảm bảo tối đa một dòng khớp.
    @Query("""
        SELECT o FROM OrderJpaEntity o
        WHERE o.schoolId = :schoolId
          AND o.status = 'PENDING'
          AND o.type IN ('SUBSCRIPTION_REQUEST', 'SUBSCRIPTION_UPGRADE')
        """)
    Optional<OrderJpaEntity> findOpenSubscriptionOrderBySchoolId(@Param("schoolId") UUID schoolId);

    // Khoảng NỬA MỞ [from, to) -- cùng lý do với SpringDataSchoolBalanceEntryRepository.
    @Query("""
        SELECT COALESCE(SUM(o.totalAmountVnd), 0) FROM OrderJpaEntity o
        WHERE o.status = :status
          AND o.createdAt >= :from AND o.createdAt < :to
        """)
    BigDecimal sumTotalAmountByStatusInRange(
        @Param("status") String status,
        @Param("from") Instant from,
        @Param("to") Instant to);

    /**
     * Đơn theo trạng thái trong một khoảng, cùng khoảng NỬA MỞ [from, to) với
     * {@link #sumTotalAmountByStatusInRange} -- hai câu này phải soi cùng một mốc thời gian, nếu
     * không thì tổng doanh thu và biểu đồ theo tháng trên cùng một màn hình sẽ không cộng lại bằng
     * nhau.
     *
     * <p>Có mặt riêng thay vì dùng {@code findByStatus}: dashboard chỉ vẽ 12-24 tháng gần nhất, mà
     * findByStatus nạp MỌI đơn thành công từ trước tới nay -- một danh sách chỉ có tăng, không bao
     * giờ giảm.
     */
    List<OrderJpaEntity> findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        String status, Instant from, Instant to);

    /**
     * Bản LỌC THEO TRƯỜNG của câu trên, cho màn quản trị của chính trường đó.
     *
     * <p>Phải là một câu riêng chứ không dùng lại câu trên rồi lọc ở Java: câu trên KHÔNG có điều kiện
     * school_id, nên gọi nó từ màn của một trường là đưa doanh thu của MỌI trường vào tổng chi và biểu
     * đồ 12 tháng của trường đang đăng nhập -- rò rỉ số liệu giữa các trường chứ không chỉ là nạp thừa.
     */
    List<OrderJpaEntity> findBySchoolIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        UUID schoolId, String status, Instant from, Instant to);

    // PESSIMISTIC_WRITE: chặn hai lần chốt thanh toán song song trên cùng một đơn (webhook cổng đua
    // với PendingOrderReconciler). @Version trên entity chỉ phát hiện xung đột SAU khi cả hai đã
    // làm việc thừa; khóa ở đây chặn ngay từ đầu.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderJpaEntity> findWithLockById(UUID id);
}
