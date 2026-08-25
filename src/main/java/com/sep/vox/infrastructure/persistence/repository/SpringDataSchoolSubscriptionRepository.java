package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionJpaEntity;

public interface SpringDataSchoolSubscriptionRepository extends JpaRepository<SchoolSubscriptionJpaEntity, UUID> {
    Optional<SchoolSubscriptionJpaEntity> findFirstBySchoolIdAndStatus(UUID schoolId, String status);
    List<SchoolSubscriptionJpaEntity> findBySchoolId(UUID schoolId);
    boolean existsBySubscriptionPlanIdAndStatus(UUID subscriptionPlanId, String status);

    // Bulk update bỏ qua persistence context nên KHÔNG tự tăng cột @Version — phải tăng tay
    // (s.version = s.version + 1) để bất kỳ bản ghi nào đang được load ở nơi khác (vd
    // CancelSubscriptionUseCase) mà save() sau khi job này chạy sẽ bị Hibernate phát hiện lệch
    // version và ném OptimisticLockException, thay vì âm thầm ghi đè EXPIRED ngược lại ACTIVE.
    @Modifying
    @Query("""
        UPDATE SchoolSubscriptionJpaEntity s
        SET s.status = 'EXPIRED', s.version = s.version + 1
        WHERE s.status = 'ACTIVE' AND s.endDate < :cutoff
    """)
    int expireOverdue(@Param("cutoff") Instant cutoff);

    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE (:planId IS NULL OR s.subscriptionPlanId = :planId)
          AND (:status IS NULL OR s.status = :status)
          AND (:keywordPattern IS NULL OR EXISTS (
                SELECT 1 FROM SchoolJpaEntity sc
                WHERE sc.id = s.schoolId AND LOWER(sc.name) LIKE :keywordPattern
              ))
    """)
    Page<SchoolSubscriptionJpaEntity> findForAdmin(
        @Param("planId") UUID planId,
        @Param("status") String status,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    // CÁC NATIVE QUERY DƯỚI ĐÂY CÒN DÙNG TÊN CỘT CŨ (subscription.plan_id, quota.subscription_id,
    // quota.total_allocated, quota.used_quantity, user_allocation.allocated_quantity/used_quantity)
    // và tên bảng cũ school_subscription / subscription_plan. Hiện vẫn chạy đúng vì V2 mới chỉ đổi
    // TÊN BẢNG của 3 bảng quota chứ chưa đổi cột nào. Khi V2 được bổ sung phần rename cột/bảng còn
    // lại thì phải sửa đồng thời cả 3 query này -- JPQL ở trên đã theo tên mới, native thì không.
    @Query(value = """
        SELECT subscription.id
        FROM school_users school_user
        JOIN school_subscription subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND CURRENT_DATE BETWEEN subscription.start_date AND subscription.end_date
        WHERE school_user.user_id = :userId
          AND (school_user.end_date IS NULL OR school_user.end_date >= CURRENT_TIMESTAMP)
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<UUID> findActiveSubscriptionIdForUser(@Param("userId") UUID userId);

    // Phải LEFT JOIN thêm school_subscription_quota_user_allocations và lấy LEAST(...): ConsumeQuotaUseCase
    // trừ CẢ hạn mức trường LẪN hạn mức cá nhân (khi có allocation row cho user) cho PRACTICE, nên
    // cửa chặn ở BuildPracticePaperUseCase phải soi đúng 2 thước đó -- trước đây chỉ soi hạn mức
    // trường, học sinh hết hạn mức cá nhân nhưng trường còn dư sẽ lọt cửa, dựng đề xong mới chết ở
    // lượt nói đầu (ConsumeQuotaUseCase ném QuotaExceededException). COALESCE về hạn mức trường khi
    // user chưa có allocation row (ConsumeQuotaUseCase cũng bỏ qua kiểm tra cá nhân trong case đó,
    // qua .ifPresent) -- không được LEAST thẳng với NULL, Postgres trả NULL cho cả biểu thức.
    @Query(value = """
        SELECT LEAST(
            quota.total_allocated - quota.used_quantity,
            COALESCE(user_allocation.allocated_quantity - user_allocation.used_quantity,
                      quota.total_allocated - quota.used_quantity)
        )
        FROM school_users school_user
        JOIN school_subscription subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND CURRENT_DATE BETWEEN subscription.start_date AND subscription.end_date
        JOIN school_subscription_quota_records quota
          ON quota.subscription_id = subscription.id
         AND quota.quota_type = 'PRACTICE'
        LEFT JOIN school_subscription_quota_user_allocations user_allocation
          ON user_allocation.subscription_id = subscription.id
         AND user_allocation.quota_type = 'PRACTICE'
         AND user_allocation.user_id = school_user.user_id
        WHERE school_user.user_id = :userId
          AND (school_user.end_date IS NULL OR school_user.end_date >= CURRENT_TIMESTAMP)
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    List<BigDecimal> findPracticeQuotaRemaining(@Param("userId") UUID userId);

    @Query(value = """
        SELECT plan.max_time_per_attempt_min
        FROM school_users school_user
        JOIN school_subscription subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND CURRENT_DATE BETWEEN subscription.start_date AND subscription.end_date
        JOIN subscription_plan plan
          ON plan.id = subscription.plan_id
        WHERE school_user.user_id = :userId
          AND (school_user.end_date IS NULL OR school_user.end_date >= CURRENT_TIMESTAMP)
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Integer> findMaxTimePerAttemptMinForUser(@Param("userId") UUID userId);
}
