package com.sep.vox.infrastructure.persistence.query;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.repository.InvoiceQueryRepository;
import com.sep.vox.domain.dto.InvoiceDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaInvoiceQueryRepository implements InvoiceQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<InvoiceDto> findAllBySchoolId(UUID schoolId, Pageable pageable) {
        var content = em.createQuery("""
            SELECT new com.sep.vox.domain.dto.InvoiceDto(
                i.id,
                i.invoiceNumber,
                i.subscriptionId,
                i.sourceType,
                i.sourceId,
                str(i.issueDate),
                i.amount,
                i.status,
                i.paymentLinkId,
                i.checkoutUrl,
                str(i.paidAt))
            FROM InvoiceJpaEntity i
            WHERE i.subscriptionId IN (
                SELECT s.id FROM SchoolSubscriptionJpaEntity s WHERE s.schoolId = :schoolId
            )
            ORDER BY i.issueDate DESC
        """, InvoiceDto.class)
            .setParameter("schoolId", schoolId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        var total = em.createQuery("""
            SELECT COUNT(i)
            FROM InvoiceJpaEntity i
            WHERE i.subscriptionId IN (
                SELECT s.id FROM SchoolSubscriptionJpaEntity s WHERE s.schoolId = :schoolId
            )
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
