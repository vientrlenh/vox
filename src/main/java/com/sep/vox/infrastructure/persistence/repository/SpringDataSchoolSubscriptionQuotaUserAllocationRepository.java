package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionQuotaUserAllocationJpaEntity;

public interface SpringDataSchoolSubscriptionQuotaUserAllocationRepository extends JpaRepository<SchoolSubscriptionQuotaUserAllocationJpaEntity, UUID> {
    List<SchoolSubscriptionQuotaUserAllocationJpaEntity> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, String quotaType);
    Optional<SchoolSubscriptionQuotaUserAllocationJpaEntity> findBySchoolSubscriptionIdAndQuotaTypeAndUserId(UUID schoolSubscriptionId, String quotaType, UUID userId);

    @Modifying
    @Query("""
        UPDATE SchoolSubscriptionQuotaUserAllocationJpaEntity a
        SET a.usedAmountVnd = a.usedAmountVnd + :amount
        WHERE a.id = :id AND a.usedAmountVnd + :amount <= a.allocatedAmountVnd
        """)
    int tryConsume(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    // clearAutomatically=true -- cùng lý do với SpringDataSchoolSubscriptionQuotaRecordRepository.addUsage:
    // tránh Hibernate trả về entity cache cũ nếu có chỗ nào sau này đọc lại trong cùng transaction.
    //
    // flushAutomatically=true BẮT BUỘC đi kèm, không phải cho gọn đôi: ConsumeQuotaService gọi
    // consumeUserAllocation SAU chargeOverage, mà chargeOverage vừa merge SchoolBalance đã bị trừ và
    // persist bút toán OVERAGE_CHARGE -- cả hai còn nằm chờ trong persistence context. Hibernate chỉ
    // tự flush cho những query space mà câu UPDATE này chạm tới (bảng allocation), KHÔNG gồm
    // school_balances / school_balance_entries, nên em.clear() sau đó vứt thẳng khoản ghi nợ: chi phí
    // AI biến mất khỏi ví trường mà không lỗi nào nổi lên, và bất biến SUM(entries) = balance_vnd vỡ.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SchoolSubscriptionQuotaUserAllocationJpaEntity a SET a.usedAmountVnd = a.usedAmountVnd + :amount WHERE a.id = :id")
    void addUsage(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}
