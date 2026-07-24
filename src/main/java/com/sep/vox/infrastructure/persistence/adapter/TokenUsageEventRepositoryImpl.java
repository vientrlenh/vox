package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.TokenUsageEvent;
import com.sep.vox.domain.repository.TokenUsageEventRepository;
import com.sep.vox.infrastructure.persistence.mapper.TokenUsageEventMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTokenUsageEventRepository;

@Repository
public class TokenUsageEventRepositoryImpl implements TokenUsageEventRepository {

    private final SpringDataTokenUsageEventRepository springDataTokenUsageEventRepository;

    public TokenUsageEventRepositoryImpl(SpringDataTokenUsageEventRepository springDataTokenUsageEventRepository) {
        this.springDataTokenUsageEventRepository = springDataTokenUsageEventRepository;
    }

    @Override
    public Optional<TokenUsageEvent> findById(UUID id) {
        return springDataTokenUsageEventRepository.findById(id).map(TokenUsageEventMapper::toDomain);
    }

    @Override
    public TokenUsageEvent save(TokenUsageEvent event) {
        var entity = TokenUsageEventMapper.toJpa(event);
        var saved = springDataTokenUsageEventRepository.save(entity);
        return TokenUsageEventMapper.toDomain(saved);
    }

    @Override
    public List<TokenUsageEvent> findAllBySubscriptionId(UUID subscriptionId) {
        return springDataTokenUsageEventRepository.findAllBySubscriptionId(subscriptionId).stream()
            .map(TokenUsageEventMapper::toDomain)
            .toList();
    }
}
