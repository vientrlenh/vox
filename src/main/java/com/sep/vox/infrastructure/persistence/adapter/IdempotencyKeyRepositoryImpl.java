package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.IdempotencyKey;
import com.sep.vox.domain.repository.IdempotencyKeyRepository;
import com.sep.vox.infrastructure.persistence.mapper.IdempotencyKeyMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataIdempotencyKeyRepository;

@Repository
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepository {

    private final SpringDataIdempotencyKeyRepository springDataIdempotencyKeyRepository;

    public IdempotencyKeyRepositoryImpl(SpringDataIdempotencyKeyRepository springDataIdempotencyKeyRepository) {
        this.springDataIdempotencyKeyRepository = springDataIdempotencyKeyRepository;
    }

    @Override
    public Optional<IdempotencyKey> findByKey(String key) {
        return springDataIdempotencyKeyRepository.findByKey(key).map(IdempotencyKeyMapper::toDomain);
    }

    @Override
    public IdempotencyKey save(IdempotencyKey idempotencyKey) {
        var entity = IdempotencyKeyMapper.toJpa(idempotencyKey);
        var saved = springDataIdempotencyKeyRepository.save(entity);
        return IdempotencyKeyMapper.toDomain(saved);
    }
}
