package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.TokenPurchaseItem;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.infrastructure.persistence.mapper.TokenPurchaseItemMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTokenPurchaseItemRepository;

@Repository
public class TokenPurchaseItemRepositoryImpl implements TokenPurchaseItemRepository {

    private final SpringDataTokenPurchaseItemRepository springDataTokenPurchaseItemRepository;

    public TokenPurchaseItemRepositoryImpl(SpringDataTokenPurchaseItemRepository springDataTokenPurchaseItemRepository) {
        this.springDataTokenPurchaseItemRepository = springDataTokenPurchaseItemRepository;
    }

    @Override
    public Optional<TokenPurchaseItem> findById(UUID id) {
        return springDataTokenPurchaseItemRepository.findById(id).map(TokenPurchaseItemMapper::toDomain);
    }

    @Override
    public TokenPurchaseItem save(TokenPurchaseItem item) {
        var entity = TokenPurchaseItemMapper.toJpa(item);
        var saved = springDataTokenPurchaseItemRepository.save(entity);
        return TokenPurchaseItemMapper.toDomain(saved);
    }

    @Override
    public List<TokenPurchaseItem> findAllByPurchaseId(UUID purchaseId) {
        return springDataTokenPurchaseItemRepository.findAllByPurchaseId(purchaseId).stream()
            .map(TokenPurchaseItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<TokenPurchaseItem> findAllByPurchaseIdIn(Collection<UUID> purchaseIds) {
        return springDataTokenPurchaseItemRepository.findAllByPurchaseIdIn(purchaseIds).stream()
            .map(TokenPurchaseItemMapper::toDomain)
            .toList();
    }
}
