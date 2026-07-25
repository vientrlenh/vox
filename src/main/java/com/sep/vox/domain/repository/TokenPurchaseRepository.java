package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.TokenPurchase;

public interface TokenPurchaseRepository {
    Optional<TokenPurchase> findById(UUID id);
    TokenPurchase save(TokenPurchase purchase);
    List<TokenPurchase> findAllBySubscriptionId(UUID subscriptionId);
}
