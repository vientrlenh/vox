package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionQuotaRecordJpaEntity;

public interface SpringDataSchoolSubscriptionQuotaRecordRepository extends JpaRepository<SchoolSubscriptionQuotaRecordJpaEntity, UUID> {
    List<SchoolSubscriptionQuotaRecordJpaEntity> findBySchoolSubscriptionId(UUID schoolSubscriptionId);
    Optional<SchoolSubscriptionQuotaRecordJpaEntity> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, String quotaType);

    @Modifying
    @Query("""
        UPDATE SchoolSubscriptionQuotaRecordJpaEntity q
        SET q.usedAmountVnd = q.usedAmountVnd + :amount
        WHERE q.id = :id AND q.usedAmountVnd + :amount <= q.totalAllocatedAmountVnd
        """)
    int tryConsume(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    // clearAutomatically=true BẮT BUỘC: nơi gọi luôn đọc lại entity này (before/after) trong CÙNG
    // transaction để so sánh trạng thái khóa/nợ (SchoolLockedDueToDebt/SchoolDebtCapExceeded/
    // SchoolDebtCleared) -- thiếu cờ này, Hibernate trả về entity CACHE cũ từ lần load trước thay vì
    // đọc lại DB sau bulk update, khiến so sánh trước/sau luôn sai (dù DB đã đúng).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SchoolSubscriptionQuotaRecordJpaEntity q SET q.totalAllocatedAmountVnd = q.totalAllocatedAmountVnd + :amount WHERE q.id = :id")
    void addAllocation(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE SchoolSubscriptionQuotaRecordJpaEntity q SET q.usedAmountVnd = q.usedAmountVnd + :amount WHERE q.id = :id")
    void addUsage(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}
