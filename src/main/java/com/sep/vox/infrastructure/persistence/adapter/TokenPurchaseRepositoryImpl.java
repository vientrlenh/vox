package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.TokenPurchase;
import com.sep.vox.domain.repository.TokenPurchaseRepository;
import com.sep.vox.infrastructure.persistence.mapper.TokenPurchaseMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTokenPurchaseRepository;

@Repository
public class TokenPurchaseRepositoryImpl implements TokenPurchaseRepository {

    private final SpringDataTokenPurchaseRepository springDataTokenPurchaseRepository;

    public TokenPurchaseRepositoryImpl(SpringDataTokenPurchaseRepository springDataTokenPurchaseRepository) {
        this.springDataTokenPurchaseRepository = springDataTokenPurchaseRepository;
    }

    @Override
    public Optional<TokenPurchase> findById(UUID id) {
        return springDataTokenPurchaseRepository.findById(id).map(TokenPurchaseMapper::toDomain);
    }

    @Override
    public TokenPurchase save(TokenPurchase purchase) {
        var entity = TokenPurchaseMapper.toJpa(purchase);
        var saved = springDataTokenPurchaseRepository.save(entity);
        return TokenPurchaseMapper.toDomain(saved);
    }

    @Override
    public List<TokenPurchase> findAllBySubscriptionId(UUID subscriptionId) {
        return springDataTokenPurchaseRepository.findAllBySubscriptionId(subscriptionId).stream()
            .map(TokenPurchaseMapper::toDomain)
            .toList();
    }
}
