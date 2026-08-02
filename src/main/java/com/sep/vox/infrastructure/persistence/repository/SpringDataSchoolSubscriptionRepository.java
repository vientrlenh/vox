package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionJpaEntity;

public interface SpringDataSchoolSubscriptionRepository extends JpaRepository<SchoolSubscriptionJpaEntity, UUID> {
    Optional<SchoolSubscriptionJpaEntity> findFirstBySchoolIdAndStatus(UUID schoolId, String status);
    List<SchoolSubscriptionJpaEntity> findAllBySchoolId(UUID schoolId);

    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE (:planId IS NULL OR s.planId = :planId)
          AND (:status IS NULL OR s.status = :status)
          AND (:keywordPattern IS NULL OR EXISTS (
                SELECT 1 FROM SchoolJpaEntity sc
                WHERE sc.id = s.schoolId AND LOWER(sc.name) LIKE :keywordPattern
              ))
    """)
    Page<SchoolSubscriptionJpaEntity> findAllForAdmin(
        @Param("planId") UUID planId,
        @Param("status") String status,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    @Query(value = """
        SELECT subscription.id
        FROM school_users school_user
        JOIN school_subscription subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND CURRENT_DATE BETWEEN subscription.start_date AND subscription.end_date
        WHERE school_user.user_id = :userId
          AND school_user.end_date IS NULL
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<UUID> findActiveSubscriptionIdForUser(@Param("userId") UUID userId);

    @Query(value = """
        SELECT quota.total_allocated - quota.used_quantity
        FROM school_users school_user
        JOIN school_subscription subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND CURRENT_DATE BETWEEN subscription.start_date AND subscription.end_date
        JOIN subscription_quota quota
          ON quota.subscription_id = subscription.id
         AND quota.quota_type = 'PRACTICE'
        WHERE school_user.user_id = :userId
          AND school_user.end_date IS NULL
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Integer> findPracticeQuotaRemaining(@Param("userId") UUID userId);

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
          AND school_user.end_date IS NULL
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Integer> findMaxTimePerAttemptMinForUser(@Param("userId") UUID userId);
}
