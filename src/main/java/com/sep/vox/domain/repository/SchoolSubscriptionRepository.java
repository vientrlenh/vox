package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;

public interface SchoolSubscriptionRepository {
    Optional<SchoolSubscription> findById(UUID id);
    SchoolSubscription save(SchoolSubscription subscription);
    Optional<SchoolSubscription> findActiveBySchoolId(UUID schoolId);
    List<SchoolSubscription> findBySchoolId(UUID schoolId);
    PageResult<SchoolSubscription> findForAdmin(UUID planId, SchoolSubscriptionStatus status, String keyword, int page, int size);
    boolean existsActiveByPlanId(UUID planId);

    /**
     * Chuyển hàng loạt ACTIVE -> EXPIRED cho các subscription đã qua {@code cutoff}. Trả về số dòng
     * bị đổi.
     *
     * <p>{@code cutoff} là mốc EXCLUSIVE: chỉ hết hạn khi {@code endDate < cutoff}. Nơi gọi quyết
     * định mốc đó nghĩa là gì -- xem {@code SubscriptionExpiryJob}.
     */
    int expireOverdue(Instant cutoff);

    // Id gói đang hoạt động của trường mà 1 user đang trực thuộc (dùng để trừ quota)
    Optional<UUID> findActiveSubscriptionIdForUser(UUID userId);

    // Hạn mức PRACTICE còn lại (USD) của user (ZERO nếu không có gói đang hoạt động)
    BigDecimal findPracticeQuotaRemaining(UUID userId);

    // Số phút tối đa mỗi lượt luyện của gói đang hoạt động (null nếu không có gói)
    Integer findMaxTimePerAttemptMinForUser(UUID userId);
}
