package com.sep.vox.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;

public interface SchoolSubscriptionRepository {
    Optional<SchoolSubscription> findById(UUID id);
    SchoolSubscription save(SchoolSubscription subscription);
    Optional<SchoolSubscription> findActiveBySchoolId(UUID schoolId);
    List<SchoolSubscription> findAllBySchoolId(UUID schoolId);
    PageResult<SchoolSubscription> findAllForAdmin(UUID planId, SubscriptionStatus status, String keyword, int page, int size);
    boolean existsActiveByPlanId(UUID planId);

    /** Chuyển hàng loạt ACTIVE -> EXPIRED cho các subscription đã qua endDate. Trả về số dòng bị đổi. */
    int expireOverdue(LocalDate today);

    // Id gói đang hoạt động của trường mà 1 user đang trực thuộc (dùng để trừ quota)
    Optional<UUID> findActiveSubscriptionIdForUser(UUID userId);

    // Hạn mức PRACTICE còn lại của user (0 nếu không có gói đang hoạt động)
    int findPracticeQuotaRemaining(UUID userId);

    // Số phút tối đa mỗi lượt luyện của gói đang hoạt động (null nếu không có gói)
    Integer findMaxTimePerAttemptMinForUser(UUID userId);
}
