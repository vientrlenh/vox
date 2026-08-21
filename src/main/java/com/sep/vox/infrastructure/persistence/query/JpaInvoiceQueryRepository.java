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

import com.sep.vox.application.query.repository.InvoiceQueryRepository;
import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.dto.InvoiceQuotaItemDto;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaInvoiceQueryRepository implements InvoiceQueryRepository {

    @PersistenceContext
    private EntityManager em;

    private final PlanQuotaRepository planQuotaRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;

    public JpaInvoiceQueryRepository(
            PlanQuotaRepository planQuotaRepository, TokenPurchaseItemRepository tokenPurchaseItemRepository) {
        this.planQuotaRepository = planQuotaRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
    }

    @Override
    public Page<InvoiceDto> findAllBySchoolId(UUID schoolId, Pageable pageable) {
        var rows = em.createQuery("""
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
                str(i.paidAt),
                i.resolvedPlanId,
                null)
            FROM InvoiceJpaEntity i
            WHERE i.schoolId = :schoolId
            ORDER BY i.issueDate DESC
        """, InvoiceDto.class)
            .setParameter("schoolId", schoolId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        var total = em.createQuery("""
            SELECT COUNT(i)
            FROM InvoiceJpaEntity i
            WHERE i.schoolId = :schoolId
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .getSingleResult();

        var content = withQuotaItems(rows);

        return new PageImpl<>(content, pageable, total);
    }

    // Không thể gộp thẳng vào 1 JPQL vì "quotaItems" của 1 hóa đơn đến từ 1 trong 2 bảng khác nhau
    // tuỳ sourceType (plan_quota qua resolvedPlanId, hoặc token_purchase_item qua sourceId) -- gom
    // theo batch (findAllByPlanIdIn/findAllByPurchaseIdIn) để tránh N+1 query trên 1 trang invoice.
    private List<InvoiceDto> withQuotaItems(List<InvoiceDto> rows) {
        var planIds = rows.stream()
            .filter(row -> isPlanSourced(row.sourceType()) && row.resolvedPlanId() != null)
            .map(InvoiceDto::resolvedPlanId)
            .distinct()
            .toList();
        var purchaseIds = rows.stream()
            .filter(row -> InvoiceSourceType.TOKEN_PURCHASE.name().equals(row.sourceType()))
            .map(InvoiceDto::sourceId)
            .distinct()
            .toList();

        Map<UUID, List<InvoiceQuotaItemDto>> quotaItemsByPlanId = planQuotaRepository.findAllByPlanIdIn(planIds)
            .stream()
            .collect(Collectors.groupingBy(
                planQuota -> planQuota.getPlanId(),
                HashMap::new,
                Collectors.mapping(
                    planQuota -> new InvoiceQuotaItemDto(planQuota.getQuotaType(), planQuota.getIncludedQuantity()),
                    Collectors.toList())));

        Map<UUID, List<InvoiceQuotaItemDto>> quotaItemsByPurchaseId = tokenPurchaseItemRepository
            .findAllByPurchaseIdIn(purchaseIds)
            .stream()
            .collect(Collectors.groupingBy(
                item -> item.getPurchaseId(),
                HashMap::new,
                Collectors.mapping(
                    item -> new InvoiceQuotaItemDto(item.getQuotaType(), item.getQuantity()),
                    Collectors.toList())));

        return rows.stream()
            .map(row -> new InvoiceDto(
                row.id(), row.invoiceNumber(), row.subscriptionId(), row.sourceType(), row.sourceId(),
                row.issueDate(), row.amount(), row.status(), row.paymentLinkId(), row.checkoutUrl(), row.paidAt(),
                row.resolvedPlanId(),
                isPlanSourced(row.sourceType())
                    ? quotaItemsByPlanId.getOrDefault(row.resolvedPlanId(), List.of())
                    : quotaItemsByPurchaseId.getOrDefault(row.sourceId(), List.of())))
            .toList();
    }

    private boolean isPlanSourced(String sourceType) {
        return InvoiceSourceType.SUBSCRIPTION.name().equals(sourceType)
            || InvoiceSourceType.SUBSCRIPTION_REQUEST.name().equals(sourceType);
    }
}
