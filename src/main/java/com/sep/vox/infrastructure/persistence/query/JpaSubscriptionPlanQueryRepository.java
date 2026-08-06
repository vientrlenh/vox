package com.sep.vox.infrastructure.persistence.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.PlanQuotaRowDto;
import com.sep.vox.application.query.dto.SubscriptionPlanRowDto;
import com.sep.vox.application.query.repository.SubscriptionPlanQueryRepository;
import com.sep.vox.domain.dto.PlanQuotaDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.PlanStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaSubscriptionPlanQueryRepository implements SubscriptionPlanQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<SubscriptionPlanDto> findAllByStatus(PlanStatus status, Pageable pageable) {
        return queryPlans(status.name(), pageable);
    }

    @Override
    public Page<SubscriptionPlanDto> findAll(Pageable pageable) {
        return queryPlans(null, pageable);
    }

    private Page<SubscriptionPlanDto> queryPlans(String status, Pageable pageable) {
        var plans = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.SubscriptionPlanRowDto(
                p.id,
                p.name,
                p.tagline,
                p.pricePerYear,
                p.validityDays,
                p.maxTimePerAttemptMin,
                p.maxStudentCount,
                p.popular,
                p.status,
                p.version,
                str(p.createdAt),
                p.createdBy,
                p.replacedByPlanId)
            FROM SubscriptionPlanJpaEntity p
            WHERE (:status IS NULL OR p.status = :status)
            ORDER BY p.createdAt DESC
        """, SubscriptionPlanRowDto.class)
            .setParameter("status", status)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        var total = em.createQuery("""
            SELECT COUNT(p) FROM SubscriptionPlanJpaEntity p WHERE (:status IS NULL OR p.status = :status)
        """, Long.class)
            .setParameter("status", status)
            .getSingleResult();

        List<PlanQuotaRowDto> quotas = plans.isEmpty() ? List.of() : em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.PlanQuotaRowDto(q.id,
            q.planId,
            q.quotaType,
            q.includedQuantity,
            q.tokenUnitPrice)
            FROM PlanQuotaJpaEntity q
            WHERE q.planId IN :planIds
        """, PlanQuotaRowDto.class)
            .setParameter("planIds", plans.stream().map(p -> p.id()).toList())
            .getResultList();

        var quotasByPlanId = quotas.stream().collect(Collectors.groupingBy(q -> q.planId()));

        // Gói nào đang có >=1 trường ACTIVE thì bị khóa sửa (xem UpdatePlanUseCase) — FE dùng cờ
        // này để ẩn nút Sửa luôn thay vì để admin điền form xong mới bị BE từ chối.
        Set<UUID> planIdsWithActiveSubscribers = plans.isEmpty() ? Set.of() : new HashSet<>(em.createQuery("""
            SELECT DISTINCT s.planId FROM SchoolSubscriptionJpaEntity s
            WHERE s.planId IN :planIds AND s.status = 'ACTIVE'
        """, UUID.class)
            .setParameter("planIds", plans.stream().map(p -> p.id()).toList())
            .getResultList());

        var content = plans.stream()
            .map(row -> new SubscriptionPlanDto(
                row.id(), row.name(), row.tagline(), row.pricePerYear(), row.validityDays(),
                row.maxTimePerAttemptMin(), row.maxStudentCount(), row.popular(), row.status(), row.version(),
                row.createdAt(), row.createdBy(), row.replacedByPlanId(),
                planIdsWithActiveSubscribers.contains(row.id()),
                quotasByPlanId.getOrDefault(row.id(), List.of()).stream()
                    .map(q -> new PlanQuotaDto(q.id(), q.quotaType(), q.includedQuantity(), q.tokenUnitPrice()))
                    .toList()))
            .toList();

        return new PageImpl<>(content, pageable, total);
    }
}