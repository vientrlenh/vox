package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.PurchaseStatus;
import com.sep.vox.domain.model.subscription.TokenPurchase;
import com.sep.vox.infrastructure.persistence.entity.TokenPurchaseJpaEntity;

public final class TokenPurchaseMapper {

    private TokenPurchaseMapper() {}

    public static TokenPurchase toDomain(TokenPurchaseJpaEntity jpa) {
        return new TokenPurchase(
            jpa.getId(),
            jpa.getSubscriptionId(),
            jpa.getTotalAmount(),
            PurchaseStatus.valueOf(jpa.getStatus()),
            jpa.getPurchasedAt()
        );
    }

    public static TokenPurchaseJpaEntity toJpa(TokenPurchase domain) {
        return new TokenPurchaseJpaEntity(
            domain.getId(),
            domain.getSubscriptionId(),
            domain.getTotalAmount(),
            domain.getStatus().name(),
            domain.getPurchasedAt()
        );
    }
}
