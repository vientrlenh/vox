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

    /**
     * Kỳ thuê bao ĐANG CÓ HIỆU LỰC tại {@code at} -- lọc theo NGÀY chứ không chỉ theo status.
     *
     * <p>Bắt buộc phải lọc theo ngày kể từ khi cho phép gia hạn sớm: lúc đó trường có HAI dòng cùng
     * ACTIVE, một dòng đang chạy và một dòng đã trả tiền nhưng chỉ bắt đầu khi dòng kia hết hạn.
     * {@code findFirstBySchoolIdAndStatus} sẽ trả về một trong hai một cách tùy ý, và mọi thứ đọc từ
     * nó -- hạn mức, guard kỳ thi, trừ tiêu dùng -- sẽ bám vào nhầm kỳ.
     *
     * <p>Cũng vá luôn khe hở của SubscriptionExpiryJob: job chạy mỗi giờ nên ngay sau thời điểm hết
     * hạn vẫn còn một quãng dòng cũ mang status ACTIVE. Lọc theo ngày thì quãng đó không tồn tại --
     * cùng lý do SubscriptionPeriodGuardService đã cố ý so thẳng start_date/end_date.
     *
     * <p>ORDER BY start_date DESC: nếu vì lỗi dữ liệu mà có hai dòng cùng phủ {@code at} thì lấy
     * dòng mới nhất, thay vì để thứ tự phụ thuộc vào cách Postgres trả về.
     */
    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE s.schoolId = :schoolId
          AND s.status = 'ACTIVE'
          AND s.startDate <= :at
          AND s.endDate > :at
        ORDER BY s.startDate DESC
        """)
    List<SchoolSubscriptionJpaEntity> findInForceBySchoolId(
        @Param("schoolId") UUID schoolId, @Param("at") Instant at);

    /**
     * Kỳ có endDate xa nhất còn chưa kết thúc -- mốc để một lần gia hạn mới nối tiếp vào.
     *
     * <p>Lấy theo endDate lớn nhất chứ không phải kỳ đang chạy: trường gia hạn hai lần liên tiếp thì
     * lần thứ hai phải nối sau lần thứ nhất, không phải chồng lên nó.
     */
    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE s.schoolId = :schoolId
          AND s.status = 'ACTIVE'
          AND s.endDate > :at
        ORDER BY s.endDate DESC
        """)
    List<SchoolSubscriptionJpaEntity> findUnfinishedBySchoolId(
        @Param("schoolId") UUID schoolId, @Param("at") Instant at);

    /**
     * Kỳ gần đây nhất của trường, KHÔNG lọc theo trạng thái hay ngày -- để biết trường đang/từng
     * dùng gói nào mà đề xuất gia hạn.
     *
     * <p>Phải nhận cả kỳ đã EXPIRED: gia hạn muộn (gói hết hạn tuần trước, giờ trường mới quay lại)
     * là đường đi phổ biến nhất, và ở đó không còn kỳ nào ACTIVE để mà hỏi.
     */
    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE s.schoolId = :schoolId
        ORDER BY s.endDate DESC
        """)
    List<SchoolSubscriptionJpaEntity> findMostRecentBySchoolId(@Param("schoolId") UUID schoolId);
    List<SchoolSubscriptionJpaEntity> findBySchoolId(UUID schoolId);
    boolean existsBySubscriptionPlanIdAndStatus(UUID subscriptionPlanId, String status);

    // Bulk update bỏ qua persistence context nên KHÔNG tự tăng cột @Version — phải tăng tay
    // (s.version = s.version + 1) để bất kỳ bản ghi nào đang được load ở nơi khác (vd
    // CancelSubscriptionUseCase) mà save() sau khi job này chạy sẽ bị Hibernate phát hiện lệch
    // version và ném OptimisticLockException, thay vì âm thầm ghi đè EXPIRED ngược lại ACTIVE.
    //
    // Hết hạn áp cho CẢ SUSPENDED, không riêng ACTIVE. Đình chỉ là cắt quyền dùng trong phần CÒN LẠI
    // của một kỳ đã trả tiền; kỳ đó kết thúc thì không còn gì để đình chỉ nữa, và giữ nguyên trạng
    // thái SUSPENDED là lẫn giữa "kỳ này từng bị đình chỉ" (lịch sử) với "trường đang bị chế tài"
    // (trạng thái). Trước đây chỉ lọc ACTIVE nên dòng SUSPENDED không bao giờ rời trạng thái đó, mà
    // CreateSubscriptionOrderUseCase lại soi TOÀN BỘ lịch sử -- một lần đình chỉ 3 ngày ở kỳ năm
    // ngoái cũng cấm trường mua lại VĨNH VIỄN.
    //
    // Hết hạn KHÔNG phải là gỡ đình chỉ: cột suspended_* giữ nguyên, sổ school_subscription_events
    // giữ nguyên, và trường không lấy lại quyền dùng gì cả (quyền đến từ một kỳ ACTIVE còn hiệu lực,
    // mà EXPIRED thì không phải). Nó chỉ trả lại quyền MUA kỳ mới. Muốn cấm mua vĩnh viễn thì phải là
    // một cờ cấp TRƯỜNG, không phải một dòng đăng ký nằm mãi ở SUSPENDED.
    @Modifying
    @Query("""
        UPDATE SchoolSubscriptionJpaEntity s
        SET s.status = 'EXPIRED', s.version = s.version + 1
        WHERE s.status IN ('ACTIVE', 'SUSPENDED') AND s.endDate < :cutoff
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

    // CÁC NATIVE QUERY DƯỚI ĐÂY đã theo tên bảng/cột ĐÍCH của V2, giống hệt các @Column của
    // SchoolSubscriptionQuotaRecordJpaEntity / SchoolSubscriptionQuotaUserAllocationJpaEntity. Native
    // query không được Hibernate soi lúc khởi động nên sai tên ở đây chỉ nổ vào đúng request đầu tiên
    // chạm tới -- mà findActiveSubscriptionIdForUser thì chạy ở MỌI lượt nói luyện tập.
    //
    // Phần rename CỘT của 2 bảng quota hiện VẪN CÒN THIẾU trong V2 (V1 tạo ra subscription_id /
    // total_allocated / used_quantity / allocated_quantity, V2 mới chỉ đổi tên bảng). Ba query này
    // cùng với các entity sẽ chạy được khi V2 bổ sung nốt phần đó -- cố tình viết theo tên đích thay
    // vì tên cũ để chỉ còn ĐÚNG MỘT chỗ phải sửa, thay vì hai chỗ đang nói hai thứ khác nhau.
    @Query(value = """
        SELECT subscription.id
        FROM school_users school_user
        JOIN school_subscriptions subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND CURRENT_DATE BETWEEN subscription.start_date AND subscription.end_date
        WHERE school_user.user_id = :userId
          AND (school_user.end_date IS NULL OR school_user.end_date >= CURRENT_TIMESTAMP)
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<UUID> findActiveSubscriptionIdForUser(@Param("userId") UUID userId);

    // Phải LEFT JOIN thêm school_subscription_quota_user_allocations và lấy LEAST(...): ConsumeQuotaService
    // trừ CẢ hạn mức trường LẪN hạn mức cá nhân (khi có allocation row cho user) cho PRACTICE, nên
    // cửa chặn ở BuildPracticePaperUseCase phải soi đúng 2 thước đó -- trước đây chỉ soi hạn mức
    // trường, học sinh hết hạn mức cá nhân nhưng trường còn dư sẽ lọt cửa, dựng đề xong mới chết ở
    // lượt nói đầu. COALESCE về phần của trường khi user chưa có allocation row (ConsumeQuotaService
    // cũng bỏ qua kiểm tra cá nhân trong case đó, qua .ifPresent) -- không được LEAST thẳng với NULL,
    // Postgres trả NULL cho cả biểu thức.
    //
    // Vế của TRƯỜNG cộng thêm số dư ví tự nạp, vế CÁ NHÂN thì không: từ khi ConsumeQuotaService trừ
    // phần vượt hạn mức sang ví tự nạp, "trường còn trả được bao nhiêu" = hạn mức còn lại + số dư,
    // nên soi mỗi hạn mức sẽ chặn học sinh của một trường vẫn còn tiền. Còn hạn mức cá nhân là TRẦN
    // CHI nội bộ chứ không phải túi tiền -- cộng tiền của trường vào đó là xóa sạch chính cái trần
    // quản trị trường vừa đặt. Xem ClassTestTokenQuotaGuardService, cửa chặn song song bên đường thi.
    //
    // GREATEST(..., 0) kẹp số dư âm về 0: âm là NỢ, đã bị SchoolSubscriptionDebtGuardService chặn ở
    // một cửa riêng; cộng thẳng số âm vào đây sẽ trừ lần thứ hai vào hạn mức còn lại của trường.
    // LATERAL để biểu thức đó chỉ được viết MỘT lần dù LEAST cần nó ở cả hai vế.
    @Query(value = """
        SELECT LEAST(
            school_funds.amount_vnd,
            COALESCE(user_allocation.allocated_amount_vnd - user_allocation.used_amount_vnd,
                     school_funds.amount_vnd)
        )
        FROM school_users school_user
        JOIN school_subscriptions subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND CURRENT_DATE BETWEEN subscription.start_date AND subscription.end_date
        JOIN school_subscription_quota_records quota
          ON quota.school_subscription_id = subscription.id
         AND quota.quota_type = 'PRACTICE'
        LEFT JOIN school_subscription_quota_user_allocations user_allocation
          ON user_allocation.school_subscription_id = subscription.id
         AND user_allocation.quota_type = 'PRACTICE'
         AND user_allocation.user_id = school_user.user_id
        LEFT JOIN school_balances balance
          ON balance.school_id = school_user.school_id
        CROSS JOIN LATERAL (
            SELECT quota.total_allocated_amount_vnd - quota.used_amount_vnd
                 + GREATEST(COALESCE(balance.balance_vnd, 0), 0) AS amount_vnd
        ) school_funds
        WHERE school_user.user_id = :userId
          AND (school_user.end_date IS NULL OR school_user.end_date >= CURRENT_TIMESTAMP)
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    List<BigDecimal> findPracticeSpendableFundsVnd(@Param("userId") UUID userId);

    @Query(value = """
        SELECT plan.max_time_per_attempt_min
        FROM school_users school_user
        JOIN school_subscriptions subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND CURRENT_DATE BETWEEN subscription.start_date AND subscription.end_date
        JOIN subscription_plans plan
          ON plan.id = subscription.subscription_plan_id
        WHERE school_user.user_id = :userId
          AND (school_user.end_date IS NULL OR school_user.end_date >= CURRENT_TIMESTAMP)
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Integer> findMaxTimePerAttemptMinForUser(@Param("userId") UUID userId);
}
