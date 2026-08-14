package com.sep.vox.infrastructure.persistence.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                p.status,
                p.version,
                str(p.createdAt),
                p.createdBy,
                p.replacedByPlanId,
                p.serviceFeeRatio)
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

        // popular được tính live: đếm số trường đang ACTIVE trên từng gói, rồi đánh dấu popular
        // cho (các) gói ACTIVE có số trường cao nhất — không còn là cờ admin tự set (xem
        // JpaSubscriptionPlanQueryRepository / kế hoạch "bỏ cờ phổ biến thủ công").
        Map<UUID, Long> activeSchoolCountByPlanId = plans.isEmpty() ? Map.of() : em.createQuery("""
            SELECT s.planId, COUNT(DISTINCT s.schoolId) FROM SchoolSubscriptionJpaEntity s
            WHERE s.planId IN :planIds AND s.status = 'ACTIVE'
            GROUP BY s.planId
        """, Object[].class)
            .setParameter("planIds", plans.stream().map(p -> p.id()).toList())
            .getResultList()
            .stream()
            .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> (Long) row[1],
                Long::sum,
                HashMap::new
            ));

        var maxActiveSchoolCountAmongActivePlans = plans.stream()
            .filter(row -> PlanStatus.ACTIVE.name().equals(row.status()))
            .mapToLong(row -> activeSchoolCountByPlanId.getOrDefault(row.id(), 0L))
            .max()
            .orElse(0L);

        var content = plans.stream()
            .map(row -> {
                var activeSchoolCount = activeSchoolCountByPlanId.getOrDefault(row.id(), 0L);
                var popular = PlanStatus.ACTIVE.name().equals(row.status())
                    && maxActiveSchoolCountAmongActivePlans > 0
                    && activeSchoolCount == maxActiveSchoolCountAmongActivePlans;

                return new SubscriptionPlanDto(
                    row.id(), row.name(), row.tagline(), row.pricePerYear(), row.validityDays(),
                    row.maxTimePerAttemptMin(), row.maxStudentCount(), popular, row.status(), row.version(),
                    row.createdAt(), row.createdBy(), row.replacedByPlanId(), row.serviceFeeRatio(),
                    quotasByPlanId.getOrDefault(row.id(), List.of()).stream()
                        .map(q -> new PlanQuotaDto(q.id(), q.quotaType(), q.includedQuantity(), q.tokenUnitPrice()))
                        .toList());
            })
            .toList();

        return new PageImpl<>(content, pageable, total);
    }
}