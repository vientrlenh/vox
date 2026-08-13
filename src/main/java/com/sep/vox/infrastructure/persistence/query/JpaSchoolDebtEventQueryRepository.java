package com.sep.vox.infrastructure.persistence.query;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.repository.SchoolDebtEventQueryRepository;
import com.sep.vox.domain.dto.SchoolDebtEventDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaSchoolDebtEventQueryRepository implements SchoolDebtEventQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<SchoolDebtEventDto> findAllBySchoolId(UUID schoolId, Pageable pageable) {
        var content = em.createQuery("""
            SELECT new com.sep.vox.domain.dto.SchoolDebtEventDto(
                e.id,
                e.schoolId,
                e.subscriptionId,
                e.eventType,
                e.quotaType,
                e.triggerExamSessionId,
                e.triggerAmountUsd,
                e.totalAllocatedUsd,
                e.usedQuantityUsd,
                e.overageUsd,
                str(e.occurredAt))
            FROM SchoolDebtEventJpaEntity e
            WHERE e.schoolId = :schoolId
            ORDER BY e.occurredAt DESC
        """, SchoolDebtEventDto.class)
            .setParameter("schoolId", schoolId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        var total = em.createQuery("""
            SELECT COUNT(e)
            FROM SchoolDebtEventJpaEntity e
            WHERE e.schoolId = :schoolId
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
