package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.TokenUsageEvent;

public interface TokenUsageEventRepository {
    Optional<TokenUsageEvent> findById(UUID id);
    TokenUsageEvent save(TokenUsageEvent event);
    List<TokenUsageEvent> findAllBySubscriptionId(UUID subscriptionId);
}
