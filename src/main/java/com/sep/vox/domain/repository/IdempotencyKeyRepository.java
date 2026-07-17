package com.sep.vox.domain.repository;

import java.util.Optional;

import com.sep.vox.domain.model.subscription.IdempotencyKey;

public interface IdempotencyKeyRepository {
    Optional<IdempotencyKey> findByKey(String key);
    IdempotencyKey save(IdempotencyKey idempotencyKey);
}
