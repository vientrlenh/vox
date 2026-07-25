package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.TokenPurchaseItem;

public interface TokenPurchaseItemRepository {
    Optional<TokenPurchaseItem> findById(UUID id);
    TokenPurchaseItem save(TokenPurchaseItem item);
    List<TokenPurchaseItem> findAllByPurchaseId(UUID purchaseId);
    List<TokenPurchaseItem> findAllByPurchaseIdIn(Collection<UUID> purchaseIds);
}
