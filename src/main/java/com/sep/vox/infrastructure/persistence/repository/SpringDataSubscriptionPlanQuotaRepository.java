package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SubscriptionPlanQuotaJpaEntity;

public interface SpringDataSubscriptionPlanQuotaRepository extends JpaRepository<SubscriptionPlanQuotaJpaEntity, UUID> {
    List<SubscriptionPlanQuotaJpaEntity> findBySubscriptionPlanId(UUID subscriptionPlanId);
    List<SubscriptionPlanQuotaJpaEntity> findBySubscriptionPlanIdIn(Collection<UUID> subscriptionPlanIds);

    /**
     * DML thẳng chứ KHÔNG phải derived delete (load entity lên rồi đánh dấu removed). Bắt buộc phải
     * vậy vì UpdateSubscriptionPlanUseCase thay bộ hạn mức theo kiểu xóa hết -- ghi lại: derived
     * delete chỉ xếp hàng chờ tới lúc flush, mà ActionQueue của Hibernate chạy INSERT TRƯỚC DELETE,
     * nên dòng mới sẽ đâm vào dòng cũ chưa kịp xóa. Hiện chưa có unique (subscription_plan_id,
     * quota_type) nên chưa vỡ, nhưng đúng ra phải có -- xem CreateSubscriptionPlanUseCase.
     *
     * <p>flushAutomatically = true để thay đổi đang treo (vd. UPDATE gói vừa lưu ngay trước đó) được
     * đẩy xuống DB TRƯỚC khi câu DELETE này chạy, nếu không clearAutomatically sẽ vứt mất chúng.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SubscriptionPlanQuotaJpaEntity q WHERE q.subscriptionPlanId = :subscriptionPlanId")
    void deleteBySubscriptionPlanId(@Param("subscriptionPlanId") UUID subscriptionPlanId);
}
