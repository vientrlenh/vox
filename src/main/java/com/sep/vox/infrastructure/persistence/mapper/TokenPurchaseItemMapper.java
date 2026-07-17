package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.TokenPurchaseItem;
import com.sep.vox.infrastructure.persistence.entity.TokenPurchaseItemJpaEntity;

public final class TokenPurchaseItemMapper {

    private TokenPurchaseItemMapper() {}

    public static TokenPurchaseItem toDomain(TokenPurchaseItemJpaEntity jpa) {
        return new TokenPurchaseItem(
            jpa.getId(),
            jpa.getPurchaseId(),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getQuantity(),
            jpa.getUnitPriceSnapshot(),
            jpa.getSubtotal()
        );
    }

    public static TokenPurchaseItemJpaEntity toJpa(TokenPurchaseItem domain) {
        return new TokenPurchaseItemJpaEntity(
            domain.getId(),
            domain.getPurchaseId(),
            domain.getQuotaType().name(),
            domain.getQuantity(),
            domain.getUnitPriceSnapshot(),
            domain.getSubtotal()
        );
    }
}
