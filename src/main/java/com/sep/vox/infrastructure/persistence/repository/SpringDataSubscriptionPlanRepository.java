package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SubscriptionPlanJpaEntity;

public interface SpringDataSubscriptionPlanRepository extends JpaRepository<SubscriptionPlanJpaEntity, UUID> {
    List<SubscriptionPlanJpaEntity> findByStatus(String status);

    /**
     * Có gói nào đang trỏ replacedByPlanId vào gói này không -- tức gói này đang là ĐÍCH của một
     * chuỗi thay thế. Dùng để không cho lưu trữ nó mà không chỉ định gói thay thế tiếp theo, vì làm
     * vậy sẽ cắt cụt chuỗi và những trường ở đầu chuỗi mất luôn đường gia hạn.
     */
    boolean existsByReplacedByPlanId(UUID replacedByPlanId);
    Page<SubscriptionPlanJpaEntity> findByStatus(String status, Pageable pageable);
}
