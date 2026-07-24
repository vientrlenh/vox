package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.TokenPurchaseRowDto;
import com.sep.vox.application.query.repository.TokenPurchaseQueryRepository;
import com.sep.vox.domain.dto.TokenPurchaseDto;
import com.sep.vox.domain.dto.TokenPurchaseItemDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaTokenPurchaseQueryRepository implements TokenPurchaseQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<TokenPurchaseDto> findAllByActiveSchoolSubscription(UUID schoolId) {
        var purchases = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.TokenPurchaseRowDto(
                p.id, 
                p.subscriptionId, 
                p.totalAmount, 
                p.status, 
                str(p.purchasedAt))
            FROM TokenPurchaseJpaEntity p
            WHERE p.subscriptionId = (
                SELECT s.id FROM SchoolSubscriptionJpaEntity s
                WHERE s.schoolId = :schoolId AND s.status = 'ACTIVE'
            )
            ORDER BY p.purchasedAt DESC
        """, TokenPurchaseRowDto.class)
            .setParameter("schoolId", schoolId)
            .getResultList();

        if (purchases.isEmpty()) {
            return List.of();
        }

        var purchaseIds = purchases.stream().map(TokenPurchaseRowDto::id).toList();
        var itemsByPurchaseId = em.createQuery("""
            SELECT new com.sep.vox.domain.dto.TokenPurchaseItemDto(
                i.id, 
                i.purchaseId, 
                i.quotaType, 
                i.quantity, 
                i.unitPriceSnapshot, 
                i.subtotal)
            FROM TokenPurchaseItemJpaEntity i
            WHERE i.purchaseId IN :purchaseIds
        """, TokenPurchaseItemDto.class)
            .setParameter("purchaseIds", purchaseIds)
            .getResultList()
            .stream()
            .collect(Collectors.groupingBy(TokenPurchaseItemDto::purchaseId));

        return purchases.stream()
            .map(row -> new TokenPurchaseDto(
                row.id(),
                row.subscriptionId(),
                row.totalAmount(),
                row.status(),
                row.purchasedAt(),
                itemsByPurchaseId.getOrDefault(row.id(), List.of())))
            .toList();
    }
}
