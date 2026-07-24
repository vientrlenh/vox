package com.sep.vox.infrastructure.persistence.query;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.repository.FinancialEventQueryRepository;
import com.sep.vox.domain.dto.FinancialEventDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaFinancialEventQueryRepository implements FinancialEventQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<FinancialEventDto> findAllBySchoolId(UUID schoolId, Pageable pageable) {
        var content = em.createQuery("""
            SELECT new com.sep.vox.domain.dto.FinancialEventDto(
                e.id,
                e.schoolId,
                e.subscriptionId,
                e.eventType,
                e.amountSigned,
                e.currency,
                e.paymentMethod,
                e.actorId,
                str(e.occurredAt))
            FROM FinancialEventJpaEntity e
            WHERE e.schoolId = :schoolId
            ORDER BY e.occurredAt DESC
        """, FinancialEventDto.class)
            .setParameter("schoolId", schoolId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        var total = em.createQuery("""
            SELECT COUNT(e)
            FROM FinancialEventJpaEntity e
            WHERE e.schoolId = :schoolId
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
